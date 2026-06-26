package com.kmp.setplay.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun GoogleSignInButton(
    isLoading: Boolean,
    onFallback: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier  // no default value on actual
) {
    OutlinedButton(
        onClick = onFallback,
        enabled = !isLoading,
        modifier = modifier
    ) {
        AnimatedVisibility(isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp
            )
        }
        Text("Continue with Google")
    }
}