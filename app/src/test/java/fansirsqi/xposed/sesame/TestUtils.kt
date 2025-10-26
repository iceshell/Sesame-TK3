package fansirsqi.xposed.sesame

import java.io.File

/**
 * 测试工具类
 * 提供测试中常用的辅助方法
 */
object TestUtils {
    
    /**
     * 创建临时测试目录
     */
    fun createTempDir(prefix: String = "test"): File {
        return kotlin.io.path.createTempDirectory(prefix).toFile().apply {
            deleteOnExit()
        }
    }
    
    /**
     * 创建临时测试文件
     */
    fun createTempFile(prefix: String = "test", suffix: String = ".tmp"): File {
        return kotlin.io.path.createTempFile(prefix, suffix).toFile().apply {
            deleteOnExit()
        }
    }
    
    /**
     * 创建测试用JSON字符串
     */
    fun createTestJson(vararg pairs: Pair<String, Any>): String {
        val entries = pairs.joinToString(",") { (key, value) ->
            val valueStr = when (value) {
                is String -> "\"$value\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"$value\""
            }
            "\"$key\":$valueStr"
        }
        return "{$entries}"
    }
    
    /**
     * 延迟执行（用于测试异步代码）
     */
    fun delay(millis: Long) {
        Thread.sleep(millis)
    }
    
    /**
     * 断言两个浮点数相等（带容差）
     */
    fun assertFloatEquals(expected: Float, actual: Float, delta: Float = 0.001f) {
        val diff = kotlin.math.abs(expected - actual)
        if (diff > delta) {
            throw AssertionError("Expected $expected but was $actual (diff: $diff)")
        }
    }
}
