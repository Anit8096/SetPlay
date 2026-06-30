package com.kmp.setplay.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.kmp.setplay.domain.model.BracketFormat
import com.kmp.setplay.presentation.browse.BrowseScreen
import com.kmp.setplay.presentation.history.HistoryScreen
import com.kmp.setplay.presentation.home.HomeScreen
import com.kmp.setplay.presentation.profile.ProfileScreen

private enum class Tab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    BROWSE(Icons.Filled.Search, Icons.Outlined.Search, "Browse"),
    HISTORY(Icons.Filled.History, Icons.Outlined.History, "History"),
    PROFILE(Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle, "Profile"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    onFormatSelected: (BracketFormat) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }

    val topBarTitle = when (selectedTab) {
        Tab.HOME    -> "SetPlay"
        Tab.BROWSE  -> "Browse"
        Tab.HISTORY -> "History"
        Tab.PROFILE -> "Profile"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        topBarTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == tab) tab.selectedIcon
                                else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            Tab.HOME    -> HomeScreen(
                onFormatSelected = onFormatSelected,
                contentPadding = innerPadding
            )
            Tab.BROWSE  -> BrowseScreen(contentPadding = innerPadding)
            Tab.HISTORY -> HistoryScreen(contentPadding = innerPadding)
            Tab.PROFILE -> ProfileScreen(contentPadding = innerPadding)
        }
    }
}