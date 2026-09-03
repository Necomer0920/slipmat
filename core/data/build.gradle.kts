plugins {
    id("slipmat.android.library")
}

android {
    namespace = "com.example.slipmat.core.data"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
