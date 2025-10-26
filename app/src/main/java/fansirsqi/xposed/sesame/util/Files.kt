package fansirsqi.xposed.sesame.util

import android.annotation.SuppressLint
import android.os.Environment
import fansirsqi.xposed.sesame.data.General
import java.io.*
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文件工具类，提供文件和目录操作方法
 *
 * **迁移说明**:
 * - 使用Kotlin的扩展函数和高阶函数
 * - 保持所有方法的Java兼容性 (@JvmStatic/@JvmField)
 * - 优化异常处理和空安全
 */
object Files {

    private const val TAG = "Files"

    /**
     * 配置文件夹名称
     */
    const val CONFIG_DIR_NAME = "sesame-TK"

    /**
     * 应用配置文件夹主路径
     */
    @JvmField
    val MAIN_DIR: File = getMainDir()

    /**
     * 配置文件夹路径
     */
    @JvmField
    val CONFIG_DIR: File = getConfigDir()

    /**
     * 日志文件夹路径
     */
    @JvmField
    val LOG_DIR: File? = getLogDir()

    // ==================== 目录管理 ====================

    /**
     * 确保指定的目录存在且不是一个文件
     */
    @JvmStatic
    fun ensureDir(directory: File?) {
        try {
            if (directory == null) {
                Log.error(TAG, "Directory cannot be null")
                return
            }
            when {
                !directory.exists() -> {
                    if (!directory.mkdirs()) {
                        Log.error(TAG, "Failed to create directory: ${directory.absolutePath}")
                    }
                }
                directory.isFile -> {
                    if (!directory.delete() || !directory.mkdirs()) {
                        Log.error(TAG, "Failed to replace file with directory: ${directory.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace("$TAG ensureDir error", e)
        }
    }

    private fun getMainDir(): File {
        val storageDirStr = "${Environment.getExternalStorageDirectory()}${File.separator}Android${File.separator}media${File.separator}${General.PACKAGE_NAME}"
        val storageDir = File(storageDirStr)
        val mainDir = File(storageDir, CONFIG_DIR_NAME)
        ensureDir(mainDir)
        return mainDir
    }

    private fun getLogDir(): File? {
        val logDir = File(MAIN_DIR, "log")
        ensureDir(logDir)
        return if (logDir.exists()) logDir else null
    }

    private fun getConfigDir(): File {
        val configDir = File(MAIN_DIR, "config")
        ensureDir(configDir)
        return configDir
    }

    @JvmStatic
    fun getUserConfigDir(userId: String): File {
        val configDir = File(CONFIG_DIR, userId)
        ensureDir(configDir)
        return configDir
    }

    // ==================== 配置文件管理 ====================

    @JvmStatic
    fun getDefaultConfigV2File(): File {
        return File(CONFIG_DIR, "config_v2.json")
    }

    @JvmStatic
    @Synchronized
    fun setDefaultConfigV2File(json: String): Boolean {
        return write2File(json, File(CONFIG_DIR, "config_v2.json"))
    }

    @JvmStatic
    @Synchronized
    fun getConfigV2File(userId: String): File {
        val confV2File = File(CONFIG_DIR, "${userId}${File.separator}config_v2.json")
        
        // 如果新配置文件不存在，尝试从旧配置文件迁移
        if (!confV2File.exists()) {
            val oldFile = File(CONFIG_DIR, "config_v2-${userId}.json")
            if (oldFile.exists()) {
                val content = readFromFile(oldFile)
                if (write2File(content, confV2File)) {
                    if (!oldFile.delete()) {
                        Log.error(TAG, "Failed to delete old config file: ${oldFile.absolutePath}")
                    }
                } else {
                    Log.error(TAG, "Failed to migrate config file for user: $userId")
                    return oldFile
                }
            }
        }
        return confV2File
    }

    @JvmStatic
    @Synchronized
    fun setConfigV2File(userId: String, json: String): Boolean {
        return write2File(json, File(CONFIG_DIR, "${userId}${File.separator}config_v2.json"))
    }

    // ==================== 用户文件管理 ====================

    @JvmStatic
    @Synchronized
    fun getTargetFileofUser(userId: String?, fullTargetFileName: String): File? {
        if (userId.isNullOrEmpty()) {
            Log.error(TAG, "Invalid userId for target file: $fullTargetFileName")
            return null
        }

        val userDir = File(CONFIG_DIR, userId)
        ensureDir(userDir)

        val targetFile = File(userDir, fullTargetFileName)
        if (!targetFile.exists()) {
            try {
                if (targetFile.createNewFile()) {
                    Log.runtime(TAG, "${targetFile.name} created successfully")
                } else {
                    Log.runtime(TAG, "${targetFile.name} creation failed")
                }
            } catch (e: IOException) {
                Log.error(TAG, "Failed to create file: ${targetFile.name}")
                Log.printStackTrace(TAG, e)
            }
        } else {
            val canRead = targetFile.canRead()
            val canWrite = targetFile.canWrite()
            Log.system(TAG, "$fullTargetFileName permissions: r=$canRead; w=$canWrite")

            if (!canWrite) {
                if (targetFile.setWritable(true)) {
                    Log.runtime(TAG, "${targetFile.name} write permission set successfully")
                } else {
                    Log.runtime(TAG, "${targetFile.name} write permission set failed")
                }
            }
        }

        return targetFile
    }

    @JvmStatic
    @Synchronized
    fun getTargetFileofDir(dir: File, fullTargetFileName: String): File {
        ensureDir(dir)

        val targetFile = File(dir, fullTargetFileName)

        if (!targetFile.exists()) {
            try {
                if (targetFile.createNewFile()) {
                    Log.runtime(TAG, "File created successfully: ${targetFile.absolutePath}")
                } else {
                    Log.runtime(TAG, "File creation failed: ${targetFile.absolutePath}")
                }
            } catch (e: IOException) {
                Log.error(TAG, "Failed to create file: ${targetFile.absolutePath}")
                Log.printStackTrace(TAG, e)
            }
        } else {
            val canRead = targetFile.canRead()
            val canWrite = targetFile.canWrite()
            Log.system(TAG, "File permissions for ${targetFile.absolutePath}: r=$canRead; w=$canWrite")

            if (!canWrite) {
                if (targetFile.setWritable(true)) {
                    Log.runtime(TAG, "Write permission set successfully for file: ${targetFile.absolutePath}")
                } else {
                    Log.runtime(TAG, "Write permission set failed for file: ${targetFile.absolutePath}")
                }
            }
        }

        return targetFile
    }

    @JvmStatic
    @Synchronized
    fun setTargetFileofDir(content: String, targetFileName: File): Boolean {
        return write2File(content, targetFileName)
    }

    // ==================== 特定文件获取 ====================

    @JvmStatic
    fun getSelfIdFile(userId: String): File? = getTargetFileofUser(userId, "self.json")

    @JvmStatic
    fun getFriendIdMapFile(userId: String): File? = getTargetFileofUser(userId, "friend.json")

    @JvmStatic
    fun runtimeInfoFile(userId: String): File? = getTargetFileofUser(userId, "runtime.json")

    @JvmStatic
    fun getStatusFile(userId: String): File? = getTargetFileofUser(userId, "status.json")

    @JvmStatic
    fun getStatisticsFile(): File = getTargetFileofDir(MAIN_DIR, "statistics.json")

    @JvmStatic
    fun getExportedStatisticsFile(): File? {
        return try {
            val storageDirStr = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}${File.separator}$CONFIG_DIR_NAME"
            val storageDir = File(storageDirStr)
            if (!storageDir.exists()) {
                if (storageDir.mkdirs()) {
                    Log.system(TAG, "create downloads's $CONFIG_DIR_NAME directory success")
                } else {
                    Log.error(TAG, "create downloads's $CONFIG_DIR_NAME directory failed")
                }
            }
            getTargetFileofDir(storageDir, "statistics.json")
        } catch (e: Exception) {
            Log.printStackTrace("${TAG}export statistics file error", e)
            null
        }
    }

    @JvmStatic
    fun getFriendWatchFile(userId: String): File? = getTargetFileofUser(userId, "friendWatch.json")

    @JvmStatic
    fun getWuaFile(): File = getTargetFileofDir(MAIN_DIR, "wua.list")

    @JvmStatic
    fun getCityCodeFile(): File {
        val cityCodeFile = File(MAIN_DIR, "cityCode.json")
        if (cityCodeFile.exists() && cityCodeFile.isDirectory) {
            if (!cityCodeFile.delete()) {
                Log.error(TAG, "Failed to delete directory: ${cityCodeFile.absolutePath}")
            }
        }
        return cityCodeFile
    }

    // ==================== 文件导出 ====================

    @JvmStatic
    fun exportFile(file: File, hasTime: Boolean): File? {
        val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), CONFIG_DIR_NAME)
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            Log.error(TAG, "Failed to create export directory: ${exportDir.absolutePath}")
            return null
        }

        val fileNameWithoutExtension = file.name.substring(0, file.name.lastIndexOf('.'))
        val fileExtension = file.name.substring(file.name.lastIndexOf('.'))
        
        val newFileName = if (hasTime) {
            @SuppressLint("SimpleDateFormat")
            val dateTimeString = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(Date())
            "${fileNameWithoutExtension}_$dateTimeString$fileExtension"
        } else {
            "$fileNameWithoutExtension$fileExtension"
        }

        val exportFile = File(exportDir, newFileName)
        if (exportFile.exists() && exportFile.isDirectory) {
            if (!exportFile.delete()) {
                Log.error(TAG, "Failed to delete existing directory: ${exportFile.absolutePath}")
                return null
            }
        }

        if (!copy(file, exportFile)) {
            Log.error(TAG, "Failed to copy file: ${file.absolutePath} to ${exportFile.absolutePath}")
            return null
        }

        return exportFile
    }

