package com.kmp.setplay.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Default value ONLY on the expect — actuals must NOT repeat it.
@Composable
expect fun GoogleSignInButton(
    isLoading: Boolean,
    onFallback: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
)