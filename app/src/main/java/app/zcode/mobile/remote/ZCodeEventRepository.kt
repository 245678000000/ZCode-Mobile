package app.zcode.mobile.remote

import app.zcode.mobile.model.ApprovalRequest
import app.zcode.mobile.model.ApprovalRequired
import app.zcode.mobile.model.ApprovalResolved
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.ArtifactCreated
import app.zcode.mobile.model.ConnectionLost
import app.zcode.mobile.model.ConnectionRestored
import app.zcode.mobile.model.ConnectionState
import app.zcode.mobile.model.PageSnapshot
import app.zcode.mobile.model.Task
import app.zcode.mobile.model.TaskCompleted
import app.zcode.mobile.model.TaskCreated
import app.zcode.mobile.model.TaskFailed
import app.zcode.mobile.model.TaskRunning
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.model.TaskUpdated
import app.zcode.mobile.model.TaskWaiting
import app.zcode.mobile.model.ZCodeEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque

class ZCodeEventRepository(
    private val maxEvents: Int = 50,
    private val onAccepted: (ZCodeEvent) -> Unit = {},
) {
    private val _events = MutableStateFlow<List<ZCodeEvent>>(emptyList())
    val events: StateFlow<List<ZCodeEvent>> = _events.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _approvals = MutableStateFlow<List<ApprovalRequest>>(emptyList())
    val approvals: StateFlow<List<ApprovalRequest>> = _approvals.asStateFlow()

    private val _artifacts = MutableStateFlow<List<Artifact>>(emptyList())
    val artifacts: StateFlow<List<Artifact>> = _artifacts.asStateFlow()

    private val _connection = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _observerActive = MutableStateFlow(false)
    val observerActive: StateFlow<Boolean> = _observerActive.asStateFlow()

    private val _lastUrl = MutableStateFlow("")
    val lastUrl: StateFlow<String> = _lastUrl.asStateFlow()

    private val lastEventCache = ArrayDeque<String>()
    private var lastSnapshot: PageSnapshot? = null
    private var lastConnectionEventAt: Long = 0L

    val activeTask: Task?
        get() = activeTask(_tasks.value)

    fun activeTask(list: List<Task>): Task? {
        val sorted = sortedTasks(list)
        return sorted.firstOrNull {
            it.status == TaskStatus.WAITING_APPROVAL || it.status == TaskStatus.RUNNING
        } ?: sorted.firstOrNull()
    }

    fun ingestEvent(event: ZCodeEvent): Boolean {
        val key = event.dedupeKey()
        if (lastEventCache.contains(key)) return false
        remember(key)
        applyEvent(event)
        _events.value = (listOf(event) + _events.value).take(maxEvents)
        onAccepted(event)
        return true
    }

    fun ingestSnapshot(snapshot: PageSnapshot) {
        _observerActive.value = snapshot.observerActive
        if (snapshot.url.isNotBlank()) _lastUrl.value = snapshot.url
        val emitted = PageSnapshotReducer.diff(lastSnapshot, snapshot)
        lastSnapshot = snapshot
        emitted.forEach { ingestEvent(it) }
        mergeSnapshotTasks(snapshot)
    }

    fun ingestConnection(state: ConnectionState, forceEvent: Boolean = false) {
        val previous = _connection.value
        if (previous == state && !forceEvent) return
        _connection.value = state
        val now = System.currentTimeMillis()
        val quiet = now - lastConnectionEventAt < 4_000
        when {
            state == ConnectionState.CONNECTED && previous != ConnectionState.CONNECTED && previous != ConnectionState.CONNECTING -> {
                if (!quiet) ingestEvent(ConnectionRestored())
                lastConnectionEventAt = now
            }
            state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR || state == ConnectionState.SESSION_EXPIRED -> {
                if (previous == ConnectionState.CONNECTED || previous == ConnectionState.CONNECTING) {
                    if (!quiet) ingestEvent(ConnectionLost(reason = state.name))
                    lastConnectionEventAt = now
                }
            }
        }
    }

    fun clear() {
        _events.value = emptyList()
        _tasks.value = emptyList()
        _approvals.value = emptyList()
        _artifacts.value = emptyList()
        lastEventCache.clear()
        lastSnapshot = null
        _observerActive.value = false
        _connection.value = ConnectionState.DISCONNECTED
    }

    fun taskById(id: String): Task? = _tasks.value.find { it.id == id }

    fun approvalById(id: String): ApprovalRequest? = _approvals.value.find { it.id == id }

    /** Pure sort so Compose can `remember(tasks)` it instead of re-reading the flow. */
    fun sortedTasks(list: List<Task> = _tasks.value): List<Task> {
        val rank = mapOf(
            TaskStatus.WAITING_APPROVAL to 0,
            TaskStatus.RUNNING to 1,
            TaskStatus.FAILED to 2,
            TaskStatus.QUEUED to 3,
            TaskStatus.UNKNOWN to 4,
            TaskStatus.COMPLETED to 5,
            TaskStatus.CANCELLED to 6,
        )
        return list.sortedWith(
            compareBy<Task> { rank[it.status] ?: 9 }.thenByDescending { it.updatedAt },
        )
    }

    fun sanitizedDebugDump(): String {
        return buildString {
            appendLine("connection=${_connection.value}")
            appendLine("observer=${_observerActive.value}")
            appendLine("url=${_lastUrl.value}")
            appendLine("tasks=${_tasks.value.size}")
            _tasks.value.forEach { appendLine("- ${it.status} ${it.title}") }
            appendLine("approvals=${_approvals.value.size}")
            _approvals.value.forEach { appendLine("- ${it.title} ${it.riskLevel}") }
            appendLine("artifacts=${_artifacts.value.size}")
            _artifacts.value.forEach { appendLine("- ${it.name}") }
            appendLine("events=${_events.value.size}")
            _events.value.take(50).forEach { appendLine("- ${it.type} ${it.timestamp}") }
        }
    }

    private fun applyEvent(event: ZCodeEvent) {
        when (event) {
            is TaskCreated -> upsertTask(event.taskId) {
                it.copy(title = event.title, sessionId = event.sessionId ?: it.sessionId, updatedAt = event.timestamp)
            }
            is TaskUpdated -> upsertTask(event.taskId) {
                it.copy(
                    title = event.title.ifBlank { it.title },
                    status = event.status,
                    currentStep = event.currentStep ?: it.currentStep,
                    updatedAt = event.timestamp,
                    completedAt = if (TaskStatusMapper.isTerminal(event.status)) event.timestamp else it.completedAt,
                )
            }
            is TaskRunning -> upsertTask(event.taskId) {
                it.copy(
                    title = event.title.ifBlank { it.title },
                    status = TaskStatus.RUNNING,
                    currentStep = event.currentStep ?: it.currentStep,
                    description = event.summary ?: it.description,
                    progress = event.progress,
                    updatedAt = event.timestamp,
                )
            }
            is TaskWaiting -> upsertTask(event.taskId) {
                it.copy(title = event.title.ifBlank { it.title }, status = TaskStatus.WAITING_APPROVAL, updatedAt = event.timestamp)
            }
            is TaskCompleted -> upsertTask(event.taskId) {
                it.copy(
                    title = event.title.ifBlank { it.title },
                    status = TaskStatus.COMPLETED,
                    description = event.summary ?: it.description,
                    updatedAt = event.timestamp,
                    completedAt = event.timestamp,
                )
            }
            is TaskFailed -> upsertTask(event.taskId) {
                it.copy(
                    title = event.title.ifBlank { it.title },
                    status = TaskStatus.FAILED,
                    description = event.summary ?: it.description,
                    updatedAt = event.timestamp,
                    completedAt = event.timestamp,
                )
            }
            is ApprovalRequired -> {
                val request = ApprovalRequest(
                    id = event.approvalId,
                    title = event.title,
                    description = event.description,
                    command = event.command,
                    riskLevel = event.riskLevel,
                    source = "dom",
                    timestamp = event.timestamp,
                    taskId = event.taskId,
                )
                _approvals.value = listOf(request) + _approvals.value.filterNot { it.id == request.id }
                event.taskId?.let { id ->
                    upsertTask(id) { it.copy(approvalCount = it.approvalCount + 1, status = TaskStatus.WAITING_APPROVAL, updatedAt = event.timestamp) }
                }
            }
            is ApprovalResolved -> {
                _approvals.value = _approvals.value.filterNot { it.id == event.approvalId }
            }
            is ArtifactCreated -> {
                val artifact = Artifact(
                    id = event.artifactId,
                    taskId = event.taskId,
                    name = event.name,
                    kind = event.kind,
                    sourceUrl = event.url?.ifBlank { null },
                    createdAt = event.timestamp,
                )
                _artifacts.value = listOf(artifact) + _artifacts.value.filterNot { it.id == artifact.id }
                event.taskId?.let { id ->
                    upsertTask(id) { t ->
                        t.copy(artifacts = listOf(artifact) + t.artifacts.filterNot { it.id == artifact.id })
                    }
                }
            }
            else -> Unit
        }
    }

    private fun mergeSnapshotTasks(snapshot: PageSnapshot) {
        snapshot.tasks.forEach { snap ->
            val status = TaskStatusMapper.fromText(snap.statusText)
            upsertTask(snap.id) { current ->
                current.copy(
                    title = snap.title.ifBlank { current.title },
                    sessionId = snapshot.sessionId ?: current.sessionId,
                    status = if (status != TaskStatus.UNKNOWN) status else current.status,
                    currentStep = snap.step ?: current.currentStep,
                    agentName = snap.agentName ?: current.agentName,
                    updatedAt = snapshot.timestamp,
                )
            }
        }
    }

    private fun upsertTask(id: String, transform: (Task) -> Task) {
        val current = _tasks.value
        val existing = current.find { it.id == id } ?: Task(id = id, title = "Untitled")
        val next = transform(existing)
        _tasks.value = listOf(next) + current.filterNot { it.id == id }
    }

    private fun remember(key: String) {
        lastEventCache.addLast(key)
        while (lastEventCache.size > 200) lastEventCache.removeFirst()
    }
}
