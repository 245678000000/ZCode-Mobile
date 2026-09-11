package app.zcode.mobile.remote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ZCodeSelectorConfig(
    val debounceMs: Int,
    val taskTitle: List<String>,
    val taskStatus: List<String>,
    val taskRow: List<String>,
    val approvalDialog: List<String>,
    val approvalAllowButton: List<String>,
    val approvalRejectButton: List<String>,
    val artifact: List<String>,
    val agentMessage: List<String>,
    val errorBanner: List<String> = emptyList(),
    val errorAction: List<String> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("debounceMs", debounceMs)
        put("taskTitle", taskTitle.toJsonArray())
        put("taskStatus", taskStatus.toJsonArray())
        put("taskRow", taskRow.toJsonArray())
        put("approvalDialog", approvalDialog.toJsonArray())
        put("approvalAllowButton", approvalAllowButton.toJsonArray())
        put("approvalRejectButton", approvalRejectButton.toJsonArray())
        put("artifact", artifact.toJsonArray())
        put("agentMessage", agentMessage.toJsonArray())
        put("errorBanner", errorBanner.toJsonArray())
        put("errorAction", errorAction.toJsonArray())
    }

    private fun List<String>.toJsonArray(): JSONArray {
        val arr = JSONArray()
        forEach { arr.put(it) }
        return arr
    }

    companion object {
        fun load(context: Context): ZCodeSelectorConfig {
            val raw = context.assets.open("zcode-selectors.json").bufferedReader().use { it.readText() }
            return fromJson(raw)
        }

        fun fromJson(raw: String): ZCodeSelectorConfig {
            val obj = JSONObject(raw)
            fun arr(key: String): List<String> {
                val a = obj.optJSONArray(key) ?: return emptyList()
                return buildList {
                    for (i in 0 until a.length()) add(a.optString(i))
                }.filter { it.isNotBlank() }
            }
            return ZCodeSelectorConfig(
                debounceMs = obj.optInt("debounceMs", 500).coerceIn(300, 1000),
                taskTitle = arr("taskTitle"),
                taskStatus = arr("taskStatus"),
                taskRow = arr("taskRow"),
                approvalDialog = arr("approvalDialog"),
                approvalAllowButton = arr("approvalAllowButton"),
                approvalRejectButton = arr("approvalRejectButton"),
                artifact = arr("artifact"),
                agentMessage = arr("agentMessage"),
                errorBanner = arr("errorBanner"),
                errorAction = arr("errorAction"),
            )
        }
    }
}
