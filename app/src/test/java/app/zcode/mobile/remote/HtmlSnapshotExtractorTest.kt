package app.zcode.mobile.remote

import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlSnapshotExtractorTest {
    @Test
    fun extractsApprovalFromFixture() {
        val html = javaClass.classLoader!!.getResource("fixtures/approval-dialog.html")!!.readText()
        val snap = HtmlSnapshotExtractor.extract(html)
        assertTrue(ApprovalDetector.isApproval(snap.approval))
        assertTrue(snap.approval!!.command.contains("rm"))
    }

    @Test
    fun extractsArtifactsFromFixture() {
        val html = javaClass.classLoader!!.getResource("fixtures/task-completed-artifacts.html")!!.readText()
        val snap = HtmlSnapshotExtractor.extract(html)
        assertTrue(snap.artifacts.any { it.name.contains("implementation.md") })
        assertTrue(snap.artifacts.any { it.name.contains("png") })
    }

    @Test
    fun runningFixtureHasTask() {
        val html = javaClass.classLoader!!.getResource("fixtures/task-running.html")!!.readText()
        val snap = HtmlSnapshotExtractor.extract(html)
        assertTrue(snap.tasks.isNotEmpty())
        val events = PageSnapshotReducer.diff(null, snap)
        assertTrue(events.isNotEmpty())
    }
}
