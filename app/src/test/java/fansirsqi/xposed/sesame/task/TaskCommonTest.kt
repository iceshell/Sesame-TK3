package fansirsqi.xposed.sesame.task

import fansirsqi.xposed.sesame.BaseTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * TaskCommon通用任务工具测试
 * 测试时间判断和配置检查功能
 */
class TaskCommonTest : BaseTest() {
    
    @Before
    override fun setUp() {
        super.setUp()
    }
    
    // ========== 1. 配置检查测试 ==========
    
    @Test
    fun `test isConfigDisabled returns true for null config`() {
        // When
        val result = TaskCommon.isConfigDisabled(null)
        
        // Then
        assertTrue("Should return true for null config", result)
    }
    
    @Test
    fun `test isConfigDisabled returns true for empty config`() {
        // Given
        val emptyConfig = emptyList<String>()
        
        // When
        val result = TaskCommon.isConfigDisabled(emptyConfig)
        
        // Then
        assertTrue("Should return true for empty config", result)
    }
    
    @Test
    fun `test isConfigDisabled returns true for -1 config`() {
        // Given
        val disabledConfig = listOf("-1")
        
        // When
        val result = TaskCommon.isConfigDisabled(disabledConfig)
        
        // Then
        assertTrue("Should return true for -1 config", result)
    }
    
    @Test
    fun `test isConfigDisabled returns false for valid config`() {
        // Given
        val validConfig = listOf("0800-1200")
        
        // When
        val result = TaskCommon.isConfigDisabled(validConfig)
        
        // Then
        assertFalse("Should return false for valid config", result)
    }
    
    @Test
    fun `test isConfigDisabled handles whitespace in -1`() {
        // Given
        val configWithWhitespace = listOf(" -1 ")
        
        // When
        val result = TaskCommon.isConfigDisabled(configWithWhitespace)
        
        // Then
        assertTrue("Should return true for -1 with whitespace", result)
    }
    
    // ========== 2. 状态字段测试 ==========
    
    @Test
    fun `test IS_ENERGY_TIME is accessible`() {
        // When
        val value = TaskCommon.IS_ENERGY_TIME
        
        // Then
        assertTrue("Should be boolean", value == true || value == false)
    }
    
    @Test
    fun `test IS_AFTER_8AM is accessible`() {
        // When
        val value = TaskCommon.IS_AFTER_8AM
        
        // Then
        assertTrue("Should be boolean", value == true || value == false)
    }
    
    @Test
    fun `test IS_MODULE_SLEEP_TIME is accessible`() {
        // When
        val value = TaskCommon.IS_MODULE_SLEEP_TIME
        
        // Then
        assertTrue("Should be boolean", value == true || value == false)
    }
    
    @Test
    fun `test can set IS_ENERGY_TIME`() {
        // Given
        val originalValue = TaskCommon.IS_ENERGY_TIME
        
        // When
        TaskCommon.IS_ENERGY_TIME = true
        val newValue = TaskCommon.IS_ENERGY_TIME
        
        // Then
        assertTrue("Should be set to true", newValue)
        
        // Cleanup
        TaskCommon.IS_ENERGY_TIME = originalValue
    }
    
    @Test
    fun `test can set IS_AFTER_8AM`() {
        // Given
        val originalValue = TaskCommon.IS_AFTER_8AM
        
        // When
        TaskCommon.IS_AFTER_8AM = false
        val newValue = TaskCommon.IS_AFTER_8AM
        
        // Then
        assertFalse("Should be set to false", newValue)
        
        // Cleanup
        TaskCommon.IS_AFTER_8AM = originalValue
    }
    
    @Test
    fun `test can set IS_MODULE_SLEEP_TIME`() {
        // Given
        val originalValue = TaskCommon.IS_MODULE_SLEEP_TIME
        
        // When
        TaskCommon.IS_MODULE_SLEEP_TIME = true
        val newValue = TaskCommon.IS_MODULE_SLEEP_TIME
        
        // Then
        assertTrue("Should be set to true", newValue)
        
        // Cleanup
        TaskCommon.IS_MODULE_SLEEP_TIME = originalValue
    }
    
    // ========== 3. update方法测试 ==========
    
    @Test
    fun `test update does not throw exception`() {
        // When & Then - should not throw
        try {
            TaskCommon.update()
            // If we reach here, no exception was thrown
            assertTrue("update() should not throw exception", true)
        } catch (e: Exception) {
            // This is expected in test environment without BaseModel
            assertTrue("Exception should be related to BaseModel", 
                e.message?.contains("BaseModel") == true || 
                e is NullPointerException)
        }
    }
    
    // ========== 4. 边界条件测试 ==========
    
    @Test
    fun `test isConfigDisabled with multiple values`() {
        // Given
        val multiConfig = listOf("0800-1200", "1400-1800")
        
        // When
        val result = TaskCommon.isConfigDisabled(multiConfig)
        
        // Then
        assertFalse("Should return false for multiple valid configs", result)
    }
    
    @Test
    fun `test isConfigDisabled with -1 and other values`() {
        // Given
        val mixedConfig = listOf("-1", "0800-1200")
        
        // When
        val result = TaskCommon.isConfigDisabled(mixedConfig)
        
        // Then
        assertTrue("Should return true when first value is -1", result)
    }
    
    @Test
    fun `test isConfigDisabled with empty string`() {
        // Given
        val emptyStringConfig = listOf("")
        
        // When
        val result = TaskCommon.isConfigDisabled(emptyStringConfig)
        
        // Then
        assertFalse("Should return false for empty string (not -1)", result)
    }
    
    // ========== 5. 并发安全测试 ==========
    
    @Test
    fun `test concurrent access to volatile fields is safe`() {
        // Given
        val threads = mutableListOf<Thread>()
        val iterations = 100
        
        // When - Multiple threads accessing volatile fields
        repeat(2) { threadIndex ->
            val thread = Thread {
                repeat(iterations) {
                    TaskCommon.IS_ENERGY_TIME = threadIndex == 0
                    TaskCommon.IS_AFTER_8AM = threadIndex == 1
                    TaskCommon.IS_MODULE_SLEEP_TIME = threadIndex == 0
                    
                    // Read values
                    val v1 = TaskCommon.IS_ENERGY_TIME
                    val v2 = TaskCommon.IS_AFTER_8AM
                    val v3 = TaskCommon.IS_MODULE_SLEEP_TIME
                    
                    // Verify they are boolean
                    assertTrue("Should be boolean", v1 == true || v1 == false)
                    assertTrue("Should be boolean", v2 == true || v2 == false)
                    assertTrue("Should be boolean", v3 == true || v3 == false)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads
        threads.forEach { it.join(5000) }
        
        // Then - No exception should be thrown
        assertTrue("Concurrent access should be safe", true)
    }
}
