/**
 * Single source of truth for the SDK levels every module compiles against.
 *
 * These live here so `:app`, `:core:data` and `:core:media` cannot drift apart. Module build files
 * declare only their namespace and their dependencies.
 */
internal const val COMPILE_SDK = 36
internal const val COMPILE_SDK_MINOR = 1
internal const val MIN_SDK = 28
internal const val TARGET_SDK = 36
