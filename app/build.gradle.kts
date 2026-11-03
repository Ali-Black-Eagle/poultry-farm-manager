plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.poultryapp"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.profarm.poultryapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    val keystorePropertiesFile = if (file("keystore.properties").exists()) file("keystore.properties") else rootProject.file("keystore.properties")
    val hasKeystoreProps = keystorePropertiesFile.exists()
    val keystoreProps = if (hasKeystoreProps) {
        keystorePropertiesFile.readLines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) null
            else {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
            }
        }.toMap()
    } else emptyMap()

    signingConfigs {
        create("release") {
            val storeFileName = keystoreProps["storeFile"] ?: "profarm_release.jks"
            val resolvedStoreFile = if (file(storeFileName).exists()) file(storeFileName) else rootProject.file(storeFileName)
            storeFile = resolvedStoreFile.absoluteFile
            storePassword = keystoreProps["storePassword"] ?: "ProFarm_Release_Key_2026_SecureVault!"
            keyAlias = keystoreProps["keyAlias"] ?: "profarm_key"
            keyPassword = keystoreProps["keyPassword"] ?: "ProFarm_Release_Key_2026_SecureVault!"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner, org.json
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation("org.json:json:20240303")

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
}

afterEvaluate {
    tasks.findByName("packageRelease")?.let { pkgTask ->
        pkgTask.doLast {
            val intermediateApk = file("build/intermediates/apk/release/app-release.apk")
            val outputApk = file("build/outputs/apk/release/app-release.apk")
            if (intermediateApk.exists()) {
                outputApk.parentFile.mkdirs()
                intermediateApk.copyTo(outputApk, overwrite = true)
            }
        }
    }
}
