plugins {
    id("com.android.application")
}
android {
    namespace = "com.neovm.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.neovm.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
