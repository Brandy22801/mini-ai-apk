plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.lombok)
}

android {
    namespace = "com.miniai.plugin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.miniai.plugin"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            optIn.add("kotlin.RequiresOptIn")
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xmx8g",
                "-Xss16m",
                "-XX:+UseParallelGC"
            )
        }
        sourceSets {
            getByName("main") {
                kotlin.srcDirs("src/main/kotlin")
            }
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs(
                "src/main/assets/",
                "src/main/assets/android_assets/",
                "src/main/assets/plugin_assets/",
                "src/main/assets/knowledge_basetxt/",
                "src/main/assets/js/"
            )
        }
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    aaptOptions {
        noCompress("json", "html", "txt", "gz", "js")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.bom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.webkit)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.org.json)
    implementation(libs.gson)
    implementation(libs.lombok)
    implementation(libs.kotlin.osgi.bundle)
    implementation(libs.androidx.multidex.instrumentation)
    implementation(libs.androidx.media3.common.ktx)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    ksp(project(":ksp-processor"))
    configurations.all {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }
}

ksp {
    arg("ksp.incremental", "true")
    arg("ksp.incremental.intermodule", "true")
    arg("ksp.generate.kdoc", "true")
    arg("ksp.validate.components", "MiniAIComponent")
}

tasks.register("processMiniAIComponents") {
    group = "ksp"
    description = "Validates @MiniAIComponent annotations for release build"
    dependsOn("kspReleaseKotlin")
    doLast {
        println("Validated Mini AI components for release build")
    }
}

tasks.register<Copy>("copyMiniAIAssets") {
    group = "assets"
    description = "Copies Mini AI assets to build directory"
    from("src/main/assets")
    into("$buildDir/assets")
    include(
        "miniaiconfig.json",
        "engagement.json",
        "mini_ai.html",
        "js/**",
        "knowledge_base/**",
        "cache_files/**"
    )
    doLast {
        println("Copied Mini AI assets to $buildDir/assets")
    }
}

tasks.register<Exec>("compressMiniAIAssets") {
    group = "assets"
    description = "Compresses knowledge base and cache files using gzip"
    dependsOn("copyMiniAIAssets")
    commandLine("gzip", "-k", "-r", "$buildDir/assets/knowledge_base", "$buildDir/assets/cache_files")
    doLast {
        println("Compressed knowledge base and cache files in $buildDir/assets")
    }
}

tasks.register<org.jetbrains.dokka.gradle.DokkaTask>("generateMiniAIKDoc") {
    group = "documentation"
    description = "Generates KDoc for Mini AI plugin (release)"
    dependsOn("processMiniAIComponents")
    outputDirectory.set(file("$buildDir/kdoc"))
    moduleName.set("MiniAIPlugin")
    dokkaSourceSets {
        configureEach {
            includes.from("README.md")
            sourceRoots.from(file("src/main/kotlin"))
        }
    }
}

tasks.named("preBuild") {
    dependsOn("processMiniAIComponents", "copyMiniAIAssets", "compressMiniAIAssets")
}

tasks.named("assembleRelease") {
    dependsOn("generateMiniAIKDoc")
}

tasks.named("clean") {
    doLast {
        delete("$buildDir/intermediates")
        println("Cleaned build intermediates, preserved assets and kdoc")
    }
}