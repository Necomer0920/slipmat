plugins {
    id("slipmat.android.library")
}

android {
    namespace = "com.example.slipmat.core.media"
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
