package fansirsqi.xposed.sesame.util

import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * PerformanceMonitor 单元测试
 */
class PerformanceMonitorTest {

    @Before
    fun setup() {
        // Mock Log对象
        mockkObject(Log)
        every { Log.record(any(), any()) } just Runs
        every { Log.runtime(any(), any()) } just Runs
        every { Log.error(any(), any()) } just Runs
        
        // 清空统计数据
        PerformanceMonitor.clearStats()
        PerformanceMonitor.enabled = true
        PerformanceMonitor.slowMethodThreshold = 100
    }

    @After
    fun teardown() {
        PerformanceMonitor.clearStats()
        unmockkAll()
    }

    // ========== 基础监控测试 ==========

    @Test
    fun `monitor should execute block and return result`() {
        val result = PerformanceMonitor.monitor("testMethod") {
            "success"
        }
        assertEquals("success", result)
    }

    @Test
    fun `monitor should record method call statistics`() {
        PerformanceMonitor.monitor("testMethod") {
            Thread.sleep(10)
            "success"
        }
        
        val stats = PerformanceMonitor.getMethodStats("testMethod")
        assertTrue(stats?.contains("调用次数: 1") == true)
        assertTrue(stats?.contains("testMethod") == true)
    }

    @Test
    fun `monitor should record multiple calls`() {
        repeat(5) {
            PerformanceMonitor.monitor("testMethod") {
                Thread.sleep(5)
            }
        }
        
        val stats = PerformanceMonitor.getMethodStats("testMethod")
        assertTrue(stats?.contains("调用次数: 5") == true)
    }

    @Test
    fun `monitor should propagate exceptions`() {
        assertFailsWith<RuntimeException> {
            PerformanceMonitor.monitor("testMethod") {
                throw RuntimeException("test error")
            }
        }
        
        val stats = PerformanceMonitor.getMethodStats("testMethod")
        assertTrue(stats?.contains("错误次数: 1") == true)
    }

    @Test
    fun `monitor should detect slow methods`() {
        PerformanceMonitor.slowMethodThreshold = 50
        
        PerformanceMonitor.monitor("slowMethod") {
            Thread.sleep(60)
        }
        
        verify { Log.runtime("PerformanceMonitor", match { it.contains("慢方法检测") }) }
    }

    // ========== 协程监控测试 ==========

    @Test
    fun `monitorSuspend should work with coroutines`() = runTest {
        val result = PerformanceMonitor.monitorSuspend("suspendMethod") {
            delay(10)
            "coroutine success"
        }
        assertEquals("coroutine success", result)
        
        val stats = PerformanceMonitor.getMethodStats("suspendMethod")
        assertTrue(stats?.contains("调用次数: 1") == true)
    }

    @Test
    fun `monitorSuspend should detect slow coroutine methods`() = runTest {
        PerformanceMonitor.slowMethodThreshold = 50
        
        PerformanceMonitor.monitorSuspend("slowSuspendMethod") {
            delay(60)
        }
        
        verify { Log.runtime("PerformanceMonitor", match { it.contains("慢协程方法检测") }) }
    }

    // ========== 统计测试 ==========

    @Test
    fun `getMethodStats should return null for unknown method`() {
        val stats = PerformanceMonitor.getMethodStats("unknownMethod")
        assertEquals(null, stats)
    }

    @Test
    fun `getMethodStats should return formatted statistics`() {
        PerformanceMonitor.monitor("testMethod") {
            Thread.sleep(10)
        }
        
        val stats = PerformanceMonitor.getMethodStats("testMethod")
        assertTrue(stats?.contains("方法: testMethod") == true)
        assertTrue(stats?.contains("调用次数:") == true)
        assertTrue(stats?.contains("总耗时:") == true)
        assertTrue(stats?.contains("平均耗时:") == true)
    }

    @Test
    fun `generateReport should return report when data exists`() {
        PerformanceMonitor.monitor("method1") { Thread.sleep(10) }
        PerformanceMonitor.monitor("method2") { Thread.sleep(5) }
        
        val report = PerformanceMonitor.generateReport()
        assertTrue(report.contains("性能监控报告"))
        assertTrue(report.contains("method1"))
        assertTrue(report.contains("method2"))
    }

