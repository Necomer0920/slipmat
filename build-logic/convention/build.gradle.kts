plugins {
    `kotlin-dsl`
}

group = "com.example.slipmat.buildlogic"

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "slipmat.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "slipmat.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
