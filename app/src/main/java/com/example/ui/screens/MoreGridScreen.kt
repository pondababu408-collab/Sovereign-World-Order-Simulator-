package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.NavDestination
import com.example.ui.components.OledBadge
import com.example.ui.components.OledCard
import com.example.ui.theme.*

private data class MoreTile(
    val title: String,
    val dest: NavDestination,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun MoreGridScreen(
    state: GameState,
    onNavigate: (NavDestination) -> Unit,
    onOpenNewCampaign: () -> Unit
) {
    val unreadAlerts = state.notifications.count { !it.isRead }
    val tiles = listOf(
        MoreTile("Economy", NavDestination.ECONOMY, Icons.Filled.AttachMoney, AccentGreen),
        MoreTile("Organizations", NavDestination.ORGANIZATIONS, Icons.Filled.AccountBalance, AccentGold),
        MoreTile("Conflict", NavDestination.CONFLICT, Icons.Filled.Shield, AccentRed),
        MoreTile("Personal", NavDestination.PROFILE, Icons.Filled.Person, AccentCyan),
        MoreTile("Knowledge", NavDestination.KNOWLEDGE, Icons.Filled.Psychology, AccentPurple),
        MoreTile("University", NavDestination.UNIVERSITY, Icons.Filled.School, AccentCyan),
        MoreTile("Ranking", NavDestination.RANKING, Icons.Filled.Leaderboard, AccentGold),
        MoreTile("Supranational Orgs", NavDestination.SUPRANATIONAL, Icons.Filled.Public, AccentCyan),
        MoreTile("Messages", NavDestination.INBOX, Icons.Filled.Email, AccentGold),
        MoreTile("Market", NavDestination.MARKET, Icons.Filled.Storefront, AccentBitcoin),
        MoreTile("Laws & Norms", NavDestination.LAWS, Icons.Filled.Gavel, AccentPurple),
        MoreTile("Society", NavDestination.SOCIETY, Icons.Filled.Groups, AccentGreen),
        MoreTile("Administration", NavDestination.ADMINISTRATION, Icons.Filled.AdminPanelSettings, AccentCyan),
        MoreTile("Settings", NavDestination.SETTINGS, Icons.Filled.Settings, TextSecondary)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Full-width top Notifications banner
        OledCard(
            borderColor = AccentCyan.copy(alpha = 0.5f),
            onClick = { onNavigate(NavDestination.NOTIFICATIONS) }
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = AccentCyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifications & Live Dispatches", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(state.notifications.firstOrNull()?.title ?: "No new alerts", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (unreadAlerts > 0) OledBadge("$unreadAlerts NEW", AccentRed)
            }
        }

        Spacer(Modifier.height(10.dp))

        // 4-Column Navigation Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tiles, key = { it.title }) { tile ->
                Column(
                    modifier = Modifier
                        .aspectRatio(0.95f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OledCard)
                        .border(1.dp, OledBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            if (tile.dest == NavDestination.SETTINGS) {
                                onOpenNewCampaign()
                            } else {
                                onNavigate(tile.dest)
                            }
                        }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tile.icon, contentDescription = tile.title, tint = tile.accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
