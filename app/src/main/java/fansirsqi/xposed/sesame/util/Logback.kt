package fansirsqi.xposed.sesame.util

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import android.util.Log as AndroidLog
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Logback日志配置工具
 * 配置日志文件的滚动策略和输出格式
 */
object Logback {
    
    private var LOG_DIR: String? = null

    private const val MAX_FILE_SIZE = "10MB"
    private const val TOTAL_SIZE_CAP_CRITICAL = "50MB"
    private const val TOTAL_SIZE_CAP_NORMAL = "30MB"
    private const val TOTAL_SIZE_CAP_VERBOSE = "20MB"
    
    @JvmField
    val logNames = listOf(
        "runtime", "system", "record", "debug", "forest",
        "farm", "other", "error", "capture"
    )
    
    /**
     * 直接配置Logback
     */
    @JvmStatic
    fun configureLogbackDirectly() {
        // 延迟初始化 LOG_DIR
        if (LOG_DIR == null) {
            val logDir = Files.LOG_DIR ?: run {
                AndroidLog.e("Logback", "LOG_DIR未初始化")
                return
            }
            LOG_DIR = logDir.path + File.separator
        }
        
        File(LOG_DIR + "bak").mkdirs()
        
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        lc.stop()
        
        for (logName in logNames) {
            setupAppender(lc, logName)
        }
        
        val ple = PatternLayoutEncoder().apply {
            context = lc
            pattern = "[%thread] %logger{80} %msg%n"
            start()
        }
        
        val la = LogcatAppender().apply {
            context = lc
            encoder = ple
            start()
        }
        
        val root = LoggerFactory.getLogger("ROOT") as ch.qos.logback.classic.Logger
        root.addAppender(la)
    }
    
    /**
     * 设置日志Appender（异步）
     *
     * @param loggerContext Logger上下文
     * @param logName 日志名称
     */
    @JvmStatic
    fun setupAppender(loggerContext: LoggerContext, logName: String) {
        // 1. 创建RollingFileAppender
        val rfa = RollingFileAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = "${logName}File"
            file = LOG_DIR + logName + ".log"
        }
        
        // 2. 配置滚动策略
        val totalSizeCap = when (logName) {
            "runtime", "system", "record", "error" -> TOTAL_SIZE_CAP_CRITICAL
            "debug", "capture" -> TOTAL_SIZE_CAP_VERBOSE
            else -> TOTAL_SIZE_CAP_NORMAL
        }
        val satbrp = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().apply {
            context = loggerContext
            fileNamePattern = LOG_DIR + "bak/" + logName + "-%d{yyyy-MM-dd}.%i.log"
            setMaxFileSize(FileSize.valueOf(MAX_FILE_SIZE))
            setTotalSizeCap(FileSize.valueOf(totalSizeCap))
            maxHistory = 7
            isCleanHistoryOnStart = true
            setParent(rfa)
            start()
        }
        
        rfa.rollingPolicy = satbrp
        
        // 3. 配置编码器
        val ple = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = "%d{dd日 HH:mm:ss.SS} %msg%n"
            start()
        }
        
        rfa.encoder = ple
        rfa.start()
        
        // 4. 使用AsyncAppender包装（异步写入）
        val asyncAppender = AsyncAppender().apply {
            context = loggerContext
            name = logName
            queueSize = 512  // 队列大小
            discardingThreshold = 0  // 不丢弃日志
            addAppender(rfa)
            start()
        }
        
        // 5. 将AsyncAppender添加到Logger
        val logger = LoggerFactory.getLogger(logName) as ch.qos.logback.classic.Logger
        logger.addAppender(asyncAppender)
    }
}
