package com.kmp.setplay

import org.koin.core.module.Module

/**
 * Platform-specific Koin modules.
 * Android → ( androidDatabaseModule )
 * Web     → empty list (no Room)
 */
expect fun platformModules(): List<Module>