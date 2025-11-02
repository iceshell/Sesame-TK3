package fansirsqi.xposed.sesame.util

import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * ErrorHandler 单元测试
 * 
 * 测试细粒度错误处理功能
 * 
 * 注意：需要Mock Log类，暂时禁用等待Robolectric配置
 */
@Ignore("需要Android环境支持 - 等待Robolectric配置")
class ErrorHandlerTest {

    @Before
    fun setup() {
        // Mock Log对象，避免Android依赖
        mockkObject(Log)
        every { Log.error(any(), any()) } just Runs
        every { Log.runtime(any(), any()) } just Runs
        every { Log.debug(any(), any()) } just Runs
        every { Log.printStackTrace(any(), any()) } just Runs
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    // ========== safely 函数测试 ==========

    @Test
    fun `safely should return result when no exception`() {
        val result = ErrorHandler.safely("TAG", "test", fallback = null) {
            "success"
        }
        assertEquals("success", result)
    }

    @Test
    fun `safely should return fallback when exception occurs`() {
        val result = ErrorHandler.safely("TAG", "test", fallback = "fallback") {
            throw RuntimeException("error")
        }
        assertEquals("fallback", result)
    }

    @Test
    fun `safely should call Log error when exception occurs`() {
        ErrorHandler.safely("TAG", "operation", fallback = null) {
            throw RuntimeException("test error")
        }
        verify { Log.error("TAG", "operation: test error") }
    }

    // ========== safelyRun 函数测试 ==========

    @Test
    fun `safelyRun should execute block when no exception`() {
        var executed = false
        ErrorHandler.safelyRun("TAG", "test") {
            executed = true
        }
        assertEquals(true, executed)
    }

    @Test
    fun `safelyRun should catch exception and log error`() {
        ErrorHandler.safelyRun("TAG", "operation") {
            throw RuntimeException("test error")
        }
        verify { Log.error("TAG", "operation: test error") }
    }

    // ========== safelyRpcCall 函数测试 ==========

    @Test
    fun `safelyRpcCall should return result when no exception`() {
        val result = ErrorHandler.safelyRpcCall("TAG", "RPC test") {
            "rpc success"
        }
        assertEquals("rpc success", result)
    }

    @Test
    fun `safelyRpcCall should handle RpcBusinessException`() {
        var businessErrorCalled = false
        val result = ErrorHandler.safelyRpcCall(
            tag = "TAG",
            operation = "RPC test",
            fallback = "fallback",
            onBusinessError = { businessErrorCalled = true }
        ) {
            throw ErrorHandler.RpcBusinessException("LIMIT_ERROR", "操作受限")
        }
        assertEquals("fallback", result)
        assertEquals(true, businessErrorCalled)
        verify { Log.runtime("TAG", match { it.contains("RPC业务错误") }) }
    }

    @Test
    fun `safelyRpcCall should handle SocketTimeoutException`() {
        var networkErrorCalled = false
        val result = ErrorHandler.safelyRpcCall(
            tag = "TAG",
            operation = "RPC test",
            fallback = "fallback",
            onNetworkError = { networkErrorCalled = true }
        ) {
            throw SocketTimeoutException("timeout")
        }
        assertEquals("fallback", result)
        assertEquals(true, networkErrorCalled)
        verify { Log.error("TAG", match { it.contains("网络超时") }) }
    }

    @Test
    fun `safelyRpcCall should handle UnknownHostException`() {
        var networkErrorCalled = false
        val result = ErrorHandler.safelyRpcCall(
            tag = "TAG",
            operation = "RPC test",
            fallback = "fallback",
            onNetworkError = { networkErrorCalled = true }
        ) {
            throw UnknownHostException("host not found")
        }
        assertEquals("fallback", result)
        assertEquals(true, networkErrorCalled)
        verify { Log.error("TAG", match { it.contains("网络不可达") }) }
    }

    @Test
    fun `safelyRpcCall should handle JSONException`() {
        val result = ErrorHandler.safelyRpcCall(
            tag = "TAG",
            operation = "RPC test",
            fallback = "fallback"
        ) {
            throw JSONException("invalid json")
        }
        assertEquals("fallback", result)
        verify { Log.error("TAG", match { it.contains("数据解析失败") }) }
    }

    @Test
    fun `safelyRpcCall should rethrow CancellationException`() {
        assertFailsWith<CancellationException> {
            ErrorHandler.safelyRpcCall("TAG", "RPC test") {
                throw CancellationException("cancelled")
            }
        }
    }

    // ========== safelyParseJson 函数测试 ==========

    @Test
    fun `safelyParseJson should return parsed result when valid`() {
        val result = ErrorHandler.safelyParseJson("TAG", "test json") {
            JSONObject("""{"key": "value"}""")
        }
        assertEquals("value", result?.getString("key"))
    }

    @Test
    fun `safelyParseJson should return fallback on JSONException`() {
        val result = ErrorHandler.safelyParseJson("TAG", "test json", fallback = null) {
            JSONObject("invalid json")
        }
        assertNull(result)
        verify { Log.error("TAG", match { it.contains("解析JSON失败") }) }
    }

    @Test
    fun `safelyParseJson should return fallback on NullPointerException`() {
        val result = ErrorHandler.safelyParseJson("TAG", "test json", fallback = null) {
            val obj: JSONObject? = null
            obj!!.getString("key")
        }
        assertNull(result)
        verify { Log.error("TAG", match { it.contains("JSON数据为空") }) }
    }

    // ========== safelyCoroutine 函数测试 ==========

    @Test
    fun `safelyCoroutine should return result when no exception`() = runTest {
        val result = ErrorHandler.safelyCoroutine("TAG", "coroutine test") {
            "coroutine success"
        }
        assertEquals("coroutine success", result)
    }

    @Test
    fun `safelyCoroutine should rethrow CancellationException`() = runTest {
        assertFailsWith<CancellationException> {
            ErrorHandler.safelyCoroutine("TAG", "coroutine test") {
                throw CancellationException("cancelled")
            }
        }
        verify { Log.debug("TAG", match { it.contains("协程被取消") }) }
    }

    @Test
    fun `safelyCoroutine should return fallback on other exceptions`() = runTest {
        val result = ErrorHandler.safelyCoroutine("TAG", "coroutine test", fallback = "fallback") {
            throw RuntimeException("error")
        }
        assertEquals("fallback", result)
        verify { Log.error("TAG", match { it.contains("异常") }) }
    }

    // ========== safelyFileIo 函数测试 ==========

    @Test
    fun `safelyFileIo should return result when no exception`() {
        val result = ErrorHandler.safelyFileIo("TAG", "/test/path", "读取") {
            "file content"
        }
        assertEquals("file content", result)
    }

    @Test
    fun `safelyFileIo should handle FileNotFoundException`() {
        val result = ErrorHandler.safelyFileIo("TAG", "/test/path", "读取", fallback = null) {
            throw java.io.FileNotFoundException("file not found")
        }
        assertNull(result)
        verify { Log.error("TAG", match { it.contains("文件不存在") }) }
    }

    @Test
    fun `safelyFileIo should handle IOException`() {
        val result = ErrorHandler.safelyFileIo("TAG", "/test/path", "读取", fallback = null) {
            throw IOException("io error")
        }
        assertNull(result)
        verify { Log.error("TAG", match { it.contains("IO错误") }) }
    }

    @Test
    fun `safelyFileIo should handle SecurityException`() {
        val result = ErrorHandler.safelyFileIo("TAG", "/test/path", "读取", fallback = null) {
            throw SecurityException("permission denied")
        }
        assertNull(result)
        verify { Log.error("TAG", match { it.contains("权限不足") }) }
    }

    // ========== safelyWithRetry 函数测试 ==========

    @Test
    fun `safelyWithRetry should return result on first success`() {
        var attempts = 0
        val result = ErrorHandler.safelyWithRetry(
            tag = "TAG",
            maxRetries = 3,
            retryDelay = 10
        ) {
            attempts++
            "success"
        }
        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `safelyWithRetry should retry on failure`() {
        var attempts = 0
        val result = ErrorHandler.safelyWithRetry(
            tag = "TAG",
            maxRetries = 3,
            retryDelay = 10,
            fallback = "fallback"
        ) {
            attempts++
            if (attempts < 2) {
                throw RuntimeException("retry error")
            }
            "success on retry"
        }
        assertEquals("success on retry", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `safelyWithRetry should return fallback after max retries`() {
        var attempts = 0
        val result = ErrorHandler.safelyWithRetry(
            tag = "TAG",
            maxRetries = 3,
            retryDelay = 10,
            fallback = "fallback"
        ) {
            attempts++
            throw RuntimeException("always fail")
        }
        assertEquals("fallback", result)
        assertEquals(3, attempts)
    }

    // ========== require 和 check 函数测试 ==========

    @Test
    fun `require should not throw when condition is true`() {
        ErrorHandler.require(true) { "error message" }
        // No exception thrown = success
    }

    @Test
    fun `require should throw IllegalArgumentException when condition is false`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ErrorHandler.require(false) { "custom error message" }
        }
        assertEquals("custom error message", exception.message)
    }

    @Test
    fun `check should not throw when condition is true`() {
        ErrorHandler.check(true) { "error message" }
        // No exception thrown = success
    }

    @Test
    fun `check should throw IllegalStateException when condition is false`() {
        val exception = assertFailsWith<IllegalStateException> {
            ErrorHandler.check(false) { "custom error message" }
        }
        assertEquals("custom error message", exception.message)
    }
}
