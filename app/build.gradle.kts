plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.meyeringh.cfswitch"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.meyeringh.cfswitch"
        minSdk = 26
        targetSdk = 35
        versionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val releaseStoreFile =
                findProperty("RELEASE_STORE_FILE") as String?
                    ?: System.getenv("RELEASE_STORE_FILE")
            val releaseStorePassword =
                findProperty("RELEASE_STORE_PASSWORD") as String?
                    ?: System.getenv("RELEASE_STORE_PASSWORD")
            val releaseKeyAlias =
                findProperty("RELEASE_KEY_ALIAS") as String?
                    ?: System.getenv("RELEASE_KEY_ALIAS")
            val releaseKeyPassword =
                findProperty("RELEASE_KEY_PASSWORD") as String?
                    ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (releaseStoreFile != null &&
                releaseStorePassword != null &&
                releaseKeyAlias != null &&
                releaseKeyPassword != null
            ) {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseStoreFile =
                findProperty("RELEASE_STORE_FILE") as String?
                    ?: System.getenv("RELEASE_STORE_FILE")

            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fall back to debug signing locally
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    // Verify signing config when assembling release on CI
    afterEvaluate {
        tasks.named("assembleRelease") {
            doFirst {
                val releaseStoreFile =
                    findProperty("RELEASE_STORE_FILE") as String?
                        ?: System.getenv("RELEASE_STORE_FILE")
                if (System.getenv("CI") == "true" && releaseStoreFile == null) {
                    throw GradleException("Release build on CI requires signing configuration")
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        managedDevices {
            devices {
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Security
    implementation(libs.androidx.security.crypto)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.mockito.core)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
