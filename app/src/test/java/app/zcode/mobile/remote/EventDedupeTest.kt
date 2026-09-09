package app.zcode.mobile.remote

import app.zcode.mobile.model.TaskRunning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDedupeTest {
    @Test
    fun sameRunningEventIsDeduped() {
        val repo = ZCodeEventRepository()
        val a = TaskRunning(taskId = "t1", title = "A", summary = "s", progress = null, currentStep = "step")
        val b = a.copy(timestamp = a.timestamp + 10)
        assertTrue(repo.ingestEvent(a))
        assertFalse(repo.ingestEvent(b))
        assertEquals(1, repo.events.value.size)
    }

    @Test
    fun differentStepIsNotDeduped() {
        val repo = ZCodeEventRepository()
        assertTrue(repo.ingestEvent(TaskRunning("t1", "A", null, null, "one")))
        assertTrue(repo.ingestEvent(TaskRunning("t1", "A", null, null, "two")))
        assertEquals(2, repo.events.value.size)
    }
}
