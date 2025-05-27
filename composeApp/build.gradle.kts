import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
            implementation("com.google.firebase:firebase-auth")
            implementation("com.google.firebase:firebase-firestore")
            // PreCompose only for Android
            implementation("moe.tlaster:precompose:1.5.10")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        
        // Add Firebase dependencies only for non-wasm targets
        val nonWasmMain by creating {
            dependsOn(commonMain.get())
            
            dependencies {
                implementation("dev.gitlive:firebase-auth:1.11.1")
                implementation("dev.gitlive:firebase-firestore:1.11.1")
                implementation("dev.gitlive:firebase-common:1.11.1")
            }
        }
        
        androidMain.get().dependsOn(nonWasmMain)
        iosMain.get().dependsOn(nonWasmMain)
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.rapido.rocket"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rapido.rocket"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Task to copy unified index.html to production build directory
tasks.register<Copy>("copyIndexHtml") {
    from("src/wasmJsMain/resources/index.html")
    into("build/kotlin-webpack/wasmJs/productionExecutable")
}

// Make the copy task run after production webpack
tasks.named("wasmJsBrowserProductionWebpack") {
    finalizedBy("copyIndexHtml")
}

