package fansirsqi.xposed.sesame.util

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Logback日志配置工具
 * 配置日志文件的滚动策略和输出格式
 */
object Logback {
    
    private var LOG_DIR: String? = null
    
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
            assert(Files.LOG_DIR != null)
            LOG_DIR = Files.LOG_DIR!!.path + File.separator
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
     * 设置日志Appender
     *
     * @param loggerContext Logger上下文
     * @param logName 日志名称
     */
    @JvmStatic
    fun setupAppender(loggerContext: LoggerContext, logName: String) {
        val rfa = RollingFileAppender<ILoggingEvent>().apply {
            context = loggerContext
            name = logName
            file = LOG_DIR + logName + ".log"
        }
        
        val satbrp = SizeAndTimeBasedRollingPolicy<ILoggingEvent>().apply {
            context = loggerContext
            fileNamePattern = LOG_DIR + "bak/" + logName + "-%d{yyyy-MM-dd}.%i.log"
            setMaxFileSize(FileSize.valueOf("50MB"))
            setTotalSizeCap(FileSize.valueOf("100MB"))
            maxHistory = 7
            isCleanHistoryOnStart = true
            setParent(rfa)
            start()
        }
        
        rfa.rollingPolicy = satbrp
        
        val ple = PatternLayoutEncoder().apply {
            context = loggerContext
            pattern = "%d{dd日 HH:mm:ss.SS} %msg%n"
            start()
        }
        
        rfa.encoder = ple
        rfa.start()
        
        val logger = LoggerFactory.getLogger(logName) as ch.qos.logback.classic.Logger
        logger.addAppender(rfa)
    }
}
