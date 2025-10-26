package fansirsqi.xposed.sesame

import org.junit.Test
import org.junit.Assert.*
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import kotlin.test.assertTrue as ktAssertTrue
import kotlin.test.assertEquals as ktAssertEquals

/**
 * 测试框架验证测试
 * 确保测试环境正确配置
 */
class TestFrameworkTest : BaseTest() {
    
    @Test
    fun `test JUnit is working`() {
        // Given
        val expected = 42
        val actual = 42
        
        // Then
        assertEquals(expected, actual)
        assertTrue(actual > 0)
        assertNotNull(expected)
    }
    
    @Test
    fun `test Kotlin test extensions`() {
        // Given
        val list = listOf(1, 2, 3)
        
        // Then
        ktAssertEquals(3, list.size)
        ktAssertTrue(list.contains(2))
        ktAssertTrue(list.isNotEmpty())
    }
    
    @Test
    fun `test MockK is working`() {
        // Given
        val mockFile = mockk<java.io.File>()
        every { mockFile.exists() } returns true
        every { mockFile.name } returns "test.txt"
        every { mockFile.path } returns "/test/test.txt"
        
        // When
        val exists = mockFile.exists()
        val name = mockFile.name
        val path = mockFile.path
        
        // Then
        assertTrue(exists)
        assertEquals("test.txt", name)
        assertEquals("/test/test.txt", path)
        verify { mockFile.exists() }
        verify { mockFile.name }
    }
    
    @Test
    fun `test TestUtils createTempDir`() {
        // Given & When
        val tempDir = TestUtils.createTempDir("test")
        
        // Then
        assertTrue(tempDir.exists())
        assertTrue(tempDir.isDirectory)
        
        // Cleanup
        tempDir.deleteRecursively()
    }
    
    @Test
    fun `test TestUtils createTempFile`() {
        // Given & When
        val tempFile = TestUtils.createTempFile("test", ".txt")
        
        // Then
        assertTrue(tempFile.exists())
        assertTrue(tempFile.isFile)
        assertTrue(tempFile.name.startsWith("test"))
        assertTrue(tempFile.name.endsWith(".txt"))
        
        // Cleanup
        tempFile.delete()
    }
    
    @Test
    fun `test TestUtils createTestJson`() {
        // Given & When
        val json = TestUtils.createTestJson(
            "name" to "Test",
            "age" to 25,
            "active" to true,
            "score" to 98.5
        )
        
        // Then
        assertTrue(json.contains("\"name\":\"Test\""))
        assertTrue(json.contains("\"age\":25"))
        assertTrue(json.contains("\"active\":true"))
        assertTrue(json.contains("\"score\":98.5"))
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
    }
    
    @Test
    fun `test MockHelper createMockFile`() {
        // Given & When
        val mockFile = MockHelper.createMockFile(
            path = "/test/mock.txt",
            exists = true,
            content = "mock content"
        )
        
        // Then
        assertTrue(mockFile.exists())
        assertEquals("mock.txt", mockFile.name)
        assertEquals("/test/mock.txt", mockFile.path)
        assertEquals("mock content", mockFile.readText())
        assertTrue(mockFile.isFile)
        assertFalse(mockFile.isDirectory)
    }
    
    @Test
    fun `test MockHelper createMockDirectory`() {
        // Given & When
        val mockDir = MockHelper.createMockDirectory(
            path = "/test/mockdir",
            exists = true
        )
        
        // Then
        assertTrue(mockDir.exists())
        assertEquals("mockdir", mockDir.name)
        assertTrue(mockDir.isDirectory)
        assertFalse(mockDir.isFile)
    }
    
    @Test
    fun `test MockHelper createTestConfig`() {
        // Given & When
        val config = MockHelper.createTestConfig()
        
        // Then
        assertEquals("true", config["enableModule"])
        assertEquals("test_user_123", config["userId"])
        assertEquals("60", config["collectInterval"])
        assertEquals("true", config["enableForest"])
        assertEquals("false", config["enableFarm"])
        assertEquals(5, config.size)
    }
    
    @Test
    fun `test MockHelper createTestUser`() {
        // Given & When
        val user = MockHelper.createTestUser("user_456", "测试员")
        
        // Then
        assertEquals("user_456", user["userId"])
        assertEquals("测试员", user["userName"])
        assertEquals(1000, user["energy"])
        assertEquals(true, user["isValid"])
    }
}
