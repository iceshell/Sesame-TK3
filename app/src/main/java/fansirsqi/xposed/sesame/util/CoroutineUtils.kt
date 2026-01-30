package fansirsqi.xposed.sesame.util

import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport

/**
 * 协程工具类
 * 
 * 提供协程相关的通用功能，用于替代传统的线程操作
 */
object CoroutineUtils {
    
    /**
     * 协程安全的延迟方法
     * 
     * 在协程环境中使用 delay()，在非协程环境中降级到 Thread.sleep()
     * 
     * @param millis 延迟毫秒数
     */
    @JvmStatic
    suspend fun delayCompat(millis: Long) {
        try {
            kotlinx.coroutines.delay(millis)
        } catch (e: Exception) {
            Log.printStackTrace("协程延迟异常", e)
        }
    }
    
    /**
     * 兼容性延迟方法（同步版本）
     * 
     * 在当前线程中执行延迟，自动处理协程和非协程环境
     * 
     * @param millis 延迟毫秒数
     */
    @JvmStatic
    fun sleepCompat(millis: Long) {
        try {
            LockSupport.parkNanos(millis * 1_000_000L)
            if (Thread.interrupted()) {
                Log.runtime("CoroutineUtils", "延迟被中断")
            }
        } catch (t: Throwable) {
            Log.printStackTrace("CoroutineUtils", t)
        }
    }
    
    /**
     * 在指定调度器上运行协程
     */
    @JvmStatic
    fun runOnDispatcher(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return CoroutineScope(dispatcher + SupervisorJob()).launch {
            try {
                block()
            } catch (e: Exception) {
                Log.printStackTrace("协程执行异常", e)
            }
        }
    }
    
    /**
     * 在IO调度器上运行协程
     */
    @JvmStatic
    fun runOnIO(block: suspend CoroutineScope.() -> Unit): Job {
        return runOnDispatcher(Dispatchers.IO, block)
    }
    
    /**
     * 在计算调度器上运行协程
     */
    @JvmStatic
    fun runOnComputation(block: suspend CoroutineScope.() -> Unit): Job {
        return runOnDispatcher(Dispatchers.Default, block)
    }
    
    /**
     * 同步执行协程代码块
     * 
     * 警告：此方法会阻塞当前线程，仅在必要时使用
     */
    @JvmStatic
    fun <T> runBlockingSafe(
        timeout: Long = 30000, // 30秒默认超时
        block: suspend CoroutineScope.() -> T
    ): T? {
        val resultRef = AtomicReference<T?>(null)
        val errorRef = AtomicReference<Throwable?>(null)
        val latch = CountDownLatch(1)

        val job = GlobalThreadPools.execute {
            val result = runCatching {
                withTimeout(timeout) {
                    block()
                }
            }
            resultRef.set(result.getOrNull())
            errorRef.set(result.exceptionOrNull())
            latch.countDown()
        }

        return try {
            val finished = latch.await(timeout, TimeUnit.MILLISECONDS)
            if (!finished) {
                job.cancel()
                Log.error("CoroutineUtils", "协程执行超时: ${timeout}ms")
                return null
            }

            val error = errorRef.get()
            when (error) {
                is TimeoutCancellationException -> {
                    Log.error("CoroutineUtils", "协程执行超时: ${timeout}ms")
                    null
                }
                null -> resultRef.get()
                else -> {
                    Log.printStackTrace("协程同步执行异常", error)
                    null
                }
            }
        } catch (e: InterruptedException) {
            job.cancel()
            Log.printStackTrace("协程同步执行异常", e)
            null
        }
    }
}
