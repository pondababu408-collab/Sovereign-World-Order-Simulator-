package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ProfileScreen(
    state: GameState,
    onClaimWorkDiscipline: () -> Unit,
    onStartTraining: (StatType) -> Unit,
    onOpenUniversity: () -> Unit,
    onOpenKnowledge: () -> Unit
) {
    val p = state.player
    val home = state.countries.firstOrNull { it.iso == p.countryIso }
    val party = state.parties.firstOrNull { it.id == p.partyId }
    val workedToday = p.lastWorkDay == state.date.totalDays()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cover Banner & Executive Identity
        item {
            OledCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentCyan.copy(alpha = 0.28f), AccentGold.copy(alpha = 0.18f), OledElevated)
                            )
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    OledBadge("${home?.name ?: p.countryIso} • ${p.currentRegionId}", AccentGold)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExecutivePortrait(seed = p.avatarSeed, name = p.name, size = 58.dp, accent = AccentGold)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text("@${p.handle} • Lv.${p.level} • ${p.araucitas} Araucitas", style = MaterialTheme.typography.labelMedium, color = AccentCyan)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val xpRatio = (p.xp.toFloat() / p.xpToNext.coerceAtLeast(1)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { xpRatio },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = AccentCyan,
                    trackColor = OledElevated
                )
                Spacer(Modifier.height(4.dp))
                Text("XP ${p.xp} / ${p.xpToNext} (+2 Araucitas on Level-Up)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        // Daily Work Discipline (Disciplina de trabajo) & Streak (Racha)
        item {
            OledCard(borderColor = AccentGold.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Disciplina de trabajo (Work Discipline)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Racha (Streak): ${p.workStreak} days • Multiplier ${String.format("%.2fx", 1.0 + p.workStreak * 0.25)}", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                    }
                    OledPrimaryButton(
                        text = if (workedToday) "Done Today" else "Log Work",
                        enabled = !workedToday,
                        onClick = onClaimWorkDiscipline,
                        modifier = Modifier.testTag("work_discipline_button"),
                        accent = AccentGold
                    )
                }
            }
        }

        // Single-Slot Stat Training
        item {
            OledCard {
                OledSectionHeader("Single-Slot Stat Training", "Train one attribute at a time with real-time countdown", Icons.Filled.FitnessCenter, AccentCyan)
                Spacer(Modifier.height(8.dp))
                p.activeTraining?.let { job ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentCyan.copy(alpha = 0.14f))
                            .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Training ${job.stat.displayName}...", style = MaterialTheme.typography.titleSmall, color = AccentCyan)
                            Text("${job.remainingSeconds}s / ${job.totalSeconds}s", style = MaterialTheme.typography.labelMedium, color = AccentGold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTrainBox("INT", p.intelligence, StatType.INTELLIGENCE, p.activeTraining == null, onStartTraining, Modifier.weight(1f))
                    StatTrainBox("STR", p.strength, StatType.STRENGTH, p.activeTraining == null, onStartTraining, Modifier.weight(1f))
                    StatTrainBox("END", p.endurance, StatType.ENDURANCE, p.activeTraining == null, onStartTraining, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Efficiency: ${p.efficiency}", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
                    Text("Charisma: ${p.charisma}", style = MaterialTheme.typography.labelMedium, color = AccentPurple)
                    Text("Energy: ${p.energy}/${p.maxEnergy}", style = MaterialTheme.typography.labelMedium, color = AccentGold)
                }
            }
        }

        // Quick Academic Launchers
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OledPrimaryButton("Universidad (${p.energy}E)", onOpenUniversity, Modifier.weight(1f), accent = AccentCyan)
                OledOutlinedButton("Knowledge Tree", onOpenKnowledge, Modifier.weight(1f), accent = AccentPurple)
            }
        }

        // Attributes, Ideologies, Offices, Party & Noble Titles
        item {
            OledCard {
                Text("Attributes & Ideologies", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Culture: ${p.attributes.culture} • Belief: ${p.attributes.belief} • Gender: ${p.attributes.gender}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Ideology: ${p.ideology.primary} / ${p.ideology.secondary}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                Text("System: ${p.ideology.economicSystem.label} • ${p.ideology.formOfGovernment.label} • ${p.ideology.powerStructure.label}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Text("Offices Held (Cargos)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(
                    text = if (p.officesHeld.isEmpty()) "Private Citizen" + if (p.isParliamentMember) " + Member of Parliament" else "" else p.officesHeld.joinToString(", ") { it.title },
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGold
                )
                Spacer(Modifier.height(6.dp))
                Text("Political Party: ${party?.name ?: "Independent"}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Noble Titles: ${if (p.nobleTitles.isEmpty()) "None" else p.nobleTitles.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = AccentPurple)
            }
        }
    }
}

@Composable
private fun StatTrainBox(
    code: String,
    value: Int,
    stat: StatType,
    canTrain: Boolean,
    onTrain: (StatType) -> Unit,
    modifier: Modifier
) {
    OledCard(modifier = modifier, containerColor = OledElevated) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(code, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("$value", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            OledOutlinedButton("Train", { onTrain(stat) }, enabled = canTrain, accent = AccentCyan)
        }
    }
}

@Composable
fun UniversityScreen(
    state: GameState,
    onStudy: (UniversityFaculty) -> Unit,
    onBoostAraucitas: (UniversityFaculty) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OledSectionHeader(
                title = "Universidad (400 Daily Energy)",
                subtitle = "Energy: ${state.player.energy}/400 • Araucitas: ${state.player.araucitas}",
                icon = Icons.Filled.School,
                accent = AccentCyan
            )
        }
        items(UniversityFaculty.entries, key = { it.name }) { fac ->
            val lvl = state.player.facultyLevels[fac.name] ?: 1
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(fac.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            OledBadge("Lv.$lvl", AccentCyan)
                        }
                        Text("50 Energy → +40 XP & Core Stats", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OledPrimaryButton("Study", { onStudy(fac) }, enabled = state.player.energy >= 50)
                        OledOutlinedButton("⚡", { onBoostAraucitas(fac) }, enabled = state.player.araucitas >= 1, accent = AccentPurple)
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgeTreeScreen(
    state: GameState,
    onStartResearch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OledSectionHeader(
                title = "Conocimiento (Knowledge Tree)",
                subtitle = "20-Day Masteries & 15-Day Repeatable Specializations",
                icon = Icons.Filled.Psychology,
                accent = AccentPurple
            )
        }

        state.player.activeResearch?.let { res ->
            item {
                OledCard(borderColor = AccentPurple) {
                    val node = state.knowledgeNodes.firstOrNull { it.id == res.nodeId }
                    Text("Researching: ${node?.title ?: res.nodeId}", style = MaterialTheme.typography.titleSmall, color = AccentPurple)
                    Text("${res.remainingHours / 24}d ${res.remainingHours % 24}h remaining", style = MaterialTheme.typography.labelMedium, color = AccentGold)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 1f - (res.remainingHours.toFloat() / res.totalHours.coerceAtLeast(1)) },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = AccentPurple,
                        trackColor = OledElevated
                    )
                }
            }
        }

        items(state.knowledgeNodes, key = { it.id }) { node ->
            val unlocked = state.player.level >= node.minLevel
            val completed = !node.isRepeatable && node.completionCount > 0
            OledCard(borderColor = if (completed) AccentGreen.copy(alpha = 0.5f) else OledBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(node.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            OledBadge(if (node.isRepeatable) "15d REPEAT x${node.completionCount}" else "20d MASTERY", if (node.isRepeatable) AccentCyan else AccentGold)
                        }
                        Text(node.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("${node.bonusSummary} • Min Lv.${node.minLevel}", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                    }
                    OledPrimaryButton(
                        text = if (completed) "Mastered" else "Research",
                        enabled = unlocked && !completed && state.player.activeResearch == null,
                        onClick = { onStartResearch(node.id) },
                        accent = AccentPurple
                    )
                }
            }
        }
    }
}