    // ==================== 日志文件管理 ====================

    private fun ensureLogFile(logFileName: String): File {
        val logFile = File(LOG_DIR, logFileName)
        if (logFile.exists() && logFile.isDirectory) {
            if (logFile.delete()) {
                Log.system(TAG, "日志${logFile.name}目录存在，删除成功！")
            } else {
                Log.error(TAG, "日志${logFile.name}目录存在，删除失败！")
            }
        }
        if (!logFile.exists()) {
            try {
                if (logFile.createNewFile()) {
                    Log.system(TAG, "日志${logFile.name}文件不存在，创建成功！")
                } else {
                    Log.error(TAG, "日志${logFile.name}文件不存在，创建失败！")
                }
            } catch (ignored: IOException) {
                // 忽略创建文件时可能出现的异常
            }
        }
        return logFile
    }

    @JvmStatic
    fun getLogFile(logName: String): String = "$logName.log"

    @JvmStatic
    fun getRuntimeLogFile(): File = ensureLogFile(getLogFile("runtime"))

    @JvmStatic
    fun getRecordLogFile(): File = ensureLogFile(getLogFile("record"))

    @JvmStatic
    fun getDebugLogFile(): File = ensureLogFile(getLogFile("debug"))

    @JvmStatic
    fun getCaptureLogFile(): File = ensureLogFile(getLogFile("capture"))

