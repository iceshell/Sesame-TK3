// ktlint Gradle 集成配置
// 在 app/build.gradle.kts 中添加此配置

/*
使用方法：
1. 在 app/build.gradle.kts 顶部添加:
   apply(from = "$rootDir/ktlint-gradle-setup.gradle.kts")

2. 运行命令:
   ./gradlew ktlintCheck   # 检查代码风格
   ./gradlew ktlintFormat  # 自动格式化代码
*/

// 添加 ktlint 配置
val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest.ktlint:ktlint-cli:1.1.1") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

// 创建 ktlintCheck 任务
val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}

// 创建 ktlintFormat 任务
val ktlintFormat by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fix Kotlin code style violations"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(
        "-F",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}

// 让 check 任务依赖 ktlintCheck
tasks.named("check") {
    dependsOn(ktlintCheck)
}
