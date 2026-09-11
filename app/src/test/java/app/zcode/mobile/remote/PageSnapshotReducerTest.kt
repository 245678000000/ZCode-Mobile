package app.zcode.mobile.remote

import app.zcode.mobile.model.ConnectionLost
import app.zcode.mobile.model.PageSnapshot
import app.zcode.mobile.model.SnapshotTask
import app.zcode.mobile.model.TaskCompleted
import app.zcode.mobile.model.TaskCreated
import app.zcode.mobile.model.TaskFailed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageSnapshotReducerTest {
    private fun snap(vararg tasks: SnapshotTask, ts: Long = 1_000L, hint: String = "ok", error: String? = null) =
        PageSnapshot(sessionId = "s", sessionTitle = "S", tasks = tasks.toList(), timestamp = ts, connectionHint = hint, errorText = error)

    @Test
    fun baselineDoesNotEmitTerminalEvents() {
        val first = snap(
            SnapshotTask(id = "a", title = "Old", statusText = "已完成"),
            SnapshotTask(id = "b", title = "Broken", statusText = "失败"),
        )
        val events = PageSnapshotReducer.diff(null, first)
        assertEquals(2, events.count { it is TaskCreated })
        assertTrue(events.none { it is TaskCompleted || it is TaskFailed })
    }

    @Test
    fun statusChangeAfterBaselineEmitsCompleted() {
        val first = snap(SnapshotTask(id = "a", title = "Job", statusText = "运行中"))
        val second = snap(SnapshotTask(id = "a", title = "Job", statusText = "已完成"), ts = 2_000L)
        val events = PageSnapshotReducer.diff(first, second)
        assertEquals(1, events.count { it is TaskCompleted })
    }

    @Test
    fun taskAppearingCompletedAfterBaselineEmitsCompleted() {
        val first = snap(SnapshotTask(id = "a", title = "Job", statusText = "运行中"))
        val second = snap(
            SnapshotTask(id = "a", title = "Job", statusText = "运行中"),
            SnapshotTask(id = "quick", title = "Quick", statusText = "已完成"),
            ts = 2_000L,
        )
        val events = PageSnapshotReducer.diff(first, second)
        assertEquals(1, events.count { it is TaskCompleted && it.taskId == "quick" })
    }

    @Test
    fun errorBannerBecomesConnectionLostWithReason() {
        val first = snap()
        val second = snap(ts = 2_000L, hint = "error", error = "Model request failed")
        val events = PageSnapshotReducer.diff(first, second)
        val lost = events.filterIsInstance<ConnectionLost>().single()
        assertEquals("Model request failed", lost.reason)
    }
}
