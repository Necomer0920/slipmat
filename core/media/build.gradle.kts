plugins {
    id("slipmat.android.library")
}

android {
    namespace = "com.example.slipmat.core.media"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
