package app.zcode.mobile.remote

import app.zcode.mobile.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskStatusMapperTest {
    @Test
    fun mapsKnownLabels() {
        assertEquals(TaskStatus.RUNNING, TaskStatusMapper.fromText("运行中"))
        assertEquals(TaskStatus.RUNNING, TaskStatusMapper.fromText("In progress"))
        assertEquals(TaskStatus.WAITING_APPROVAL, TaskStatusMapper.fromText("等待确认"))
        assertEquals(TaskStatus.COMPLETED, TaskStatusMapper.fromText("已完成"))
        assertEquals(TaskStatus.FAILED, TaskStatusMapper.fromText("失败"))
        assertEquals(TaskStatus.CANCELLED, TaskStatusMapper.fromText("cancelled"))
        assertEquals(TaskStatus.QUEUED, TaskStatusMapper.fromText("queued"))
        assertEquals(TaskStatus.UNKNOWN, TaskStatusMapper.fromText("hello"))
        assertEquals(TaskStatus.UNKNOWN, TaskStatusMapper.fromText(null))
    }
}
