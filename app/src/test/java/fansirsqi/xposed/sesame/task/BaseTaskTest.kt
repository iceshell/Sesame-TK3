package fansirsqi.xposed.sesame.task

import fansirsqi.xposed.sesame.BaseTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * BaseTask核心测试
 * 测试任务生命周期、线程管理、子任务管理等核心功能
 */
class BaseTaskTest : BaseTest() {
    
    private lateinit var task: BaseTask
    
    @Before
    override fun setUp() {
        super.setUp()
        // 每个测试创建一个新的任务实例
        task = BaseTask.newInstance("test-task")
    }
    
    // ========== 1. 任务基础功能测试 ==========
    
    @Test
    fun `test task getId returns correct id`() {
        // Given
        val taskId = "custom-task-id"
        val customTask = BaseTask.newInstance(taskId)
        
        // When
        val actualId = customTask.getId()
        
        // Then
        assertEquals(taskId, actualId)
    }
    
    @Test
    fun `test task check returns true by default`() {
        // Given & When
        val result = task.check()
        
        // Then
        assertTrue(result)
    }
    
    // ========== 2. 任务启动测试 ==========
    
    @Test
    fun `test startTask creates and starts thread`() {
        // Given
        val executed = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        val testTask = BaseTask.newInstance("start-test") {
            executed.set(true)
            latch.countDown()
        }
        
        // When
        testTask.startTask()
        
        // Then
        assertNotNull("Thread should be created", testTask.thread)
        assertTrue("Thread should be alive", testTask.thread!!.isAlive)
        
        // Wait for execution
        latch.await(2, TimeUnit.SECONDS)
        assertTrue("Task should have executed", executed.get())
    }
    
    @Test
    fun `test startTask without force does not restart running task`() {
        // Given
        val executionCount = AtomicInteger(0)
        val latch = CountDownLatch(1)
        val testTask = BaseTask.newInstance("no-restart-test") {
            executionCount.incrementAndGet()
            try {
                Thread.sleep(500) // 任务运行一段时间
            } catch (e: InterruptedException) {
                // Expected
            }
            latch.countDown()
        }
        
        // When
        testTask.startTask() // 第一次启动
        val firstThread = testTask.thread
        testTask.startTask() // 第二次启动（不强制）
        val secondThread = testTask.thread
        
        // Then
        assertSame("Should be the same thread", firstThread, secondThread)
        
        latch.await(2, TimeUnit.SECONDS)
        assertEquals("Should only execute once", 1, executionCount.get())
    }
    
    @Test
    fun `test startTask with force restarts running task`() {
        // Given
        val latch = CountDownLatch(2)
        val testTask = BaseTask.newInstance("force-restart-test") {
            latch.countDown()
            try {
                Thread.sleep(5000) // 长时间运行
            } catch (e: InterruptedException) {
                // Expected when interrupted
            }
        }
        
        // When
        testTask.startTask() // 第一次启动
        Thread.sleep(100) // 等待任务启动
        val firstThread = testTask.thread
        
        testTask.startTask(force = true) // 强制重启
        val secondThread = testTask.thread
        
        // Then
        assertNotNull("First thread should exist", firstThread)
        assertNotNull("Second thread should exist", secondThread)
        assertNotSame("Should be different threads", firstThread, secondThread)
        
        latch.await(2, TimeUnit.SECONDS)
        assertEquals("Should execute twice", 0, latch.count)
    }
    
    // ========== 3. 任务停止测试 ==========
    
    @Test
    fun `test stopTask interrupts running thread`() {
        // Given
        val interrupted = AtomicBoolean(false)
        val started = CountDownLatch(1)
        val testTask = BaseTask.newInstance("stop-test") {
            started.countDown()
            try {
                Thread.sleep(10000) // 长时间运行
            } catch (e: InterruptedException) {
                interrupted.set(true)
            }
        }
        
        // When
        testTask.startTask()
        started.await(2, TimeUnit.SECONDS) // 等待任务启动
        testTask.stopTask()
        
        // Then
        assertTrue("Thread should be interrupted", interrupted.get())
    }
    
    @Test
    fun `test stopTask on non-running task does nothing`() {
        // Given
        val testTask = BaseTask.newInstance("stop-inactive-test")
        
        // When & Then (should not throw exception)
        testTask.stopTask()
        assertNull("Thread should be null", testTask.thread)
    }
    
    // ========== 4. 任务时间记录测试 ==========
    
    @Test
    fun `test task records start and end time`() {
        // Given
        val latch = CountDownLatch(1)
        val testTask = BaseTask.newInstance("time-test") {
            Thread.sleep(100)
            latch.countDown()
        }
        
        // When
        val beforeStart = System.currentTimeMillis()
        testTask.startTask()
        latch.await(2, TimeUnit.SECONDS)
        val afterEnd = System.currentTimeMillis()
        
        // Then
        assertTrue("Start time should be recorded", testTask.taskStartTime > 0)
        assertTrue("Start time should be after test start", testTask.taskStartTime >= beforeStart)
        assertTrue("End time should be recorded", testTask.taskEndTime > 0)
        assertTrue("End time should be after start time", testTask.taskEndTime >= testTask.taskStartTime)
        assertTrue("End time should be before test end", testTask.taskEndTime <= afterEnd)
    }
    