    @Test
    fun `generateReport should return empty message when no data`() {
        val report = PerformanceMonitor.generateReport()
        assertEquals("暂无性能数据", report)
    }

    @Test
    fun `generateReport should sort by total time`() {
        PerformanceMonitor.monitor("fastMethod") { Thread.sleep(5) }
        PerformanceMonitor.monitor("slowMethod") { Thread.sleep(20) }
        
        val report = PerformanceMonitor.generateReport()
        val slowIndex = report.indexOf("slowMethod")
        val fastIndex = report.indexOf("fastMethod")
        
        // slowMethod should appear before fastMethod (higher total time)
        assertTrue(slowIndex < fastIndex)
    }

    // ========== 启用/禁用测试 ==========

    @Test
    fun `monitor should not record when disabled`() {
        PerformanceMonitor.enabled = false
        
        PerformanceMonitor.monitor("testMethod") {
            Thread.sleep(10)
        }
        
        val stats = PerformanceMonitor.getMethodStats("testMethod")
        assertEquals(null, stats)
    }

    @Test
    fun `monitor should still execute block when disabled`() {
        PerformanceMonitor.enabled = false
        
        val result = PerformanceMonitor.monitor("testMethod") {
            "success"
        }
        
        assertEquals("success", result)
    }

    // ========== 清空统计测试 ==========

    @Test
    fun `clearStats should remove all statistics`() {
        PerformanceMonitor.monitor("method1") { }
        PerformanceMonitor.monitor("method2") { }
        
        PerformanceMonitor.clearStats()
        
        assertEquals(null, PerformanceMonitor.getMethodStats("method1"))
        assertEquals(null, PerformanceMonitor.getMethodStats("method2"))
    }

    // ========== 内存监控测试 ==========

    @Test
    fun `getMemoryInfo should return memory statistics`() {
        val memInfo = PerformanceMonitor.getMemoryInfo()
        
        assertTrue(memInfo.contains("内存使用情况"))
        assertTrue(memInfo.contains("已用内存"))
        assertTrue(memInfo.contains("空闲内存"))
        assertTrue(memInfo.contains("总内存"))
        assertTrue(memInfo.contains("最大内存"))
    }

    // ========== DSL扩展测试 ==========

    @Test
    fun `monitored extension should work`() {
        val result = monitored("testMethod") {
            "extension success"
        }
        assertEquals("extension success", result)
        
        val stats = PerformanceMonitor.getMethodStats("testMethod")
        assertTrue(stats?.contains("调用次数: 1") == true)
    }

    @Test
    fun `monitoredSuspend extension should work`() = runTest {
        val result = monitoredSuspend("suspendMethod") {
            delay(10)
            "suspend extension success"
        }
        assertEquals("suspend extension success", result)
        
        val stats = PerformanceMonitor.getMethodStats("suspendMethod")
        assertTrue(stats?.contains("调用次数: 1") == true)
    }

    // ========== monitorAndLog 测试 ==========

    @Test
    fun `monitorAndLog should execute and log on success`() {
        val result = PerformanceMonitor.monitorAndLog("testMethod") {
            "success"
        }
        assertEquals("success", result)
    }

    @Test
    fun `monitorAndLog should log error on exception`() {
        assertFailsWith<RuntimeException> {
            PerformanceMonitor.monitorAndLog("testMethod") {
                throw RuntimeException("test error")
            }
        }
        
        verify { Log.error("PerformanceMonitor", match { it.contains("执行异常") }) }
    }

    // ========== 边界情况测试 ==========

    @Test
    fun `monitor should handle zero duration`() {
        PerformanceMonitor.monitor("instantMethod") {
            // Instant execution
        }
        
        val stats = PerformanceMonitor.getMethodStats("instantMethod")
        assertTrue(stats != null)
    }

    @Test
    fun `monitor should handle very long method name`() {
        val longName = "a".repeat(1000)
        PerformanceMonitor.monitor(longName) {
            "success"
        }
        
        val stats = PerformanceMonitor.getMethodStats(longName)
        assertTrue(stats != null)
    }
}
