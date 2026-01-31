import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.GradleException
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.rikka.tools.refine)
}

abstract class CopyReleaseApkWithRcTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceApkFile: RegularFileProperty

    @get:Input
    abstract val versionNameBase: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rcFile: RegularFileProperty

    @TaskAction
    fun copy() {
        val version = versionNameBase.get()
        val rc = rcFile.get().asFile.readText().trim().ifBlank {
            throw GradleException("rcFile is blank: ${rcFile.get().asFile.absolutePath}")
        }

        val sourceApk = sourceApkFile.get().asFile
        if (!sourceApk.isFile) {
            throw GradleException("Missing app-release.apk: ${sourceApk.absolutePath}")
        }

        val targetApk = sourceApk.parentFile.resolve("Sesame-TK-v${version}-rc${rc}.apk")
        sourceApk.copyTo(targetApk, overwrite = true)
    }
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

abstract class GenerateReleaseRcTask : DefaultTask() {
    @get:Input
    abstract val versionNameBase: Property<String>

    @get:OutputFile
    abstract val counterFile: RegularFileProperty

    @get:OutputFile
    abstract val rcOutputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val version = versionNameBase.get()
        val counter = counterFile.get().asFile
        counter.parentFile?.mkdirs()

        val props = Properties()
        if (counter.isFile) {
            counter.inputStream().use { props.load(it) }
        }

        val storedVersion = props.getProperty("versionNameBase", "")
        val lastRc = props.getProperty("lastRc", "0").toIntOrNull() ?: 0
        val nextRc = if (storedVersion == version) lastRc + 1 else 1

        props.setProperty("versionNameBase", version)
        props.setProperty("lastRc", nextRc.toString())
        counter.outputStream().use { props.store(it, null) }

        val rcText = nextRc.toString().padStart(2, '0')
        val rcFile = rcOutputFile.get().asFile
        rcFile.parentFile?.mkdirs()
        rcFile.writeText(rcText)
    }
}

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

abstract class BuildVersionCodeValueSource : ValueSource<Int, ValueSourceParameters.None> {
    override fun obtain(): Int {
        val tz = TimeZone.getTimeZone("GMT+8")
        val nowMs = System.currentTimeMillis()

        val baseCal = Calendar.getInstance(tz).apply {
            set(2020, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val baseDay = baseCal.timeInMillis / 86_400_000L

        val nowCal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMs
        }
        val nowDay = nowCal.timeInMillis / 86_400_000L
        val dayIndex = (nowDay - baseDay).toInt().coerceAtLeast(0)

        val secondsOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 3600 +
            nowCal.get(Calendar.MINUTE) * 60 +
            nowCal.get(Calendar.SECOND)

        return dayIndex * 100_000 + secondsOfDay
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
    val autoVersionCode: Int = providers.of(BuildVersionCodeValueSource::class.java) {}.get()
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

        versionCode = autoVersionCode
        val buildTag = "release"
        versionName = providers.gradleProperty("versionNameBase").orElse("0.6.0").get()

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

    compileOptions {
        isCoreLibraryDesugaringEnabled = true // 启用脱糖
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    signingConfigs {
        getByName("debug") {
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
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

val rcCounterFile = layout.buildDirectory.file("rc/rc-counter.properties")
val releaseRcTextFile = layout.buildDirectory.file("rc/release-rc.txt")

tasks.register<GenerateReleaseRcTask>("generateReleaseRc") {
    versionNameBase.set(providers.gradleProperty("versionNameBase").orElse("0.6.0"))
    counterFile.set(rcCounterFile)
    rcOutputFile.set(releaseRcTextFile)
    outputs.upToDateWhen { false }
}

tasks.register<CopyReleaseApkWithRcTask>("copyReleaseApkWithRc") {
    dependsOn("assembleRelease")
    dependsOn("generateReleaseRc")
    sourceApkFile.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    versionNameBase.set(providers.gradleProperty("versionNameBase").orElse("0.6.0"))
    rcFile.set(releaseRcTextFile)
}

tasks.matching { it.name == "assembleRelease" }.configureEach {
    finalizedBy("copyReleaseApkWithRc")
}

dependencies {

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
