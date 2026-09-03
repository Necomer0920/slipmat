/**
 * Single source of truth for the SDK levels every module compiles against.
 *
 * These live here so `:app`, `:core:data` and `:core:media` cannot drift apart. Module build files
 * declare only their namespace and their dependencies.
 *
 * compileSdk is deliberately ahead of targetSdk: current AndroidX releases refuse to build
 * against anything below 37, while targetSdk stays at 36 so runtime behaviour is unchanged.
 */
internal const val COMPILE_SDK = 37
internal const val COMPILE_SDK_MINOR = 0
internal const val MIN_SDK = 28
internal const val TARGET_SDK = 36
