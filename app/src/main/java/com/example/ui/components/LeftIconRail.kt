package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavDestination
import com.example.ui.theme.*

private data class RailItem(
    val dest: NavDestination,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun LeftIconRail(
    currentNav: NavDestination,
    unreadMailCount: Int,
    unreadAlertsCount: Int,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        RailItem(NavDestination.HOME, Icons.Filled.Home, Icons.Outlined.Home),
        RailItem(NavDestination.EXPLORER, Icons.Filled.Search, Icons.Outlined.Search),
        RailItem(NavDestination.WORLD_MAP, Icons.Filled.Public, Icons.Outlined.Public),
        RailItem(NavDestination.ECONOMY, Icons.Filled.AttachMoney, Icons.Outlined.AttachMoney),
        RailItem(NavDestination.ORGANIZATIONS, Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
        RailItem(NavDestination.CONFLICT, Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
        RailItem(NavDestination.INBOX, Icons.Filled.Email, Icons.Outlined.Email, unreadMailCount),
        RailItem(NavDestination.NOTIFICATIONS, Icons.Filled.Notifications, Icons.Outlined.Notifications, unreadAlertsCount),
        RailItem(NavDestination.AWARDS, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
        RailItem(NavDestination.PERSONAL, Icons.Filled.Badge, Icons.Outlined.Badge),
        RailItem(NavDestination.PROFILE, Icons.Filled.Person, Icons.Outlined.Person),
        RailItem(NavDestination.MORE, Icons.Filled.AccountTree, Icons.Outlined.AccountTree)
    )

    Column(
        modifier = modifier
            .width(54.dp)
            .fillMaxHeight()
            .background(OledRail)
            .border(width = 1.dp, color = OledBorder)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Top 'W' World Order Monogram Logo
        Box(
            modifier = Modifier
                .testTag("rail_logo_home")
                .size(44.dp)
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(OledCard)
                .border(1.dp, OledBorderHighlight, RoundedCornerShape(10.dp))
                .clickable { onSelect(NavDestination.HOME) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "W",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(4.dp))

        items.forEach { item ->
            val isSelected = currentNav == item.dest || (
                item.dest == NavDestination.MORE && currentNav in listOf(
                    NavDestination.UNIVERSITY, NavDestination.KNOWLEDGE, NavDestination.RANKING,
                    NavDestination.SUPRANATIONAL, NavDestination.MARKET, NavDestination.RULES,
                    NavDestination.SOCIETY, NavDestination.ADMINISTRATION, NavDestination.TUTORIAL,
                    NavDestination.SETTINGS
                )
            )
            Box(
                modifier = Modifier
                    .testTag("rail_${item.dest.name.lowercase()}")
                    .size(46.dp)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFF1A1F24) else OledRail)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) AccentGreen.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(item.dest) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.dest.label,
                    tint = if (isSelected) TextPrimary else TextSecondary,
                    modifier = Modifier.size(21.dp)
                )

                if (item.badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(AccentRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.badgeCount.coerceAtMost(9).toString(),
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

