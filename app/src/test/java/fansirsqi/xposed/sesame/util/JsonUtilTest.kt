package fansirsqi.xposed.sesame.util

import fansirsqi.xposed.sesame.TestUtils
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JsonUtilTest {

    @Test
    fun `parseJSONObjectOrNull should return null for null`() {
        val result = JsonUtil.parseJSONObjectOrNull(null)
        assertNull(result)
    }

    @Test
    fun `parseJSONObjectOrNull should return null for blank string`() {
        val result = JsonUtil.parseJSONObjectOrNull("")
        assertNull(result)
    }

    @Test
    fun `parseJSONObjectOrNull should return null for whitespace string`() {
        val result = JsonUtil.parseJSONObjectOrNull("   ")
        assertNull(result)
    }

    @Test
    fun `parseJSONObjectOrNull should return null for invalid json`() {
        val result = JsonUtil.parseJSONObjectOrNull("not-a-json")
        assertNull(result)
    }

    @Test
    fun `parseJSONObjectOrNull should parse valid json`() {
        val json = TestUtils.createTestJson(
            "success" to true,
            "count" to 3,
            "title" to "hello"
        )

        val result = JsonUtil.parseJSONObjectOrNull(json)
        assertNotNull(result)

        val obj: JSONObject = result!!
        assertEquals(true, obj.optBoolean("success"))
        assertEquals(3, obj.optInt("count"))
        assertEquals("hello", obj.optString("title"))
    }
}
