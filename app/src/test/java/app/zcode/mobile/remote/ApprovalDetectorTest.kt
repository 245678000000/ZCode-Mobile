package app.zcode.mobile.remote

import app.zcode.mobile.model.RiskLevel
import app.zcode.mobile.model.SnapshotApproval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalDetectorTest {
    @Test
    fun loneAllowTextIsNotApproval() {
        assertFalse(
            ApprovalDetector.isApprovalFromPageText(
                body = "允许访客评论",
                hasDialog = false,
                buttons = emptyList(),
            ),
        )
    }

    @Test
    fun dialogPlusButtonsIsApproval() {
        assertTrue(
            ApprovalDetector.isApprovalFromPageText(
                body = "任务暂停，等待确认。Agent 请求执行 rm -rf dist/",
                hasDialog = true,
                buttons = listOf("允许", "拒绝"),
            ),
        )
    }

    @Test
    fun snapshotRequiresTwoSignals() {
        val weak = SnapshotApproval(
            id = "1",
            title = "x",
            description = "允许",
            command = "",
            hasDialog = false,
            hasAllow = true,
            hasReject = false,
            waitingContext = false,
        )
        assertFalse(ApprovalDetector.isApproval(weak))
        val strong = weak.copy(hasDialog = true, hasReject = true, command = "rm -rf dist/")
        assertTrue(ApprovalDetector.isApproval(strong))
    }

    @Test
    fun riskRules() {
        assertEquals(RiskLevel.HIGH, ApprovalDetector.riskLevel("rm -rf legacy/", "删除文件"))
        assertEquals(RiskLevel.MEDIUM, ApprovalDetector.riskLevel("npm test", "执行命令"))
        assertEquals(RiskLevel.LOW, ApprovalDetector.riskLevel("cat README.md", "读取文件"))
        assertEquals(RiskLevel.UNKNOWN, ApprovalDetector.riskLevel("", ""))
    }
}
