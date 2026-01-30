plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }
}

tasks.register("detekt") {
    dependsOn(":app:detekt")
}

tasks.register("detektBaseline") {
    dependsOn(":app:detektBaseline")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
