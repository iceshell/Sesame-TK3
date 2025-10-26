package fansirsqi.xposed.sesame.util

import java.time.Duration
import java.time.Instant

/**
 * 时间计数器
 * 用于统计和记录操作耗时
 */
class TimeCounter(private val name: String) : AutoCloseable {
    
    private val start: Instant = Instant.now()
    private var lastCheckpoint: Instant = start
    private var stopped = false
    private var unexceptCnt = 0
    private val resultMsg = StringBuilder()
    
    /**
     * 关闭计数器（类似C++析构）
     */
    override fun close() {
        if (stopped) {
            return
        }
        if (unexceptCnt > 0) {
            stop()
        }
    }
    
    /**
     * 停止计数并输出总耗时
     */
    fun stop() {
        val end = Instant.now()
        val durationMs = Duration.between(start, end).toMillis()
        Log.record(
            name,
            "========================\n$name 耗时: $durationMs ms ($resultMsg)"
        )
        stopped = true
    }
    
    /**
     * 记录调试检查点
     *
     * @param msg 检查点消息
     */
    fun countDebug(msg: String) {
        val now = Instant.now()
        val durationMs = Duration.between(lastCheckpoint, now).toMillis()
        Log.record(name, "========================\n$msg 耗时: $durationMs ms")
        lastCheckpoint = now
    }
    
    /**
     * 记录检查点
     *
     * @param msg 检查点消息
     */
    fun count(msg: String) {
        val now = Instant.now()
        val durationMs = Duration.between(lastCheckpoint, now).toMillis()
        resultMsg.append(msg).append(":").append(durationMs).append(" ms, ")
        lastCheckpoint = now
    }
    
    /**
     * 记录超出预期时间的检查点
     *
     * @param msg 检查点消息
     * @param exceptMs 预期时间（毫秒）
     */
    fun countUnexcept(msg: String, exceptMs: Long) {
        val now = Instant.now()
        val durationMs = Duration.between(lastCheckpoint, now).toMillis()
        if (durationMs > exceptMs) {
            resultMsg.append(msg).append(":")
                .append(durationMs).append(" ms(except:")
                .append(exceptMs).append("ms), ")
            unexceptCnt++
        }
        lastCheckpoint = now
    }
}
