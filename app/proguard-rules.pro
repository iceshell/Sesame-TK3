# ---------- 框架 ----------
-keep class de.robv.android.xposed.** { *; }
-keep class io.github.libxposed.service.** { *; }
-dontwarn io.github.libxposed.service.**


# ---------- 日志 ----------
-keep class ch.qos.logback.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn ch.qos.logback.**, org.slf4j.**

# ---------- 本工程 ----------
-keep class fansirsqi.xposed.sesame.** { *; }

# ---------- Jackson（最小必要） ----------
-keep class com.fasterxml.jackson.** { *; }
-keepattributes Signature, *Annotation*
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.** *;
}

# ---------- 序列化 & 缺失类 ----------
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable { *; }
-dontwarn java.beans.ConstructorProperties, java.beans.Transient

# ========== 保守的性能优化 (minSdk 26+) ==========

# 优化级别：保守（不激进）
-optimizationpasses 3
-dontpreverify

# 保留源文件名和行号（便于调试和问题定位）
-keepattributes SourceFile,LineNumberTable

# 保留所有Native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 移除调试日志（仅Release模式生效）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Kotlin优化（保守）
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# 协程支持
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# R8/ProGuard兼容性
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn javax.annotation.**

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# MMKV支持
-keep class com.tencent.mmkv.** { *; }
-dontwarn com.tencent.mmkv.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Android 8.0+优化：可以安全移除旧版本兼容代码
-assumenosideeffects class android.os.Build$VERSION {
    public static int SDK_INT return 26..100;
}