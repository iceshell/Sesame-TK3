package fansirsqi.xposed.sesame.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.util.maps.CooperateMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.UserMap
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 导入导出工具类
 * 处理配置文件的导入和导出操作
 */
object PortUtil {
    
    /**
     * 处理配置导出
     *
     * @param context 上下文
     * @param uri 导出目标URI
     * @param userId 用户ID
     */
    @JvmStatic
    fun handleExport(context: Context, uri: Uri?, userId: String?) {
        if (uri == null) {
            ToastUtil.makeText("未选择目标位置", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val configV2File = if (userId.isNullOrEmpty()) {
                Files.getDefaultConfigV2File()
            } else {
                Files.getConfigV2File(userId)
            }
            
            val inputStream = FileInputStream(configV2File)
            val outputStream = context.contentResolver.openOutputStream(uri)
            
            if (Files.streamTo(inputStream, outputStream!!)) {
                ToastUtil.makeText("导出成功！", Toast.LENGTH_SHORT).show()
            } else {
                ToastUtil.makeText("导出失败！", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.printStackTrace(e)
            ToastUtil.makeText("导出失败：发生异常", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 处理配置导入
     *
     * @param context 上下文
     * @param uri 导入源URI
     * @param userId 用户ID
     */
    @JvmStatic
    fun handleImport(context: Context, uri: Uri?, userId: String?) {
        if (uri == null) {
            ToastUtil.makeText("导入失败：未选择文件", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val configV2File = if (userId.isNullOrEmpty()) {
                Files.getDefaultConfigV2File()
            } else {
                Files.getConfigV2File(userId)
            }
            
            val outputStream = FileOutputStream(configV2File)
            val inputStream = context.contentResolver.openInputStream(uri)
            
            if (Files.streamTo(inputStream!!, outputStream)) {
                ToastUtil.makeText("导入成功！", Toast.LENGTH_SHORT).show()
                
                if (!userId.isNullOrEmpty()) {
                    try {
                        val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                        intent.putExtra("userId", userId)
                        context.sendBroadcast(intent)
                    } catch (th: Throwable) {
                        Log.printStackTrace(th)
                    }
                }
                
                val intent = (context as Activity).intent
                context.finish()
                context.startActivity(intent)
            } else {
                ToastUtil.makeText("导入失败！", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.printStackTrace(e)
            ToastUtil.makeText("导入失败：发生异常", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 保存配置
     *
     * @param context 上下文
     * @param userId 用户ID
     */
    @JvmStatic
    fun save(context: Context, userId: String?) {
        try {
            if (Config.isModify(userId) && Config.save(userId, false)) {
                ToastUtil.showToastWithDelay("保存成功！", 100)
                
                if (!userId.isNullOrEmpty()) {
                    val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                    intent.putExtra("userId", userId)
                    context.sendBroadcast(intent)
                }
            }
            
            if (!userId.isNullOrEmpty()) {
                UserMap.save(userId)
                IdMapManager.getInstance(CooperateMap::class.java).save(userId)
            }
        } catch (th: Throwable) {
            Log.printStackTrace(th)
        }
    }
}
