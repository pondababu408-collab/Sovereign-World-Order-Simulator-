package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.model.SocialPost
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SocialFeedScreen(
    state: GameState,
    onCreatePost: (String, List<String>, String?) -> Unit,
    onLikePost: (String) -> Unit,
    onRepostPost: (String) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onAddReply: (String, String) -> Unit,
    onInspectNpc: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // For You, Following, National, Global
    val tabs = listOf("For You", "Following", "National", "Global")

    var composeText by remember { mutableStateOf("") }
    var showPollBuilder by remember { mutableStateOf(false) }
    var pollOpt1 by remember { mutableStateOf("") }
    var pollOpt2 by remember { mutableStateOf("") }
    var attachChart by remember { mutableStateOf(false) }
    var activeReplyPostId by remember { mutableStateOf<String?>(null) }
    var replyDraft by remember { mutableStateOf("") }

    val filteredPosts = remember(state.socialPosts, selectedTab, state.player.countryIso, state.player.followingNpcIds) {
        when (selectedTab) {
            1 -> state.socialPosts.filter { it.authorId in state.player.followingNpcIds || it.authorId == state.player.id }
            2 -> state.socialPosts.filter { it.countryIso == state.player.countryIso }
            else -> state.socialPosts
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Live Macro Event Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(OledElevated)
                .border(1.dp, OledBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Campaign, contentDescription = "Live Dispatch", tint = AccentGold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.lastEventBanner,
                style = MaterialTheme.typography.bodySmall,
                color = AccentGold,
                maxLines = 2
            )
        }

        Spacer(Modifier.height(8.dp))

        // Timeline Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(OledCard)
                .border(1.dp, OledBorder, RoundedCornerShape(8.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, title ->
                val active = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("feed_tab_${title.lowercase()}")
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) AccentCyan.copy(alpha = 0.16f) else OledCard)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (active) AccentCyan else TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compose Card
            item {
                OledCard {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExecutivePortrait(seed = state.player.avatarSeed, name = state.player.name, size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = composeText,
                                onValueChange = { composeText = it },
                                placeholder = { Text("Publish a political statement, tag @username, or launch an Encuesta...", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth().testTag("compose_post_input"),
                                minLines = 2,
                                maxLines = 4
                            )
                            if (showPollBuilder) {
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = pollOpt1,
                                    onValueChange = { pollOpt1 = it },
                                    label = { Text("Poll Option 1") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = pollOpt2,
                                    onValueChange = { pollOpt2 = it },
                                    label = { Text("Poll Option 2") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = showPollBuilder,
                                        onClick = { showPollBuilder = !showPollBuilder },
                                        label = { Text("Encuesta", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Filled.Poll, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )
                                    FilterChip(
                                        selected = attachChart,
                                        onClick = { attachChart = !attachChart },
                                        label = { Text("Chart", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                                OledPrimaryButton(
                                    text = "Post",
                                    onClick = {
                                        val opts = if (showPollBuilder) listOf(pollOpt1, pollOpt2) else emptyList()
                                        onCreatePost(composeText, opts, if (attachChart) "CHART:BTC" else null)
                                        composeText = ""
                                        pollOpt1 = ""
                                        pollOpt2 = ""
                                        showPollBuilder = false
                                        attachChart = false
                                    },
                                    enabled = composeText.isNotBlank(),
                                    modifier = Modifier.testTag("publish_post_button")
                                )
                            }
                        }
                    }
                }
            }

            if (filteredPosts.isEmpty()) {
                item {
                    OledCard {
                        Text(
                            text = "No dispatches in this feed yet. Follow citizens in Explorer or publish a statement above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            items(filteredPosts, key = { it.id }) { post ->
                SocialPostCard(
                    post = post,
                    state = state,
                    isReplying = activeReplyPostId == post.id,
                    replyDraft = replyDraft,
                    onReplyDraftChange = { replyDraft = it },
                    onToggleReply = {
                        activeReplyPostId = if (activeReplyPostId == post.id) null else post.id
                        replyDraft = ""
                    },
                    onSendReply = {
                        onAddReply(post.id, replyDraft)
                        replyDraft = ""
                        activeReplyPostId = null
                    },
                    onLike = { onLikePost(post.id) },
                    onRepost = { onRepostPost(post.id) },
                    onVotePoll = { optId -> onVotePoll(post.id, optId) },
                    onInspectAuthor = {
                        if (post.authorId != state.player.id) onInspectNpc(post.authorId)
                    }
                )
            }
        }
    }
}

@Composable
private fun SocialPostCard(
    post: SocialPost,
    state: GameState,
    isReplying: Boolean,
    replyDraft: String,
    onReplyDraftChange: (String) -> Unit,
    onToggleReply: () -> Unit,
    onSendReply: () -> Unit,
    onLike: () -> Unit,
    onRepost: () -> Unit,
    onVotePoll: (String) -> Unit,
    onInspectAuthor: () -> Unit
) {
    OledCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExecutivePortrait(
                seed = post.authorId.hashCode(),
                name = post.authorName,
                size = 38.dp,
                modifier = Modifier.clickable { onInspectAuthor() }
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).clickable { onInspectAuthor() }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(post.authorName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    post.authorBadge?.let {
                        OledBadge(it, if (it == "HEAD OF STATE") AccentGold else AccentCyan)
                    }
                }
                Text("@${post.authorHandle} • ${post.countryIso} • ${post.timestampStr}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(post.content, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)

        // Optional Chart Attachment
        if (post.attachmentTag != null && post.attachmentTag.startsWith("CHART:")) {
            val ticker = post.attachmentTag.removePrefix("CHART:")
            val asset = state.stocks.firstOrNull { it.ticker == ticker } ?: state.stocks.firstOrNull()
            if (asset != null) {
                Spacer(Modifier.height(8.dp))
                Text("${asset.name} ($${asset.ticker}) • ${formatUsd(asset.currentPriceUsd)}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                Spacer(Modifier.height(4.dp))
                StockChartCanvas(candles = asset.candles, modifier = Modifier.height(100.dp))
            }
        }

        // Interactive Live Poll (Encuesta)
        if (post.pollOptions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val totalVotes = post.pollOptions.sumOf { it.votes }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                post.pollOptions.forEach { opt ->
                    val pct = (opt.votes * 100f / totalVotes).toInt()
                    val votedThis = post.playerVotedOptionId == opt.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(OledElevated)
                            .border(1.dp, if (votedThis) AccentCyan else OledBorder, RoundedCornerShape(6.dp))
                            .clickable(enabled = post.playerVotedOptionId == null) { onVotePoll(opt.id) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (pct / 100f).coerceIn(0.03f, 1f))
                                .matchParentSize()
                                .background((if (votedThis) AccentCyan else AccentPurple).copy(alpha = 0.18f))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(opt.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text("$pct% (${opt.votes})", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        }
                    }
                }
                Text("Live AI Citizens Voting • $totalVotes total votes", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onToggleReply() }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Reply", tint = TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${post.replies.size}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Row(
                modifier = Modifier.clickable { onRepost() }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Repeat, contentDescription = "Repost", tint = if (post.repostedByPlayer) AccentGreen else TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${post.reposts}", style = MaterialTheme.typography.labelSmall, color = if (post.repostedByPlayer) AccentGreen else TextSecondary)
            }
            Row(
                modifier = Modifier.clickable { onLike() }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (post.likedByPlayer) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (post.likedByPlayer) AccentRed else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("${post.likes}", style = MaterialTheme.typography.labelSmall, color = if (post.likedByPlayer) AccentRed else TextSecondary)
            }
        }

        if (post.replies.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(OledElevated)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                post.replies.forEach { rep ->
                    Text("@${rep.authorHandle}: ${rep.text}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        if (isReplying) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = replyDraft,
                    onValueChange = onReplyDraftChange,
                    placeholder = { Text("Write reply...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OledPrimaryButton(text = "Send", onClick = onSendReply, enabled = replyDraft.isNotBlank())
            }
        }
    }
}
