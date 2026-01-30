import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.rikka.tools.refine)
}

tasks.register("checkAll") {
    dependsOn(
        "detekt",
        "test",
        "assembleRelease"
    )
}
var isCIBuild: Boolean = System.getenv("CI").toBoolean()

val enableDetekt = gradle.startParameter.taskNames.any { it.contains("detekt", ignoreCase = true) }
if (enableDetekt) {
    apply(plugin = "io.gitlab.arturbosch.detekt")
}

plugins.withId("io.gitlab.arturbosch.detekt") {
    extensions.configure<DetektExtension> {
        baseline = rootProject.file("detekt-baseline.xml")
        config.setFrom(rootProject.files("detekt.yml"))
    }

    tasks.register<Detekt>("detektStrict") {
        config.setFrom(rootProject.files("detekt-strict.yml"))
        setSource(files("src/main/java", "src/main/kotlin"))
        include("**/*.kt", "**/*.kts")
        exclude("**/build/**")

        jvmTarget = "17"
        classpath.setFrom(
            configurations.findByName("releaseCompileClasspath")
                ?: configurations.findByName("debugCompileClasspath")
                ?: files()
        )
    }
}

configurations.matching { it.name == "composeMappingProducerClasspath" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "compose-group-mapping") {
            val kotlinVersion = libs.versions.kotlin.plugin.get()
            useVersion(kotlinVersion)
            because("Align compose-group-mapping with Kotlin/Compose plugin ${kotlinVersion}")
        }
    }
}

//isCIBuild = true // 没有c++源码时开启CI构建, push前关闭

// 定义 ValueSource 以兼容配置缓存
abstract class GitCommitCountValueSource : ValueSource<Int, ValueSourceParameters.None> {
    override fun obtain(): Int? {
        return try {
            val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().use { it.readText().trim() }.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

abstract class BuildTimestampValueSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        return SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())
    }
}

abstract class CopyApkWithTimestampTask : DefaultTask() {
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val variant: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val timestamp: Property<String>

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun copy() {
        val ts = timestamp.get()
        val vn = versionName.get()
        val variantName = variant.get()
        val baseVersion = vn.substringBefore("-").ifBlank { vn }

        val inputFolder = inputDir.get().asFile
        val primaryApkName = "app-${variantName}.apk"
        val hasPrimaryApk = inputFolder.resolve(primaryApkName).isFile
        if (!hasPrimaryApk) {
            val apkCount = inputFolder.listFiles { file ->
                file.isFile && file.extension.equals("apk", ignoreCase = true)
            }?.size ?: 0
            if (apkCount != 1) {
                throw GradleException(
                    "Expected exactly one APK in ${inputFolder.absolutePath} when primary apk is missing; found $apkCount"
                )
            }
        }

        fileSystemOperations.copy {
            from(inputDir)
            if (hasPrimaryApk) {
                include(primaryApkName)
            } else {
                include("*.apk")
            }
            into(outputDir)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            rename { fileName ->
                // 仅保留唯一命名格式：sesame-tk-v0.5.0-<yyyyMMdd-HHmmss>.apk
                // 旧的多分支命名逻辑已按要求注释掉：
                /*
                val base = fileName.removeSuffix(".apk")
                if (hasPrimaryApk) {
                    "sesame-tk-v${vn}-${ts}.apk"
                } else {
                    "sesame-tk-v${vn}-${ts}-${variantName}-${base}.apk"
                }
                */
                "sesame-tk-v${baseVersion}-${ts}.apk"
            }
        }
    }
}

android {
    namespace = "fansirsqi.xposed.sesame"
    compileSdk = 36
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    // 获取版本递增计数（兼容配置缓存）
    // 临时固定版本号为161 - 细粒度错误处理改进
    val gitCommitCount: Int = 303    /*
    val gitCommitCount: Int = providers.of(GitCommitCountValueSource::class.java) {}
        .orElse(providers.provider {
            // 如果git不可用，使用时间戳的后4位作为递增值
            val timestamp = System.currentTimeMillis() / 1000
            (timestamp % 10000).toInt()
        }).get()
    */
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "fansirsqi.xposed.sesame"
        minSdk = 26  // ✅ Android 8.0+ (优化性能5-10%, 匹配LSPosed要求)
        targetSdk = 36

        if (!isCIBuild) {
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }


        val buildDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        val buildTime = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        val buildTargetCode = try {
            buildDate.replace("-", ".") + "." + buildTime.replace(":", ".")
        } catch (_: Exception) {
            "0000"
        }

        versionCode = gitCommitCount
        val buildTag = "release"
        // 版本号规范：遵循语义化版本+构建号
        versionName = "0.5.0-rc$versionCode"

        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "BUILD_NUMBER", "\"$buildTargetCode\"")
        buildConfigField("String", "BUILD_TAG", "\"$buildTag\"")
        buildConfigField("String", "VERSION", "\"$versionName\"")

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        testOptions {
            unitTests.all {
                it.enabled = true
                it.testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = true
                }
            }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }


    // ✅ 已移除 productFlavors，统一使用单一变体配置
    // flavorDimensions += "default"
    // productFlavors { ... } - 已移除Compatible变体
    compileOptions {
        // 全局默认设置
        isCoreLibraryDesugaringEnabled = true // 启用脱糖
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    // ✅ 已移除 productFlavors.all 配置，所有项目统一使用 Java 17

    signingConfigs {
        getByName("debug") {
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            // applicationIdSuffix = ".debug"  // ✅ 已移除后缀，Debug和Release使用相同包名，可覆盖安装
            // versionNameSuffix = ".debug"  // ✅ 移除versionNameSuffix，统一在APK文件名中体现
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs {
                directories.add("src/main/jniLibs")
            }
        }
    }
    val cmakeFile = file("src/main/cpp/CMakeLists.txt")
    if (!isCIBuild && cmakeFile.exists()) {
        externalNativeBuild {
            cmake {
                path = cmakeFile
                version = "3.31.6"
                ndkVersion = "29.0.13113456"
            }
        }
    }
}

