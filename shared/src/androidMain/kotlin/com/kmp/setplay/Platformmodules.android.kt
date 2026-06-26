package com.kmp.setplay

import com.kmp.setplay.di.androidDatabaseModule
import org.koin.core.module.Module

actual fun platformModules(): List<Module> = listOf(androidDatabaseModule)