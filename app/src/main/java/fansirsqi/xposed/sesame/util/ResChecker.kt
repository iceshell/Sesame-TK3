package fansirsqi.xposed.sesame.util

import org.json.JSONException
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * 响应检查工具类
 * 用于检查JSON响应是否表示成功
 */
object ResChecker {
    
    private val TAG = ResChecker::class.java.simpleName
    
    /**
     * 核心检查逻辑
     */
    private fun core(tag: String, jo: JSONObject): Boolean {
        return try {
            // 检查 success 或 isSuccess 字段为 true
            if (jo.optBoolean("success") || jo.optBoolean("isSuccess")) {
                return true
            }
            
            // 检查 resultCode
            val resCode = jo.opt("resultCode")
            if (resCode != null) {
                when (resCode) {
                    is Int -> if (resCode == 200) return true
                    is String -> if (Pattern.matches("(?i)SUCCESS|100", resCode)) return true
                }
            }
            
            // 检查 memo 字段
            if ("SUCCESS".equals(jo.optString("memo", ""), ignoreCase = true)) {
                return true
            }
            
            // 特殊情况：如果是"人数过多"或"小鸡睡觉"等系统状态，我们认为这不是一个需要记录的"失败"
            val resultDesc = jo.optString("resultDesc", "")
            val memo = jo.optString("memo", "")
            val resultCode = jo.optString("resultCode", "")
            
            if (resultDesc.contains("当前参与人数过多") || resultDesc.contains("请稍后再试") ||
                resultDesc.contains("手速太快") || resultDesc.contains("频繁") ||
                resultDesc.contains("操作过于频繁") ||
                memo.contains("我的小鸡在睡觉中") ||
                memo.contains("小鸡在睡觉") ||
                memo.contains("无法操作") ||
                memo.contains("手速太快") ||
                memo.contains("有人抢在你") ||
                memo.contains("饲料槽已满") ||
                memo.contains("当日达到上限") ||
                memo.contains("适可而止") ||
                memo.contains("不支持rpc完成的任务") ||
                memo.contains("庄园的小鸡太多了") ||
                memo.contains("任务已完成") ||
                "I07" == resultCode ||
                "FAMILY48" == resultCode
            ) {
                return false // 返回false，但不打印错误日志
            }
            
            // 获取调用栈信息以确定错误来源
            val stackTrace = Thread.currentThread().stackTrace
            val callerInfo = getString(stackTrace)
            Log.error(tag, "Check failed: [来源: $callerInfo] $jo")
            false
        } catch (t: Throwable) {
            Log.printStackTrace(tag, "Error checking JSON success:", t)
            false
        }
    }
    
    /**
     * 获取调用栈字符串
     */
    private fun getString(stackTrace: Array<StackTraceElement>): String {
        val callerInfo = StringBuilder()
        var foundCount = 0
        val maxStackDepth = 4
        val projectPackage = "fansirsqi.xposed.sesame"
        
        // 寻找项目包名下的调用者
        for (element in stackTrace) {
            val className = element.className
            // 只显示项目包名下的类，跳过ResChecker
            if (className.startsWith(projectPackage) && !className.contains("ResChecker")) {
                // 获取类名（保留项目包名后的部分）
                val relativeClassName = className.substring(projectPackage.length + 1)
                if (foundCount > 0) {
                    callerInfo.append(" <- ")
                }
                callerInfo.append(relativeClassName)
                    .append(".")
                    .append(element.methodName)
                    .append(":")
                    .append(element.lineNumber)
                
                foundCount++
                if (foundCount >= maxStackDepth) {
                    break
                }
            }
        }
        
        return callerInfo.toString()
    }
    
    /**
     * 检查JSON对象是否表示成功
     *
     * 成功条件包括：
     * - success == true
     * - isSuccess == true
     * - resultCode == 200 或 "SUCCESS" 或 "100"
     * - memo == "SUCCESS"
     *
     * @param tag 标签
     * @param jo JSON对象
     * @return true 如果成功
     */
    @JvmStatic
    fun checkRes(tag: String, jo: JSONObject): Boolean {
        return core(tag, jo)
    }
    
    /**
     * 检查JSON对象是否表示成功
     *
     * 成功条件包括：
     * - success == true
     * - isSuccess == true
     * - resultCode == 200 或 "SUCCESS" 或 "100"
     * - memo == "SUCCESS"
     *
     * @param tag 标签
     * @param jsonStr JSON对象的字符串表示
     * @return true 如果成功
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun checkRes(tag: String, jsonStr: String?): Boolean {
        // 检查null或空字符串
        if (jsonStr.isNullOrBlank()) {
            Log.record(TAG, "[$tag] RPC响应为空")
            return false
        }
        val jo = JSONObject(jsonStr)
        return checkRes(tag, jo)
    }
}
