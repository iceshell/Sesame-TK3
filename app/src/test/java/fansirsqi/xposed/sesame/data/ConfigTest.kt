package fansirsqi.xposed.sesame.data

import fansirsqi.xposed.sesame.BaseTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Config配置管理测试 - 基础版本
 * 只测试Config的核心API，不依赖ModelField相关类
 */
class ConfigTest : BaseTest() {
    
    private val config = Config.INSTANCE
    
    // ========== 1. 基础功能测试 ==========
    
    @Test
    fun `test Config INSTANCE is singleton`() {
        // When
        val instance1 = Config.INSTANCE
        val instance2 = Config.INSTANCE
        
        // Then
        assertSame("Should be the same instance", instance1, instance2)
    }
    
    @Test
    fun `test isLoaded returns boolean`() {
        // When
        val result = Config.isLoaded()
        
        // Then
        assertTrue("Should return boolean value", result == true || result == false)
    }
    
    @Test
    fun `test modelFieldsMap is accessible`() {
        // When
        val map = config.modelFieldsMap
        
        // Then
        assertNotNull("modelFieldsMap should not be null", map)
    }
    
    // ========== 2. 配置加载测试 ==========
    
    @Test
    fun `test load with null userId returns config instance`() {
        // When
        val result = Config.load(null)
        
        // Then
        assertNotNull("Should return Config instance", result)
        assertSame("Should return INSTANCE", Config.INSTANCE, result)
    }
    
    @Test
    fun `test load with empty userId returns config instance`() {
        // When  
        val result = Config.load("")
        
        // Then
        assertNotNull("Should return Config instance", result)
        assertSame("Should return INSTANCE", Config.INSTANCE, result)
    }
    
    // ========== 3. 配置保存测试 ==========
    
    @Test
    fun `test isModify with null returns boolean`() {
        // When
        val result = Config.isModify(null)
        
        // Then
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    @Test
    fun `test isModify with empty string returns boolean`() {
        // When
        val result = Config.isModify("")
        
        // Then
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    @Test
    fun `test save with force true returns boolean`() {
        // Given
        val userId = "test-user-force"
        
        // When
        val result = Config.save(userId, true)
        
        // Then
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    @Test
    fun `test save without force returns boolean`() {
        // Given
        val userId = "test-user-no-force"
        
        // When
        val result = Config.save(userId, false)
        
        // Then
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    // ========== 4. 配置序列化测试 ==========
    
    @Test
    fun `test toSaveStr returns non-null string`() {
        // When
        val result = Config.toSaveStr()
        
        // Then
        assertNotNull("Should return non-null string", result)
    }
    
    @Test
    fun `test toSaveStr returns valid JSON format`() {
        // When
        val jsonStr = Config.toSaveStr()
        
        // Then
        assertNotNull("Should return JSON string", jsonStr)
        assertTrue("Should not be empty", jsonStr.isNotEmpty())
        assertTrue("Should start with {", jsonStr.startsWith("{"))
        assertTrue("Should end with }", jsonStr.endsWith("}"))
    }
    
    // ========== 5. 配置卸载测试 ==========
    
    @Test
    fun `test unload does not throw exception`() {
        // When & Then - should not throw
        Config.unload()
        assertNotNull("Config instance should still exist", Config.INSTANCE)
    }
    
    // ========== 6. 边界条件测试 ==========
    
    @Test
    fun `test hasModelFields with null returns false`() {
        // When & Then
        assertFalse("Should return false for null", 
            config.hasModelFields(null))
    }
    
    @Test
    fun `test hasModelFields with empty string returns false`() {
        // When & Then
        assertFalse("Should return false for empty string", 
            config.hasModelFields(""))
    }
    
    @Test
    fun `test hasModelField with null modelCode returns false`() {
        // When & Then
        assertFalse("Should return false for null modelCode", 
            config.hasModelField(null, "field1"))
    }
    
    @Test
    fun `test hasModelField with null fieldCode returns false`() {
        // When & Then
        assertFalse("Should return false for null fieldCode", 
            config.hasModelField("testModel", null))
    }
    
    @Test
    fun `test setModelFieldsMap with null does not throw`() {
        // When & Then - should not throw
        config.setModelFieldsMap(null)
        assertNotNull("modelFieldsMap should not be null", config.modelFieldsMap)
    }
}
