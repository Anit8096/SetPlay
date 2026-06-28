package com.kmp.setplay.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centers content horizontally and caps it at [maxWidth].
 *
 * On Android the screen is narrow so this is effectively a no-op.
 * On web it prevents content from stretching across the full viewport,
 * keeping forms and lists at a readable width regardless of window size.
 *
 * Use inside a Scaffold's content lambda, after consuming innerPadding:
 *
 *   Scaffold { innerPadding ->
 *       ContentContainer(modifier = Modifier.padding(innerPadding)) {
 *           // your screen content
 *       }
 *   }
 */
@Composable
fun ContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 600.dp,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}