    // ========== 5. 子任务管理测试 ==========
    
    @Test
    fun `test addChildTask adds task to map`() {
        // Given
        val childTask = BaseTask.newInstance("child-1")
        
        // When
        task.addChildTask(childTask)
        
        // Then
        assertTrue("Should have child task", task.hasChildTask("child-1"))
        assertEquals("Should have 1 child task", 1, task.countChildTask())
        assertSame("Should return same child task", childTask, task.getChildTask("child-1"))
    }
    
    @Test
    fun `test addChildTask replaces existing task with same id`() {
        // Given
        val firstChild = BaseTask.newInstance("child-1") {
            Thread.sleep(5000)
        }
        val secondChild = BaseTask.newInstance("child-1") {
            Thread.sleep(5000)
        }
        
        // When
        task.addChildTask(firstChild)
        Thread.sleep(100) // 等待第一个子任务启动
        task.addChildTask(secondChild) // 添加同ID的第二个任务
        
        // Then
        assertEquals("Should still have 1 child task", 1, task.countChildTask())
        assertSame("Should return second child task", secondChild, task.getChildTask("child-1"))
    }
    
    @Test
    fun `test removeChildTask removes task from map`() {
        // Given
        val childTask = BaseTask.newInstance("child-to-remove")
        task.addChildTask(childTask)
        assertTrue("Should have child task initially", task.hasChildTask("child-to-remove"))
        
        // When
        task.removeChildTask("child-to-remove")
        
        // Then
        assertFalse("Should not have child task after removal", task.hasChildTask("child-to-remove"))
        assertEquals("Should have 0 child tasks", 0, task.countChildTask())
        assertNull("Should return null for removed task", task.getChildTask("child-to-remove"))
    }
    
    @Test
    fun `test getChildTask returns null for non-existent task`() {
        // When
        val result = task.getChildTask("non-existent")
        
        // Then
        assertNull("Should return null for non-existent task", result)
    }
    
    @Test
    fun `test countChildTask returns correct count`() {
        // Given
        val child1 = BaseTask.newInstance("child-1")
        val child2 = BaseTask.newInstance("child-2")
        val child3 = BaseTask.newInstance("child-3")
        
        // When & Then
        assertEquals("Should have 0 children initially", 0, task.countChildTask())
        
        task.addChildTask(child1)
        assertEquals("Should have 1 child", 1, task.countChildTask())
        
        task.addChildTask(child2)
        assertEquals("Should have 2 children", 2, task.countChildTask())
        
        task.addChildTask(child3)
        assertEquals("Should have 3 children", 3, task.countChildTask())
        
        task.removeChildTask("child-2")
        assertEquals("Should have 2 children after removal", 2, task.countChildTask())
    }
    
    // ========== 6. 并发安全测试 ==========
    
    @Test
    fun `test concurrent child task operations are thread-safe`() {
        // Given
        val iterations = 50
        val latch = CountDownLatch(2)
        
        // When - 两个线程同时添加子任务
        val thread1 = Thread {
            repeat(iterations) { i ->
                task.addChildTask(BaseTask.newInstance("thread1-child-$i"))
            }
            latch.countDown()
        }
        
        val thread2 = Thread {
            repeat(iterations) { i ->
                task.addChildTask(BaseTask.newInstance("thread2-child-$i"))
            }
            latch.countDown()
        }
        
        thread1.start()
        thread2.start()
        latch.await(5, TimeUnit.SECONDS)
        
        // Then
        assertEquals("Should have correct number of children", 
            iterations * 2, task.countChildTask())
    }
    
    // ========== 7. 辅助方法测试 ==========
    
    @Test
    fun `test shutdownAndWait with timeout stops thread`() {
        // Given
        val interrupted = AtomicBoolean(false)
        val thread = Thread {
            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                interrupted.set(true)
            }
        }
        thread.start()
        
        // When
        BaseTask.shutdownAndWait(thread, 1, TimeUnit.SECONDS)
        
        // Then
        assertTrue("Thread should be interrupted", interrupted.get())
        assertFalse("Thread should not be alive", thread.isAlive)
    }
    
    @Test
    fun `test shutdownAndWait with null thread does not throw exception`() {
        // When & Then (should not throw exception)
        BaseTask.shutdownAndWait(null, 1, TimeUnit.SECONDS)
    }
    
    @Test
    fun `test shutdownAndWait with negative timeout interrupts immediately`() {
        // Given
        val interrupted = AtomicBoolean(false)
        val thread = Thread {
            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                interrupted.set(true)
            }
        }
        thread.start()
        
        // When
        BaseTask.shutdownAndWait(thread, -1, TimeUnit.SECONDS)
        
        // Then
        assertTrue("Thread should be interrupted", interrupted.get())
    }
}
