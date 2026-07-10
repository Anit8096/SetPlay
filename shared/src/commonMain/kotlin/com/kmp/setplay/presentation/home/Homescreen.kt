package com.kmp.setplay.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.presentation.auth.LinkAccountBanner
import com.kmp.setplay.presentation.common.ContentContainer

@Composable
fun HomeScreen(
    onFormatSelected: (BracketFormat) -> Unit,
    isAnonymous: Boolean = false,
    onLinkGoogle: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    ContentContainer(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isAnonymous) {
                item {
                    LinkAccountBanner(onLinkGoogle = onLinkGoogle)
                }
            }

            item {
                Text(
                    "Choose tournament type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(
                    modifier = Modifier.size(4.dp)
                )
            }

            BracketFormat.entries.forEach { format ->
                item(key = format.name) {
                    FormatCard(
                        format = format,
                        enabled = format == BracketFormat.SINGLE_ELIMINATION,
                        onClick = { if (format == BracketFormat.SINGLE_ELIMINATION) onFormatSelected(format) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatCard(
    format: BracketFormat,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.outlineVariant
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Icon(
                imageVector = format.icon(),
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    format.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
                Text(
                    if (enabled) format.description() else "Coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.4f
                    )
                )
            }
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun BracketFormat.displayName() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "Single Elimination"
    BracketFormat.DOUBLE_ELIMINATION   -> "Double Elimination"
    BracketFormat.ROUND_ROBIN          -> "Round Robin"
    BracketFormat.SWISS                -> "Swiss"
    BracketFormat.LEAGUE               -> "League"
    BracketFormat.THREE_GAME_GUARANTEE -> "3-Game Guarantee"
}

private fun BracketFormat.icon() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> Icons.Filled.AccountTree
    BracketFormat.DOUBLE_ELIMINATION   -> Icons.Filled.SwapVert
    BracketFormat.ROUND_ROBIN          -> Icons.Filled.Hub
    BracketFormat.SWISS                -> Icons.Filled.GridView
    BracketFormat.LEAGUE               -> Icons.Filled.EmojiEvents
    BracketFormat.THREE_GAME_GUARANTEE -> Icons.Filled.Autorenew
}

private fun BracketFormat.description() = when (this) {
    BracketFormat.SINGLE_ELIMINATION   -> "One loss and you're out"
    BracketFormat.DOUBLE_ELIMINATION   -> "Two losses before you're eliminated"
    BracketFormat.ROUND_ROBIN          -> "Everyone plays everyone once"
    BracketFormat.SWISS                -> "Matched by similar win-loss record"
    BracketFormat.LEAGUE               -> "Full season with recurring matches"
    BracketFormat.THREE_GAME_GUARANTEE -> "Every team plays at least 3 games"
}