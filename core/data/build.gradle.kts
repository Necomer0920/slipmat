plugins {
    id("slipmat.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.slipmat.core.data"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
