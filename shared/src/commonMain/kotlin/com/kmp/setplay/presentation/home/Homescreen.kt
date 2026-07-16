package com.kmp.setplay.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.presentation.auth.LinkAccountBanner
import com.kmp.setplay.presentation.common.ContentContainer

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    "Pick a format to start a new tournament",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(12.dp))
            }


            item {
                val formats = BracketFormat.entries
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    formats.forEachIndexed { index, format ->
                        val enabled = format == BracketFormat.SINGLE_ELIMINATION
                        SegmentedListItem(
                            onClick = { onFormatSelected(format) },
                            shapes = ListItemDefaults.segmentedShapes(index = index, count = formats.size),
                            enabled = enabled,
                            colors = ListItemDefaults.segmentedColors(
                                leadingContentColor = MaterialTheme.colorScheme.primary
                            ),
                            leadingContent = {
                                Icon(
                                    imageVector = format.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            supportingContent = {
                                Text(if (enabled) format.description() else "Coming soon")
                            },
                            trailingContent = if (enabled) {
                                {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else null
                        ) {
                            Text(format.displayName(), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
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
