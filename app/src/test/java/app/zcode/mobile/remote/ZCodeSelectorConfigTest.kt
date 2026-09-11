package app.zcode.mobile.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZCodeSelectorConfigTest {
    @Test
    fun parsesAllSelectorGroups() {
        val raw = """
            {
              "debounceMs": 500,
              "taskTitle": ["h1"],
              "taskStatus": ["[data-status]"],
              "taskRow": ["[role='listitem']"],
              "approvalDialog": ["[role='dialog']"],
              "approvalAllowButton": ["button"],
              "approvalRejectButton": ["button"],
              "artifact": ["a[download]"],
              "agentMessage": ["[data-testid*='message']"],
              "errorBanner": ["[role='alert']"],
              "errorAction": ["button"]
            }
        """.trimIndent()
        val config = ZCodeSelectorConfig.fromJson(raw)
        assertEquals(500, config.debounceMs)
        assertEquals(listOf("h1"), config.taskTitle)
        assertTrue(config.approvalDialog.contains("[role='dialog']"))
        assertEquals(listOf("[role='alert']"), config.errorBanner)
        assertEquals(listOf("button"), config.errorAction)
        assertTrue(config.toJson().has("errorBanner"))
    }
}
