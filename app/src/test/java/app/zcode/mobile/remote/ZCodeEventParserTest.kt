package app.zcode.mobile.remote

import app.zcode.mobile.model.ApprovalRequired
import app.zcode.mobile.model.TaskCompleted
import app.zcode.mobile.model.TaskRunning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZCodeEventParserTest {
    @Test
    fun parsesTaskRunning() {
        val json = """{"type":"TaskRunning","taskId":"t1","title":"Legal SkillsHub","summary":"editing","currentStep":"Home.tsx","timestamp":1}"""
        val event = ZCodeEventParser.parseEvent(json) as TaskRunning
        assertEquals("t1", event.taskId)
        assertEquals("Legal SkillsHub", event.title)
        assertNull(event.progress)
    }

    @Test
    fun parsesTaskCompleted() {
        val event = ZCodeEventParser.parseEvent(
            """{"type":"TaskCompleted","taskId":"t1","title":"案例整理","summary":"done","timestamp":2}""",
        ) as TaskCompleted
        assertEquals("案例整理", event.title)
    }

    @Test
    fun parsesApproval() {
        val event = ZCodeEventParser.parseEvent(
            """{"type":"ApprovalRequired","approvalId":"a1","title":"需要确认","description":"删除旧构建文件","command":"rm -rf dist/","timestamp":3}""",
        ) as ApprovalRequired
        assertEquals("a1", event.approvalId)
        assertTrue(event.command.contains("rm"))
    }

    @Test
    fun rejectsOversizedPayload() {
        val huge = """{"type":"TaskRunning","taskId":"x","title":"${"a".repeat(ZCodeEventParser.MAX_BYTES)}"}"""
        assertNull(ZCodeEventParser.parseEvent(huge))
    }

    @Test
    fun rejectsNonJson() {
        assertNull(ZCodeEventParser.parseEvent("not-json"))
        assertNull(ZCodeEventParser.parseEvent("<script>alert(1)</script>"))
    }

    @Test
    fun snapshotRoundTrip() {
        val snap = ZCodeEventParser.parseSnapshot(
            """{"url":"https://host/remote/abc?token=secret","title":"ZCode","connectionHint":"ok","sessionTitle":"Legal SkillsHub","observerActive":true,"tasks":[{"id":"t1","title":"Legal SkillsHub","status":"运行中","step":"修改首页"}],"approval":null,"artifacts":[{"id":"a","name":"notes.md"}],"messages":[]}""",
        )
        assertNotNull(snap)
        assertEquals("Legal SkillsHub", snap!!.sessionTitle)
        assertTrue(snap.url.contains("token").not())
        assertEquals("运行中", snap.tasks.first().statusText)
    }
}