val buildTimestampProvider = providers.of(BuildTimestampValueSource::class.java) {}
val versionNameForApkNaming = android.defaultConfig.versionName ?: "unknown"

tasks.register<CopyApkWithTimestampTask>("copyReleaseApkWithTimestamp") {
    dependsOn("assembleRelease")
    inputDir.set(layout.buildDirectory.dir("outputs/apk/release"))
    outputDir.set(layout.buildDirectory.dir("outputs/apk-timestamped/release"))
    variant.set("release")
    versionName.set(versionNameForApkNaming)
    timestamp.set(buildTimestampProvider)
}

tasks.register<CopyApkWithTimestampTask>("copyDebugApkWithTimestamp") {
    dependsOn("assembleDebug")
    inputDir.set(layout.buildDirectory.dir("outputs/apk/debug"))
    outputDir.set(layout.buildDirectory.dir("outputs/apk-timestamped/debug"))
    variant.set("debug")
    versionName.set(versionNameForApkNaming)
    timestamp.set(buildTimestampProvider)
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy("copyReleaseApkWithTimestamp")
}

tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy("copyDebugApkWithTimestamp")
}

dependencies {

    // Shizuku 相关依赖 - 用于获取系统级权限
    implementation(libs.rikka.shizuku.api)        // Shizuku API
    implementation(libs.rikka.shizuku.provider)   // Shizuku 提供者
    implementation(libs.rikka.refine)             // Rikka 反射工具
    implementation(libs.ui.tooling.preview.android)

    // Compose 相关依赖 - 现代化 UI 框架
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")  // Compose BOM 版本管理
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.material3)                // Material 3 设计组件
    implementation(libs.androidx.ui.tooling.preview)              // UI 工具预览
    debugImplementation(libs.androidx.ui.tooling)                 // 调试时的 UI 工具

    // 生命周期和数据绑定
    implementation(libs.androidx.lifecycle.viewmodel.compose) // Compose ViewModel 支持

    // JSON 序列化
    implementation(libs.kotlinx.serialization.json) // Kotlin JSON 序列化库

    // Kotlin 协程依赖 - 异步编程
    implementation(libs.kotlinx.coroutines.core)     // 协程核心库
    implementation(libs.kotlinx.coroutines.android)  // Android 协程支持

    // 数据观察和 HTTP 服务
    implementation(libs.androidx.lifecycle.livedata.ktx)  // LiveData KTX 扩展
    implementation(libs.androidx.runtime.livedata)        // Compose LiveData 运行时
    implementation(libs.nanohttpd)                   // 轻量级 HTTP 服务器

    // UI 布局和组件
    implementation(libs.androidx.constraintlayout)  // 约束布局

    implementation(libs.activity.compose)           // Compose Activity 支持

    // Android 核心库
    implementation(libs.core.ktx)                   // Android KTX 核心扩展
    implementation(libs.kotlin.stdlib)              // Kotlin 标准库
    implementation(libs.slf4j.api)                  // SLF4J 日志 API
    implementation(libs.logback.android)            // Logback Android 日志实现
    implementation(libs.appcompat)                  // AppCompat 兼容库
    implementation(libs.recyclerview)               // RecyclerView 列表组件
    implementation(libs.viewpager2)                 // ViewPager2 页面滑动
    implementation(libs.material)                   // Material Design 组件
    implementation(libs.webkit)                     // WebView 组件

    // 仅编译时依赖 - Xposed 相关
    compileOnly(files("libs/api-82.jar"))          // Xposed API 82
    compileOnly(files("libs/api-100.aar"))         // Xposed API 100 https://github.com/libxposed/api
    compileOnly(files("libs/interface-100.aar")) // Xposed 模块接口 https://github.com/libxposed/api
    implementation(files("libs/service-100-1.0.0.aar"))  // https://github.com/libxposed/service
//    compileOnly(files("libs/helper-100.aar"))        // https://github.com/libxposed/helper

    // 代码生成和工具库
    implementation(libs.okhttp)                    // OkHttp 网络请求库
    implementation(libs.dexkit)                    // DEX 文件分析工具
    implementation(libs.jackson.kotlin)            // Jackson Kotlin 支持
    implementation(libs.mmkv)                      // 腾讯 MMKV 高性能键值存储

    // 核心库脱糖和系统 API 访问
    coreLibraryDesugaring(libs.desugar)            // Java 8+ API 脱糖支持

    implementation(libs.hiddenapibypass)           // 隐藏 API 访问绕过

    // ✅ Jackson JSON 处理库 - 统一使用最新版本
    implementation(libs.jackson.core)         // Jackson 核心库
    implementation(libs.jackson.databind)     // Jackson 数据绑定
    implementation(libs.jackson.annotations)  // Jackson 注解

    // ========== 测试依赖 ==========
    
    // JUnit - 基础测试框架
    testImplementation("junit:junit:4.13.2")
    
    // Kotlin 测试
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.3.0")
    
    // 协程测试
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // MockK - Kotlin Mock框架
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    
    // Truth - Google断言库
    testImplementation("com.google.truth:truth:1.1.5")
    
    // Robolectric - Android单元测试框架
    testImplementation("org.robolectric:robolectric:4.11.1")
}
