plugins {
    // No version: build-logic already puts AGP on the classpath, and asking for one there fails
    // with "already on the classpath with an unknown version".
    id("com.android.test")
}

android {
    namespace = "com.example.slipmat.benchmark"

    // Not the convention plugin: that one configures library and application modules, and this is
    // com.android.test, which has neither's extension type.
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // The variant under test: `benchmark` in :app is release, signed with the debug key.
        create("benchmark") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        // A macrobenchmark of a debug build measures the debugger; only the benchmark variant is
        // worth building at all.
        variant.enable = variant.buildType == "benchmark"
    }
}
