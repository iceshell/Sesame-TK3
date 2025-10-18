package fansirsqi.xposed.sesame.util;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

public class ResChecker {
    private static final String TAG = ResChecker.class.getSimpleName();

    private static boolean core(String TAG, JSONObject jo) {
        try {
//            Log.runtime(TAG, "Checking JSON success: " + jo);
            // 检查 success 或 isSuccess 字段为 true
            if (jo.optBoolean("success") || jo.optBoolean("isSuccess")) {
                return true;
            }
            // 检查 resultCode
            Object resCode = jo.opt("resultCode");
            if (resCode != null) {
                if (resCode instanceof Integer && (Integer) resCode == 200) {
                    return true;
                } else if (resCode instanceof String &&
                        Pattern.matches("(?i)SUCCESS|100", (String) resCode)) {
                    return true;
                }
            }
            // 检查 memo 字段
            if ("SUCCESS".equalsIgnoreCase(jo.optString("memo", ""))) {
                return true;
            }

            // 特殊情况：如果是"人数过多"或"小鸡睡觉"等系统状态，我们认为这不是一个需要记录的"失败"
            String resultDesc = jo.optString("resultDesc", "");
            String memo = jo.optString("memo", "");
            String resultCode = jo.optString("resultCode", "");
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
                    "I07".equals(resultCode)) {
                 return false; // 返回false，但不打印错误日志
             }
            // 获取调用栈信息以确定错误来源
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callerInfo = getString(stackTrace);
            Log.error(TAG, "Check failed: [来源: " + callerInfo + "] " + jo);
            return false;
        } catch (Throwable t) {
            Log.printStackTrace(TAG, "Error checking JSON success:", t);
            return false;
        }
    }

    @NonNull
    private static String getString(StackTraceElement[] stackTrace) {
        StringBuilder callerInfo = new StringBuilder();
        int foundCount = 0;
        // 最多显示4层调用栈
        final int MAX_STACK_DEPTH = 4;
        final String PROJECT_PACKAGE = "fansirsqi.xposed.sesame";
        
        // 寻找项目包名下的调用者
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            // 只显示项目包名下的类，跳过ResChecker
            if (className.startsWith(PROJECT_PACKAGE) && !className.contains("ResChecker")) {
                // 获取类名（保留项目包名后的部分）
                String relativeClassName = className.substring(PROJECT_PACKAGE.length() + 1);
                if (foundCount > 0) {
                    callerInfo.append(" <- ");
                }
                callerInfo.append(relativeClassName)
                         .append(".")
                         .append(element.getMethodName())
                         .append(":")
                         .append(element.getLineNumber());
                
                foundCount++;
                if (foundCount >= MAX_STACK_DEPTH) {
                    break;
                }
            }
        }

        return callerInfo.toString();
    }

    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jo JSON对象
     * @return true 如果成功
     */
    public static boolean checkRes(String TAG, JSONObject jo) {
        return core(TAG, jo);
    }

    /**
     * 检查JSON对象是否表示成功
     * <p>
     * 成功条件包括：<br/>
     * - success == true<br/>
     * - isSuccess == true<br/>
     * - resultCode == 200 或 "SUCCESS" 或 "100"<br/>
     * - memo == "SUCCESS"<br/>
     *
     * @param jsonStr JSON对象的字符串表示
     * @return true 如果成功
     */
    public static boolean checkRes(String TAG, String jsonStr) throws JSONException {
        JSONObject jo = new JSONObject(jsonStr);
        return checkRes(TAG, jo);
    }
}