package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.GameState
import com.example.ui.components.OledBadge
import com.example.ui.components.OledCard
import com.example.ui.components.WorldOrderHubHeader
import com.example.ui.theme.*

@Composable
fun SocietyScreen(state: GameState) {
    val home = state.countries.first { it.iso == state.player.countryIso }
    val localProvs = remember(state.provinces, home.iso) {
        state.provinces.filter { it.countryIso == home.iso }
    }
    val totalPopM = localProvs.sumOf { it.populationMillions }.coerceAtLeast(0.5)
    val totalPops = localProvs.sumOf { it.localPopsCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        WorldOrderHubHeader(
            title = "Society",
            quote = "\"The wealth of a nation rests in the productivity and stability of its citizens.\""
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OledCard(borderColor = AccentGreen.copy(alpha = 0.5f)) {
                    Text("${home.name} National Demographics", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Population", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("${String.format("%.1fM", totalPopM)}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        }
                        Column {
                            Text("Active Pops", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("$totalPops Cohorts", style = MaterialTheme.typography.titleSmall, color = AccentCyan)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Stability", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("${home.stability}%", style = MaterialTheme.typography.titleSmall, color = if (home.stability >= 60) AccentGreen else AccentRed)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (home.stability / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = if (home.stability >= 60) AccentGreen else AccentRed,
                        trackColor = OledElevated
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (home.stability < 45) "WARNING: High unrest & labor strikes impacting retail consumption." else "Civic Order: Steady White/Black market consumption across provinces.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (home.stability < 45) AccentRed else TextDescription
                    )
                }
            }

            item { Text("Regional Pop Consumption & Grid Tiers", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
            items(localProvs, key = { it.id }) { prov ->
                OledCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${prov.name} (${prov.capitalCity})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Pop: ${String.format("%.2fM", prov.populationMillions)} • Pops: ${prov.localPopsCount}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                        }
                        OledBadge("${prov.powerSupplyMw}/${prov.powerDemandMw} MW", if (prov.powerSupplyMw >= prov.powerDemandMw) AccentGreen else AccentRed)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Weekly Demand Index: Legal ${String.format("%.2fx", prov.whiteMarketIndex)} • Shadow ${String.format("%.2fx", prov.blackMarketPremium)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGold
                    )
                }
            }
        }
    }
}

@Composable
fun TutorialScreen() {
    val chapters = listOf(
        "1. Career Start & Daily Work" to "Begin at 0.00 cash in Career Mode (or Head of State in Sandbox). Log daily Work Discipline to build your Streak (Racha), train single-slot stats, and spend 400 daily Energy at Universidad.",
        "2. LinkedOut & Founding Corporations" to "Apply for jobs in Organizations -> Search for jobs (LinkedOut). Save capital to incorporate Farms, Mines, Fabs, Commercial Banks, Media, or Power Plants in Economy -> B2B platform.",
        "3. Stocks, >50% Control & Finance" to "Trade real equities, sovereign bonds, bilateral debt, forex, and leveraged derivatives. Buying >50% of any company's shares grants majority board control.",
        "4. Parliament, Laws & Constitutional Reform" to "Enter Parliament via elections or party leadership. Debate and vote on 46 real-world laws and 10 Constitutional Chapters in Rules & Administration.",
        "5. Strict Portfolio-Tag Isolation" to "National controls like the Central Bank (M0/M1/Rates/SWIFT), Covert Intelligence, Armed Forces, and Central Planning Committee unlock strictly for appointed officeholders.",
        "6. Real-Time Speed & 5-Slot Saves" to "Adjust time speed (||, 1x, 2x, 5x, 10x) in the top clock bar and persist your campaign anytime across 5 local offline save slots."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        WorldOrderHubHeader(
            title = "Tutorial",
            quote = "\"Knowledge is power.\" - World Order Offline Grand Strategy Field Manual"
        )
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chapters, key = { it.first }) { (title, body) ->
                OledCard {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
                    Spacer(Modifier.height(4.dp))
                    Text(body, style = MaterialTheme.typography.bodySmall, color = TextDescription)
                }
            }
        }
    }
}