    @JvmStatic
    fun getForestLogFile(): File = ensureLogFile(getLogFile("forest"))

    @JvmStatic
    fun getFarmLogFile(): File = ensureLogFile(getLogFile("farm"))

    @JvmStatic
    fun getOtherLogFile(): File = ensureLogFile(getLogFile("other"))

    @JvmStatic
    fun getErrorLogFile(): File = ensureLogFile(getLogFile("error"))

    // ==================== 文件读写操作 ====================

    @JvmStatic
    fun close(c: Closeable?) {
        c?.let {
            try {
                it.close()
            } catch (e: IOException) {
                Log.printStackTrace(TAG, e)
            }
        }
    }

    @JvmStatic
    fun readFromFile(f: File): String {
        if (!f.exists()) return ""
        if (!f.canRead()) {
            ToastUtil.showToast("${f.name}没有读取权限！")
            return ""
        }

        val result = StringBuilder()
        var fr: FileReader? = null
        try {
            fr = FileReader(f)
            val chs = CharArray(1024)
            var len: Int
            while (fr.read(chs).also { len = it } >= 0) {
                result.append(chs, 0, len)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        } finally {
            close(fr)
        }
        return result.toString()
    }

    @JvmStatic
    fun beforWrite(f: File): Boolean {
        if (f.exists()) {
            if (!f.canWrite()) {
                ToastUtil.showToast("${f.absoluteFile}没有写入权限！")
                return true
            }
            if (f.isDirectory) {
                if (!f.delete()) {
                    ToastUtil.showToast("${f.absoluteFile}无法删除目录！")
                    return true
                }
            }
        } else {
            val parentFile = f.parentFile
            if (parentFile != null && !parentFile.mkdirs() && !parentFile.exists()) {
                ToastUtil.showToast("${f.absoluteFile}无法创建目录！")
                return true
            }
        }
        return false
    }

    @JvmStatic
    @Synchronized
    fun write2File(s: String, f: File): Boolean {
        if (beforWrite(f)) return false
        return try {
            FileWriter(f, false).use { fw ->
                fw.write(s)
                fw.flush()
                true
            }
        } catch (e: IOException) {
            Log.printStackTrace(TAG, e)
            false
        }
    }

    @JvmStatic
    fun copy(source: File, dest: File): Boolean {
        return try {
            FileInputStream(source).use { fis ->
                FileOutputStream(createFile(dest), false).use { fos ->
                    fis.channel.use { inputChannel ->
                        fos.channel.use { outputChannel ->
                            outputChannel.transferFrom(inputChannel, 0, inputChannel.size())
                        }
                    }
                }
            }
            true
        } catch (e: IOException) {
            Log.printStackTrace(e)
            false
        }
    }

    @JvmStatic
    fun streamTo(source: InputStream, dest: OutputStream): Boolean {
        val buffer = ByteArray(1024)
        var length: Int
        return try {
            while (source.read(buffer).also { length = it } > 0) {
                dest.write(buffer, 0, length)
                dest.flush()
            }
            true
        } catch (e: IOException) {
            Log.printStackTrace(e)
            false
        } finally {
            closeStream(source)
            closeStream(dest)
        }
    }

    private fun closeStream(stream: AutoCloseable?) {
        stream?.let {
            try {
                it.close()
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }

    @JvmStatic
    fun createFile(file: File): File? {
        if (file.exists() && file.isDirectory) {
            if (!file.delete()) return null
        }
        if (!file.exists()) {
            try {
                val parentFile = file.parentFile
                parentFile?.mkdirs()
                if (!file.createNewFile()) return null
            } catch (e: Exception) {
                Log.printStackTrace(e)
                return null
            }
        }
        return file
    }

    @JvmStatic
    fun clearFile(file: File): Boolean {
        if (!file.exists()) return false
        return try {
            FileWriter(file).use { fw ->
                fw.write("")
                fw.flush()
                true
            }
        } catch (e: IOException) {
            Log.printStackTrace(e)
            false
        }
    }

    @JvmStatic
    fun delFile(file: File): Boolean {
        if (!file.exists()) {
            ToastUtil.showToast("${file.absoluteFile}不存在！别勾把删了")
            Log.record(TAG, "delFile: ${file.absoluteFile}不存在！,无须删除")
            return false
        }

        if (file.isFile) {
            return deleteFileWithRetry(file)
        }

        val files = file.listFiles() ?: return deleteFileWithRetry(file)

        var allSuccess = true
        for (innerFile in files) {
            if (!delFile(innerFile)) {
                allSuccess = false
            }
        }

        return allSuccess && deleteFileWithRetry(file)
    }

    private fun deleteFileWithRetry(file: File): Boolean {
        var retryCount = 3
        while (retryCount > 0) {
            if (file.delete()) {
                return true
            }
            retryCount--
            Log.runtime(TAG, "删除失败，重试中: ${file.absolutePath}")
            CoroutineUtils.sleepCompat(500)
        }
        Log.error(TAG, "删除失败: ${file.absolutePath}")
        return false
    }
}
