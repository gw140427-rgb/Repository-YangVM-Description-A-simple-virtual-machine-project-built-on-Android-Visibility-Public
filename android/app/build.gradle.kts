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
        versionCode = 5
        versionName = "0.2.4"
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}
