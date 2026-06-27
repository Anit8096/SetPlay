package com.kmp.setplay

import com.kmp.setplay.data.local.LocalCache
import com.kmp.setplay.data.local.NoOpLocalCache
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModules(): List<Module> = listOf(
    module {
        single<LocalCache> { NoOpLocalCache() }
    }
)