plugins {
    id("slipmat.android.library")
}

android {
    namespace = "com.example.slipmat.core.data"
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
