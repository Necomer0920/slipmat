plugins {
    id("slipmat.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.slipmat.core.data"

    // MigrationTestHelper reads the exported schemas at runtime, so they have to ship inside the
    // androidTest APK as assets. Without this the test fails with "Cannot find the schema file".
    sourceSets.getByName("androidTest") {
        assets.srcDir(files("$projectDir/schemas"))
    }
}

/**
 * Room writes the schema of every version to disk. Migration tests in Phases 5 and 7 need the
 * previous version's JSON to migrate from, so this must be in place before version 1 ships.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
}
