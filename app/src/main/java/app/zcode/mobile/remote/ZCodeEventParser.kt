package app.zcode.mobile.remote

import app.zcode.mobile.model.ApprovalRequired
import app.zcode.mobile.model.ApprovalResolved
import app.zcode.mobile.model.ArtifactCreated
import app.zcode.mobile.model.ConnectionLost
import app.zcode.mobile.model.ConnectionRestored
import app.zcode.mobile.model.MessageReceived
import app.zcode.mobile.model.ObserverUnknown
import app.zcode.mobile.model.PageSnapshot
import app.zcode.mobile.model.RiskLevel
import app.zcode.mobile.model.SessionDetected
import app.zcode.mobile.model.SnapshotApproval
import app.zcode.mobile.model.SnapshotArtifact
import app.zcode.mobile.model.SnapshotMessage
import app.zcode.mobile.model.SnapshotTask
import app.zcode.mobile.model.TaskCompleted
import app.zcode.mobile.model.TaskCreated
import app.zcode.mobile.model.TaskFailed
import app.zcode.mobile.model.TaskRunning
import app.zcode.mobile.model.TaskStatus
import app.zcode.mobile.model.TaskUpdated
import app.zcode.mobile.model.TaskWaiting
import app.zcode.mobile.model.ZCodeEvent
import org.json.JSONArray
import org.json.JSONObject

object ZCodeEventParser {
    const val MAX_BYTES = 32_768

