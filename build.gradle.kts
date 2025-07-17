import com.sun.source.util.Plugin

// Top-level build file where you can add configuration options common to all sub-projects/modules.
Plugin {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.ksp) apply false
     }
}
allprojects {
repositories {
google()
mavenCentral()
}
