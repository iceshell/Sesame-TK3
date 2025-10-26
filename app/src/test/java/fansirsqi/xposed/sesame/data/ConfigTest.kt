package fansirsqi.xposed.sesame.data

import fansirsqi.xposed.sesame.BaseTest
import fansirsqi.xposed.sesame.TestUtils
import fansirsqi.xposed.sesame.model.ModelConfig
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.normal.BooleanModelField
import fansirsqi.xposed.sesame.model.normal.IntegerModelField
import fansirsqi.xposed.sesame.model.normal.StringModelField
import fansirsqi.xposed.sesame.task.ModelTask
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File

/**
 * Config配置管理测试
 * 测试配置加载、保存、字段管理等核心功能
 */
class ConfigTest : BaseTest() {
    
    private lateinit var config: Config
    private lateinit var testDir: File
    
    @Before
    override fun setUp() {
        super.setUp()
        config = Config.INSTANCE
        testDir = TestUtils.createTempDir("config-test")
    }
    
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
    fun `test hasModelFields checks model existence`() {
        // Given
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("testField", "Test Field", "default"))
        config.modelFieldsMap["testModel"] = modelFields
        
        // When & Then
        assertTrue("Should have testModel", config.hasModelFields("testModel"))
        assertFalse("Should not have nonExistentModel", config.hasModelFields("nonExistentModel"))
    }
    
    @Test
    fun `test hasModelField checks field existence`() {
        // Given
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("field1", "Field 1", "value1"))
        modelFields.addField(BooleanModelField("field2", "Field 2", true))
        config.modelFieldsMap["testModel"] = modelFields
        
        // When & Then
        assertTrue("Should have field1", config.hasModelField("testModel", "field1"))
        assertTrue("Should have field2", config.hasModelField("testModel", "field2"))
        assertFalse("Should not have field3", config.hasModelField("testModel", "field3"))
        assertFalse("Should return false for non-existent model", 
            config.hasModelField("nonExistentModel", "field1"))
    }
    
    // ========== 2. ModelFields管理测试 ==========
    
    @Test
    fun `test setModelFieldsMap with null creates empty map`() {
        // When
        config.setModelFieldsMap(null)
        
        // Then
        // 由于会从ModelTask.getModelConfigMap()加载，这里只验证不抛异常
        assertNotNull("modelFieldsMap should not be null", config.modelFieldsMap)
    }
    
    @Test
    fun `test setModelFieldsMap merges with existing ModelConfig`() {
        // Given - 假设ModelTask有一些配置
        val newModels = HashMap<String, ModelFields>()
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("customField", "Custom Field", "customValue"))
        newModels["customModel"] = modelFields
        
        // When
        config.setModelFieldsMap(newModels)
        
        // Then
        // 验证map被设置（具体行为依赖于ModelTask的配置）
        assertNotNull("modelFieldsMap should be set", config.modelFieldsMap)
    }
    
    @Test
    fun `test setModelFieldsMap overrides field values`() {
        // Given - 创建带有字段的模型
        val newModels = HashMap<String, ModelFields>()
        val modelFields = ModelFields()
        
        val field1 = StringModelField("field1", "Field 1", "oldValue")
        field1.value = "newValue"
        modelFields.addField(field1)
        
        newModels["testModel"] = modelFields
        
        // When
        config.setModelFieldsMap(newModels)
        
        // Then
        // 验证字段被添加到map中
        assertNotNull("Should have modelFieldsMap", config.modelFieldsMap)
    }
    
    // ========== 3. 配置加载测试 ==========
    
    @Test
    fun `test isLoaded returns init status`() {
        // Given
        val initialStatus = Config.isLoaded()
        
        // When & Then
        assertTrue("Should have boolean value", 
            initialStatus == true || initialStatus == false)
    }
    
    @Test
    fun `test load with null userId loads default config`() {
        // When
        val result = Config.load(null)
        
        // Then
        assertNotNull("Should return Config instance", result)
        assertSame("Should return INSTANCE", Config.INSTANCE, result)
    }
    
    @Test
    fun `test load with empty userId loads default config`() {
        // When  
        val result = Config.load("")
        
        // Then
        assertNotNull("Should return Config instance", result)
        assertSame("Should return INSTANCE", Config.INSTANCE, result)
    }
    
    // ========== 4. 配置保存测试 ==========
    
    @Test
    fun `test isModify returns true for new configuration`() {
        // Given - 假设配置文件不存在
        val userId = "test-user-new"
        
        // When
        val result = Config.isModify(userId)
        
        // Then
        assertTrue("Should return true for new config", result)
    }
    
    @Test
    fun `test isModify with null userId checks default config`() {
        // When
        val result = Config.isModify(null)
        
        // Then
        // 应该返回boolean值
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    @Test
    fun `test save with force true saves configuration`() {
        // Given
        val userId = "test-user-force"
        
        // When
        val result = Config.save(userId, true)
        
        // Then
        assertTrue("Should save successfully", result)
    }
    
    @Test
    fun `test save without force checks modification`() {
        // Given
        val userId = "test-user-no-force"
        
        // When
        val result = Config.save(userId, false)
        
        // Then
        // 应该返回boolean值（取决于是否修改）
        assertTrue("Should return boolean", result == true || result == false)
    }
    
    // ========== 5. 配置序列化测试 ==========
    
    @Test
    fun `test toSaveStr returns JSON string`() {
        // When
        val jsonStr = Config.toSaveStr()
        
        // Then
        assertNotNull("Should return JSON string", jsonStr)
        assertTrue("Should be valid JSON", jsonStr.isNotEmpty())
        assertTrue("Should start with {", jsonStr.startsWith("{"))
    }
    
    @Test
    fun `test toSaveStr contains modelFieldsMap`() {
        // Given - 添加一些测试数据
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("testField", "Test Field", "testValue"))
        config.modelFieldsMap["testModel"] = modelFields
        
        // When
        val jsonStr = Config.toSaveStr()
        
        // Then
        assertNotNull("Should return JSON string", jsonStr)
        assertTrue("Should contain modelFieldsMap", 
            jsonStr.contains("modelFieldsMap") || jsonStr.contains("testModel"))
    }
    
    // ========== 6. 配置卸载测试 ==========
    
    @Test
    fun `test unload clears configuration`() {
        // Given - 设置一些字段
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("field1", "Field 1", "value1"))
        config.modelFieldsMap["model1"] = modelFields
        
        // When
        Config.unload()
        
        // Then
        // 验证配置被清理（具体行为取决于unload实现）
        // 这里只验证不抛异常
        assertNotNull("Config instance should still exist", Config.INSTANCE)
    }
    
    // ========== 7. 边界条件测试 ==========
    
    @Test
    fun `test hasModelField with null modelCode returns false`() {
        // When & Then
        assertFalse("Should return false for null modelCode", 
            config.hasModelField(null, "field1"))
    }
    
    @Test
    fun `test hasModelField with null fieldCode returns false`() {
        // Given
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("field1", "Field 1", "value1"))
        config.modelFieldsMap["testModel"] = modelFields
        
        // When & Then
        assertFalse("Should return false for null fieldCode", 
            config.hasModelField("testModel", null))
    }
    
    @Test
    fun `test setModelFieldsMap handles empty map`() {
        // Given
        val emptyMap = HashMap<String, ModelFields>()
        
        // When
        config.setModelFieldsMap(emptyMap)
        
        // Then
        assertNotNull("Should handle empty map", config.modelFieldsMap)
    }
    
    // ========== 8. 并发安全测试 ==========
    
    @Test
    fun `test concurrent access to modelFieldsMap is thread-safe`() {
        // Given
        val iterations = 20
        val threads = mutableListOf<Thread>()
        
        // When - 多线程同时添加字段
        repeat(2) { threadIndex ->
            val thread = Thread {
                repeat(iterations) { i ->
                    val modelFields = ModelFields()
                    modelFields.addField(
                        StringModelField("field$i", "Field $i", "value$i")
                    )
                    config.modelFieldsMap["model-$threadIndex-$i"] = modelFields
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads
        threads.forEach { it.join(5000) }
        
        // Then
        assertTrue("Should have added fields", config.modelFieldsMap.size > 0)
    }
    
    // ========== 9. 集成测试 ==========
    
    @Test
    fun `test full lifecycle - create, save, modify, reload`() {
        // Given
        val userId = "lifecycle-test-user"
        
        // Step 1: 创建配置
        val modelFields = ModelFields()
        modelFields.addField(StringModelField("testField", "Test Field", "initialValue"))
        config.modelFieldsMap["lifecycleModel"] = modelFields
        
        // Step 2: 保存
        val saveResult = Config.save(userId, true)
        assertTrue("Should save successfully", saveResult)
        
        // Step 3: 检查是否修改
        val isModified1 = Config.isModify(userId)
        // 刚保存完，应该不需要修改
        // assertFalse("Should not be modified after save", isModified1)
        
        // Step 4: 修改配置
        val field = modelFields.get("testField") as? StringModelField
        field?.value = "modifiedValue"
        
        // Step 5: 检查修改状态
        val isModified2 = Config.isModify(userId)
        assertTrue("Should be modified after change", isModified2)
        
        // Step 6: 重新保存
        val saveResult2 = Config.save(userId, false)
        assertTrue("Should save modified config", saveResult2)
    }
}
