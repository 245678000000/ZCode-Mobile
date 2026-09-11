package app.zcode.mobile.remote

import app.zcode.mobile.model.ApprovalRequired
import app.zcode.mobile.model.ApprovalResolved
import app.zcode.mobile.model.Artifact
import app.zcode.mobile.model.ArtifactCreated
import app.zcode.mobile.model.ConnectionLost
import app.zcode.mobile.model.ConnectionRestored
import app.zcode.mobile.model.MessageReceived
import app.zcode.mobile.model.PageSnapshot
import app.zcode.mobile.model.SessionDetected
import app.zcode.mobile.model.TaskCompleted
import app.zcode.mobile.model.TaskCreated
import app.zcode.mobile.model.TaskFailed
import app.zcode.mobile.model.TaskRunning
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.model.TaskUpdated
import app.zcode.mobile.model.TaskWaiting
import app.zcode.mobile.model.ZCodeEvent

object PageSnapshotReducer {
    /**
     * Diffs two page snapshots into events.
     *
     * When [previous] is null the snapshot is a baseline: tasks are created so the UI can
     * show them, but terminal events (completed / failed) are NOT emitted — every task the
     * page already listed as finished would otherwise fire a "just completed" notification.
     */
    fun diff(previous: PageSnapshot?, current: PageSnapshot): List<ZCodeEvent> {
        val out = mutableListOf<ZCodeEvent>()
        val ts = current.timestamp
        val baseline = previous == null

        if (!current.sessionTitle.isNullOrBlank()) {
            val sid = current.sessionId ?: current.sessionTitle.hashCode().toString()
            if (previous?.sessionTitle != current.sessionTitle || previous.sessionId != current.sessionId) {
                out += SessionDetected(sessionId = sid, title = current.sessionTitle, timestamp = ts)
            }
        }

        val prevTasks = previous?.tasks.orEmpty().associateBy { it.id }
        current.tasks.forEach { task ->
            val status = TaskStatusMapper.fromText(task.statusText)
            val old = prevTasks[task.id]
            if (old == null) {
                out += TaskCreated(taskId = task.id, sessionId = current.sessionId, title = task.title, timestamp = ts)
            }
            if (old == null || old.statusText != task.statusText || old.step != task.step || old.title != task.title) {
                out += TaskUpdated(
                    taskId = task.id,
                    title = task.title,
                    status = status,
                    currentStep = task.step,
                    timestamp = ts,
                )
                when (status) {
                    TaskStatus.RUNNING -> out += TaskRunning(
                        taskId = task.id,
                        title = task.title,
                        summary = task.step,
                        progress = null,
                        currentStep = task.step,
                        timestamp = ts,
                    )
                    TaskStatus.WAITING_APPROVAL -> out += TaskWaiting(
                        taskId = task.id,
                        title = task.title,
                        summary = task.step,
                        timestamp = ts,
                    )
                    TaskStatus.COMPLETED -> if (!baseline && (old == null || TaskStatusMapper.fromText(old.statusText) != TaskStatus.COMPLETED)) {
                        out += TaskCompleted(taskId = task.id, title = task.title, summary = task.step, timestamp = ts)
                    }
                    TaskStatus.FAILED -> if (!baseline && (old == null || TaskStatusMapper.fromText(old.statusText) != TaskStatus.FAILED)) {
                        out += TaskFailed(taskId = task.id, title = task.title, summary = task.step, timestamp = ts)
                    }
                    else -> Unit
                }
            }
        }

        val prevApproval = previous?.approval
        val currApproval = ApprovalDetector.snapshotFromPage(current)
        if (currApproval != null && (prevApproval == null || prevApproval.id != currApproval.id || prevApproval.command != currApproval.command)) {
            out += ApprovalRequired(
                approvalId = currApproval.id,
                title = currApproval.title,
                description = currApproval.description,
                command = currApproval.command,
                riskLevel = ApprovalDetector.riskLevel(currApproval.command, currApproval.description),
                timestamp = ts,
            )
        } else if (prevApproval != null && currApproval == null && ApprovalDetector.isApproval(prevApproval)) {
            out += ApprovalResolved(approvalId = prevApproval.id, accepted = null, timestamp = ts)
        }

        val prevArtifacts = previous?.artifacts.orEmpty().map { it.id }.toSet()
        current.artifacts.forEach { art ->
            if (art.id !in prevArtifacts && ArtifactTypeDetector.looksLikeArtifactName(art.name)) {
                out += ArtifactCreated(
                    artifactId = art.id,
                    name = art.name,
                    kind = Artifact.kindFor(art.name, art.mime),
                    url = art.href,
                    timestamp = ts,
                )
            }
        }

        val prevMessages = previous?.messages.orEmpty().map { it.id }.toSet()
        current.messages.takeLast(8).forEach { msg ->
            if (msg.id !in prevMessages) {
                out += MessageReceived(messageId = msg.id, taskId = current.sessionId, text = msg.text, timestamp = ts)
            }
        }

        val prevHint = previous?.connectionHint?.lowercase()
        val currHint = current.connectionHint?.lowercase()
        if (currHint == "expired" || currHint == "lost" || currHint == "error") {
            if (prevHint != currHint || previous.errorText != current.errorText) {
                out += ConnectionLost(reason = current.errorText?.ifBlank { null } ?: currHint, timestamp = ts)
            }
        } else if (currHint == "ok" || currHint == "connected") {
            if (prevHint == "expired" || prevHint == "lost" || prevHint == "error") {
                out += ConnectionRestored(timestamp = ts)
            }
        }
        return out
    }
}
