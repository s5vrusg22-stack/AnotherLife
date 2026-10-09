plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "com.anotherlife.app"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.anotherlife.app"
  minSdk = 28
  targetSdk = 35
  versionCode = 1
  versionName = "0.2.0-native"
  ndk { abiFilters += "arm64-v8a" }
 }
 buildFeatures { compose = true }
 if (providers.gradleProperty("enableLlamaNative").orNull == "true") {
  externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 testImplementation("junit:junit:4.13.2")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
}