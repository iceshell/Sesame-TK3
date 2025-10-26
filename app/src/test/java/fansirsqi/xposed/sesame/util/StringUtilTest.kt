package fansirsqi.xposed.sesame.util

import org.junit.Assert.*
import org.junit.Test

/**
 * StringUtil 测试类
 * 
 * 测试重构后的 Kotlin 版本是否保持与 Java 版本相同的行为
 * 
 * @author Sesame-TK Team
 * @since 重构阶段1
 */
class StringUtilTest {
    
    // ==================== isEmpty 测试 ====================
    
    @Test
    fun `isEmpty should return true for null string`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.isEmpty(null)
        assertTrue("null string should be empty", result)
    }
    
    @Test
    fun `isEmpty should return true for empty string`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.isEmpty("")
        assertTrue("empty string should be empty", result)
    }
    
    @Test
    fun `isEmpty should return false for non-empty string`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.isEmpty("test")
        assertFalse("non-empty string should not be empty", result)
    }
    
    @Test
    fun `isEmpty should return false for whitespace string`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.isEmpty(" ")
        assertFalse("whitespace string is not empty (matches Java behavior)", result)
    }
    
    // ==================== Kotlin 扩展函数测试 ====================
    
    @Test
    fun `Kotlin isNullOrEmpty extension works correctly`() {
        val nullString: String? = null
        val emptyString = ""
        val nonEmptyString = "test"
        
        assertTrue(nullString.isNullOrEmpty())
        assertTrue(emptyString.isNullOrEmpty())
        assertFalse(nonEmptyString.isNullOrEmpty())
    }
    
    // ==================== collectionJoinString 测试 ====================
    
    @Test
    fun `collectionJoinString should join collection elements`() {
        val collection = listOf("a", "b", "c")
        @Suppress("DEPRECATION")
        val result = StringUtil.collectionJoinString(", ", collection)
        assertEquals("a, b, c", result)
    }
    
    @Test
    fun `collectionJoinString should handle empty collection`() {
        val collection = emptyList<String>()
        @Suppress("DEPRECATION")
        val result = StringUtil.collectionJoinString(", ", collection)
        assertEquals("", result)
    }
    
    @Test
    fun `collectionJoinString should handle single element`() {
        val collection = listOf("only")
        @Suppress("DEPRECATION")
        val result = StringUtil.collectionJoinString(", ", collection)
        assertEquals("only", result)
    }
    
    @Test
    fun `Kotlin joinToString extension works correctly`() {
        val list = listOf("a", "b", "c")
        val result = list.joinToString(", ")
        assertEquals("a, b, c", result)
    }
    
    // ==================== arrayJoinString 测试 ====================
    
    @Test
    fun `arrayJoinString should join array elements`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.arrayJoinString(", ", "a", "b", "c")
        assertEquals("a, b, c", result)
    }
    
    @Test
    fun `arrayJoinString should handle empty array`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.arrayJoinString(", ")
        assertEquals("", result)
    }
    
    @Test
    fun `arrayJoinString should handle null elements`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.arrayJoinString(", ", "a", null, "c")
        assertEquals("a, null, c", result)
    }
    
    // ==================== arrayToString 测试 ====================
    
    @Test
    fun `arrayToString should use comma separator`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.arrayToString("x", "y", "z")
        assertEquals("x,y,z", result)
    }
    
    // ==================== padLeft 测试 ====================
    
    @Test
    fun `padLeft should pad integer with zeros`() {
        val result = StringUtil.padLeft(5, 3, '0')
        assertEquals("005", result)
    }
    
    @Test
    fun `padLeft should not truncate if already longer`() {
        val result = StringUtil.padLeft(12345, 3, '0')
        assertEquals("12345", result)
    }
    
    @Test
    fun `padLeft should pad string with spaces`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.padLeft("abc", 5, ' ')
        assertEquals("  abc", result)
    }
    
    @Test
    fun `Kotlin padStart extension works correctly`() {
        val result = "5".padStart(3, '0')
        assertEquals("005", result)
    }
    
    @Test
    fun `Int padStartWith extension works correctly`() {
        val result = 5.padStartWith(3, '0')
        assertEquals("005", result)
    }
    
    // ==================== padRight 测试 ====================
    
    @Test
    fun `padRight should pad integer with zeros`() {
        val result = StringUtil.padRight(5, 3, '0')
        assertEquals("500", result)
    }
    
    @Test
    fun `padRight should pad string with spaces`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.padRight("abc", 5, ' ')
        assertEquals("abc  ", result)
    }
    
    @Test
    fun `Kotlin padEnd extension works correctly`() {
        val result = "abc".padEnd(5, ' ')
        assertEquals("abc  ", result)
    }
    
    @Test
    fun `Int padEndWith extension works correctly`() {
        val result = 5.padEndWith(3, '0')
        assertEquals("500", result)
    }
    
    // ==================== getSubString 测试 ====================
    
    @Test
    fun `getSubString should extract text between markers`() {
        val text = "Hello [World] Test"
        val result = StringUtil.getSubString(text, "[", "]")
        assertEquals("World", result)
    }
    
    @Test
    fun `getSubString should handle null left marker`() {
        val text = "Hello World"
        val result = StringUtil.getSubString(text, null, " ")
        assertEquals("Hello", result)
    }
    
    @Test
    fun `getSubString should handle null right marker`() {
        val text = "Hello World"
        val result = StringUtil.getSubString(text, "Hello ", null)
        assertEquals("World", result)
    }
    
    @Test
    fun `getSubString should handle empty markers`() {
        val text = "Hello World"
        val result = StringUtil.getSubString(text, "", "")
        assertEquals("Hello World", result)
    }
    
    @Test
    fun `getSubString should handle marker not found`() {
        val text = "Hello World"
        val result = StringUtil.getSubString(text, "X", "Y")
        assertEquals("Hello World", result)
    }
    
    @Test
    fun `Kotlin substringBetween extension works correctly`() {
        val text = "Hello [World] Test"
        val result = text.substringBetween("[", "]")
        assertEquals("World", result)
    }
    
    // ==================== 边界情况测试 ====================
    
    @Test
    fun `should handle Chinese characters correctly`() {
        @Suppress("DEPRECATION")
        val result = StringUtil.collectionJoinString("、", listOf("苹果", "香蕉", "橙子"))
        assertEquals("苹果、香蕉、橙子", result)
    }
    
    @Test
    fun `should handle special characters correctly`() {
        val text = "abc<tag>content</tag>xyz"
        val result = StringUtil.getSubString(text, "<tag>", "</tag>")
        assertEquals("content", result)
    }
    
    @Test
    fun `should handle numbers correctly`() {
        val result = 123.padStartWith(6, '0')
        assertEquals("000123", result)
    }
    
    // ==================== 性能测试（可选）====================
    
    @Test
    fun `large collection joinToString performance`() {
        val largeList = (1..1000).map { "item$it" }
        val result = largeList.joinToString(", ")
        assertTrue("Should join 1000 items", result.contains("item1") && result.contains("item1000"))
    }
}
