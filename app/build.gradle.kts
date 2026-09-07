plugins {
    alias(libs.plugins.hilt)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.luminor.actionbox"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.luminor.actionbox"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "3.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
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
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform(libs.compose.compose.bom)

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.core.core.ktx)
    implementation(libs.activity.activity.compose)
    implementation(libs.lifecycle.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.lifecycle.runtime.compose)
    implementation(libs.lifecycle.lifecycle.viewmodel.compose)
    implementation(libs.navigation.navigation.compose)

    implementation(libs.compose.ui.ui)
    implementation(libs.compose.ui.ui.tooling.preview)
    implementation(libs.compose.foundation.foundation)
    implementation(libs.compose.animation.animation)
    implementation(libs.compose.material3.material3)
    implementation(libs.compose.material.material.icons.extended)
    debugImplementation(libs.compose.ui.ui.tooling)

    implementation(libs.room.room.runtime)
    implementation(libs.room.room.ktx)
    ksp(libs.room.room.compiler)

    implementation(libs.datastore.datastore.preferences)
    implementation(libs.work.work.runtime.ktx)

    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espresso.espresso.core)
    androidTestImplementation(libs.compose.ui.ui.test.junit4)
    androidTestImplementation(libs.room.room.testing)
    debugImplementation(libs.compose.ui.ui.test.manifest)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.hilt.hilt.navigation.compose)
    implementation(libs.hilt.hilt.work)
    ksp(libs.hilt.hilt.compiler)
    implementation(libs.paging.paging.runtime.ktx)
    implementation(libs.paging.paging.compose)
    implementation(libs.room.room.paging)
    testImplementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.test)
    androidTestImplementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.test)
    androidTestImplementation(libs.paging.paging.testing)
}