    fun parseEvent(json: String): ZCodeEvent? {
        val obj = parseObject(json) ?: return null
        val type = obj.optString("type").ifBlank { return null }
        val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        return try {
            when (type) {
                "SessionDetected" -> SessionDetected(
                    sessionId = req(obj, "sessionId") ?: return null,
                    title = SensitiveSanitizer.text(obj.optString("title")),
                    timestamp = timestamp,
                )
                "TaskCreated" -> TaskCreated(
                    taskId = req(obj, "taskId") ?: return null,
                    sessionId = obj.optString("sessionId").ifBlank { null },
                    title = SensitiveSanitizer.text(obj.optString("title")),
                    timestamp = timestamp,
                )
                "TaskUpdated" -> TaskUpdated(
                    taskId = req(obj, "taskId") ?: return null,
                    title = SensitiveSanitizer.text(obj.optString("title")),
                    status = TaskStatusMapper.fromText(obj.optString("status")),
                    currentStep = obj.optString("currentStep").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                    timestamp = timestamp,
                )
                "TaskRunning" -> TaskRunning(
                    taskId = req(obj, "taskId") ?: return null,
                    title = SensitiveSanitizer.text(obj.optString("title")),
                    summary = obj.optString("summary").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                    progress = progressOrNull(obj),
                    currentStep = obj.optString("currentStep").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                    timestamp = timestamp,
                )
                "TaskWaiting" -> TaskWaiting(
                    taskId = req(obj, "taskId") ?: return null,
                    title = SensitiveSanitizer.text(obj.optString("title")),
                    summary = obj.optString("summary").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                    timestamp = timestamp,
                )
                "TaskCompleted" -> TaskCompleted(
                    taskId = req(obj, "taskId") ?: return null,
                    title = SensitiveSanitizer.text(obj.optString("title")),
                    summary = obj.optString("summary").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                    timestamp = timestamp,
                )
                "TaskFailed" -> TaskFailed(
                    taskId = req(obj, "taskId") ?: return null,
                    title = SensitiveSanitizer.text(obj.optString("title")),
                    summary = obj.optString("summary").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                    timestamp = timestamp,
                )
                "ApprovalRequired" -> ApprovalRequired(
                    approvalId = req(obj, "approvalId") ?: return null,
                    title = SensitiveSanitizer.text(obj.optString("title").ifBlank { "需要确认" }),
                    description = SensitiveSanitizer.text(obj.optString("description")),
                    command = SensitiveSanitizer.text(obj.optString("command"), 400),
                    riskLevel = risk(obj.optString("riskLevel"), obj.optString("command"), obj.optString("description")),
                    taskId = obj.optString("taskId").ifBlank { null },
                    timestamp = timestamp,
                )
                "ApprovalResolved" -> ApprovalResolved(
                    approvalId = req(obj, "approvalId") ?: return null,
                    accepted = if (obj.has("accepted")) obj.optBoolean("accepted") else null,
                    timestamp = timestamp,
                )
                "ArtifactCreated" -> ArtifactCreated(
                    artifactId = req(obj, "artifactId") ?: obj.optString("name").ifBlank { return null },
                    name = SensitiveSanitizer.text(obj.optString("name")),
                    kind = ArtifactTypeDetector.fromName(obj.optString("name"), obj.optString("mime").ifBlank { null }),
                    url = SensitiveSanitizer.url(obj.optString("url").ifBlank { null }),
                    taskId = obj.optString("taskId").ifBlank { null },
                    timestamp = timestamp,
                )
                "MessageReceived" -> MessageReceived(
                    messageId = req(obj, "messageId") ?: return null,
                    taskId = obj.optString("taskId").ifBlank { null },
                    text = SensitiveSanitizer.text(obj.optString("text")),
                    timestamp = timestamp,
                )
                "ConnectionLost" -> ConnectionLost(
                    reason = obj.optString("reason").ifBlank { null },
                    timestamp = timestamp,
                )
                "ConnectionRestored" -> ConnectionRestored(timestamp = timestamp)
                else -> ObserverUnknown(detail = type, timestamp = timestamp)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun parseSnapshot(json: String): PageSnapshot? {
        val obj = parseObject(json) ?: return null
        return try {
            val tasks = obj.optJSONArray("tasks").orEmpty().mapNotNull { item ->
                val id = item.optString("id").ifBlank { item.optString("title") }
                if (id.isBlank()) null
                else SnapshotTask(
                    id = id,
                    title = SensitiveSanitizer.text(item.optString("title").ifBlank { "Untitled" }),
                    statusText = item.optString("status").ifBlank { null },
                    step = item.optString("step").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                    agentName = item.optString("agentName").ifBlank { null },
                )
            }
            val approvalObj = obj.optJSONObject("approval")
            val approval = approvalObj?.let {
                SnapshotApproval(
                    id = it.optString("id").ifBlank { "approval" },
                    title = SensitiveSanitizer.text(it.optString("title").ifBlank { "需要确认" }),
                    description = SensitiveSanitizer.text(it.optString("description")),
                    command = SensitiveSanitizer.text(it.optString("command"), 400),
                    hasDialog = it.optBoolean("hasDialog", false),
                    hasAllow = it.optBoolean("hasAllow", false),
                    hasReject = it.optBoolean("hasReject", false),
                    waitingContext = it.optBoolean("waitingContext", false),
                    allowLabel = it.optString("allowLabel").ifBlank { null },
                    rejectLabel = it.optString("rejectLabel").ifBlank { null },
                )
            }
            PageSnapshot(
                url = SensitiveSanitizer.url(obj.optString("url")),
                title = SensitiveSanitizer.text(obj.optString("title"), 120),
                connectionHint = obj.optString("connectionHint").ifBlank { null },
                errorText = obj.optString("errorText").ifBlank { null }?.let { SensitiveSanitizer.text(it, 200) },
                sessionId = obj.optString("sessionId").ifBlank { null },
                sessionTitle = obj.optString("sessionTitle").ifBlank { null }?.let { SensitiveSanitizer.text(it) },
                tasks = tasks,
                approval = approval,
                artifacts = obj.optJSONArray("artifacts").orEmpty().mapNotNull { item ->
                    val name = item.optString("name").ifBlank { return@mapNotNull null }
                    SnapshotArtifact(
                        id = item.optString("id").ifBlank { name },
                        name = SensitiveSanitizer.text(name),
                        href = SensitiveSanitizer.url(item.optString("href").ifBlank { null }),
                        mime = item.optString("mime").ifBlank { null },
                    )
                },
                messages = obj.optJSONArray("messages").orEmpty().mapNotNull { item ->
                    val text = item.optString("text").ifBlank { return@mapNotNull null }
                    SnapshotMessage(
                        id = item.optString("id").ifBlank { text.hashCode().toString() },
                        text = SensitiveSanitizer.text(text),
                    )
                },
                observerActive = obj.optBoolean("observerActive", true),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseObject(json: String): JSONObject? {
        if (json.isBlank() || json.length > MAX_BYTES) return null
        return try {
            JSONObject(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun req(obj: JSONObject, key: String): String? =
        obj.optString(key).ifBlank { null }

    private fun progressOrNull(obj: JSONObject): Float? {
        if (!obj.has("progress") || obj.isNull("progress")) return null
        val value = obj.optDouble("progress", Double.NaN)
        if (value.isNaN()) return null
        if (value < 0.0 || value > 1.0) return null
        return value.toFloat()
    }

    private fun risk(raw: String, command: String, description: String): RiskLevel {
        return when (raw.uppercase()) {
            "LOW" -> RiskLevel.LOW
            "MEDIUM" -> RiskLevel.MEDIUM
            "HIGH" -> RiskLevel.HIGH
            "UNKNOWN" -> RiskLevel.UNKNOWN
            else -> ApprovalDetector.riskLevel(command, description)
        }
    }

    private fun JSONArray?.orEmpty(): List<JSONObject> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                optJSONObject(i)?.let { add(it) }
            }
        }
    }
}
