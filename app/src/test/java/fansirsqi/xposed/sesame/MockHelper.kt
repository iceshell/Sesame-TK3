package fansirsqi.xposed.sesame

import io.mockk.mockk
import io.mockk.every
import java.io.File

/**
 * Mock辅助类
 * 提供常用的Mock对象创建
 */
object MockHelper {
    
    /**
     * 创建Mock文件对象
     */
    fun createMockFile(
        path: String = "/test/file.txt",
        exists: Boolean = true,
        content: String = "test content"
    ): File {
        return mockk<File>(relaxed = true) {
            every { this@mockk.exists() } returns exists
            every { this@mockk.path } returns path
            every { this@mockk.name } returns File(path).name
            every { this@mockk.readText() } returns content
            every { this@mockk.isFile } returns true
            every { this@mockk.isDirectory } returns false
        }
    }
    
    /**
     * 创建Mock目录对象
     */
    fun createMockDirectory(
        path: String = "/test/dir",
        exists: Boolean = true
    ): File {
        return mockk<File>(relaxed = true) {
            every { this@mockk.exists() } returns exists
            every { this@mockk.path } returns path
            every { this@mockk.name } returns File(path).name
            every { this@mockk.isFile } returns false
            every { this@mockk.isDirectory } returns true
        }
    }
    
    /**
     * 创建测试用的配置数据
     */
    fun createTestConfig(): Map<String, String> {
        return mapOf(
            "enableModule" to "true",
            "userId" to "test_user_123",
            "collectInterval" to "60",
            "enableForest" to "true",
            "enableFarm" to "false"
        )
    }
    
    /**
     * 创建测试用的用户数据
     */
    fun createTestUser(
        userId: String = "test_user_123",
        userName: String = "测试用户"
    ): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "userName" to userName,
            "energy" to 1000,
            "isValid" to true
        )
    }
}
