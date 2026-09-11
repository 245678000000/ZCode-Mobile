package app.zcode.mobile.remote

import app.zcode.mobile.model.ConnectionLost
import app.zcode.mobile.model.ConnectionRestored
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionEventDedupeTest {
    @Test
    fun repeatedConnectionEventsAreNotSwallowed() {
        val repo = ZCodeEventRepository()
        assertTrue(repo.ingestEvent(ConnectionLost(reason = "x", timestamp = 1)))
        assertTrue(repo.ingestEvent(ConnectionRestored(timestamp = 2)))
        assertTrue(repo.ingestEvent(ConnectionLost(reason = "x", timestamp = 3)))
        assertTrue(repo.ingestEvent(ConnectionRestored(timestamp = 4)))
    }
}
