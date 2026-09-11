plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.zcode.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.zcode.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.1"
        vectorDrawables.useSupportLibrary = true
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        // Release signing, in order of precedence:
        //   1. RELEASE_STORE_FILE / RELEASE_STORE_PASSWORD / RELEASE_KEY_ALIAS / RELEASE_KEY_PASSWORD env vars (CI)
        //   2. keystore.properties at the repo root (local, git-ignored)
        //   3. the debug keystore — installable, but NOT upgradeable across machines
        create("release") {
            val env = System.getenv()
            val propsFile = rootProject.file("keystore.properties")
            when {
                !env["RELEASE_STORE_FILE"].isNullOrBlank() -> {
                    storeFile = rootProject.file(env.getValue("RELEASE_STORE_FILE"))
                    storePassword = env["RELEASE_STORE_PASSWORD"]
                    keyAlias = env["RELEASE_KEY_ALIAS"]
                    keyPassword = env["RELEASE_KEY_PASSWORD"]
                }
                propsFile.exists() -> {
                    val props = propsFile.readLines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                        .associate { line ->
                            val i = line.indexOf('=')
                            line.substring(0, i) to line.substring(i + 1)
                        }
                    storeFile = rootProject.file(props.getValue("storeFile"))
                    storePassword = props.getValue("storePassword")
                    keyAlias = props.getValue("keyAlias")
                    keyPassword = props.getValue("keyPassword")
                }
                else -> {
                    logger.warn("No release keystore configured; signing release with the debug keystore.")
                    storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ""
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "SENSITIVE_LOGS", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("boolean", "SENSITIVE_LOGS", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.documentfile)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.coil.compose)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    debugImplementation(libs.androidx.compose.ui.tooling)
}
