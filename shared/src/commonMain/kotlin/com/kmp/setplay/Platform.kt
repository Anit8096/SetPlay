package com.kmp.setplay

/**
 * True when running on Android. Used to pick between platform-appropriate
 * interactions where a single commonMain composable can't fit both — e.g.
 * a "Scan QR" action only makes sense with a device camera (Android),
 * while a manual refresh button covers Web/Desktop where there's no
 * touch-drag gesture to trigger pull-to-refresh.
 */
expect fun isAndroidPlatform(): Boolean
