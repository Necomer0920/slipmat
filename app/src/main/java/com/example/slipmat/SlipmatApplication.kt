package com.example.slipmat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt's dependency graph is rooted here. Playback state deliberately is not — that lives in
 * [com.example.slipmat.core.media] behind the service, so it survives process death.
 */
@HiltAndroidApp
class SlipmatApplication : Application()
