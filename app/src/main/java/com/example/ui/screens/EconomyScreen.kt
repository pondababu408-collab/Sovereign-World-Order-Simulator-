package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun EconomyScreen(
    state: GameState,
    initialTab: Int = 0, // 0: LinkedOut, 1: Businesses, 2: Markets, 3: Stocks, 4: Grid, 5: Central Bank, 6: Central Planning
    onApplyJob: (String) -> Unit,
    onFoundCompany: (String, BusinessType, PowerFuel?) -> Unit,
    onHireNpc: (String, String) -> Unit,
    onBuyStock: (String, Long) -> Unit,
    onSellStock: (String, Long) -> Unit,
    onSellCommodity: (CommodityGood, Int, Boolean, Boolean) -> Unit,
    onCreateContract: (String, CommodityGood, Int, Double, Boolean) -> Unit,
    onBuildSubstationOrUpgrade: (String) -> Unit,
    onBuildTransmission: (String, String, String) -> Unit,
    onUpdateCentralBank: (Double, Double, Double, Boolean) -> Unit,
    onUpdatePlanning: (Int, Double, Double) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("LinkedOut", "Businesses", "Markets", "Stocks", "Grid", "Central Bank", "Planning")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        OledSectionHeader(
            title = "worldorder.app/economy",
            subtitle = "Cash: ${formatUsd(state.player.cashUsd)} • ₿ ${String.format("%.4f", state.player.bitcoinBalance)}",
            icon = Icons.Filled.AttachMoney,
            accent = AccentGreen
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Box(
                    modifier = Modifier
                        .testTag("econ_tab_${label.lowercase().replace(" ", "_")}")
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) AccentCyan.copy(alpha = 0.16f) else OledCard)
                        .border(1.dp, if (active) AccentCyan else OledBorder, RoundedCornerShape(6.dp))
                        .clickable { selectedTab = idx }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Normal),
                        color = if (active) AccentCyan else TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> LinkedOutJobsTab(state, onApplyJob)
                1 -> BusinessesTab(state, onFoundCompany, onHireNpc)
                2 -> MarketsAndContractsTab(state, onSellCommodity, onCreateContract)
                3 -> StockMarketTab(state, onBuyStock, onSellStock)
                4 -> ElectricalGridTab(state, onBuildSubstationOrUpgrade, onBuildTransmission)
                5 -> CentralBankTab(state, onUpdateCentralBank)
                6 -> CentralPlanningTab(state, onUpdatePlanning)
            }
        }
    }
}

@Composable
private fun LinkedOutJobsTab(state: GameState, onApplyJob: (String) -> Unit) {
    val homeJobs = state.jobListings.filter { it.countryIso == state.player.countryIso }
    val globalJobs = state.jobListings.filter { it.countryIso != state.player.countryIso }.take(20)
    val workerScore = (state.player.efficiency + state.player.intelligence) / 2.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                OledSectionHeader("LinkedOut Career Network", "Daily salary scales with Efficiency (${state.player.efficiency}) & Intelligence (${state.player.intelligence})", Icons.Filled.Work)
                Spacer(Modifier.height(4.dp))
                val activeJob = state.jobListings.firstOrNull { it.id == state.player.currentJobId }
                if (activeJob != null) {
                    val estPay = activeJob.salaryPerPointUsd * workerScore
                    Text("Current Role: ${activeJob.roleTitle} at ${activeJob.companyName}", style = MaterialTheme.typography.titleSmall, color = AccentGreen)
                    Text("Estimated Daily Pay: ${formatUsd(estPay)}/day", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                } else {
                    Text("Unemployed • Starting at 0.00? Apply to any position below to earn daily income.", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                }
            }
        }
        item { Text("Domestic Positions (${state.player.countryIso})", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
        items(homeJobs, key = { it.id }) { job ->
            JobRow(job, workerScore, state.player.currentJobId == job.id, onApplyJob)
        }
        item { Text("International Positions", style = MaterialTheme.typography.titleSmall, color = TextSecondary) }
        items(globalJobs, key = { it.id }) { job ->
            JobRow(job, workerScore, state.player.currentJobId == job.id, onApplyJob)
        }
    }
}

@Composable
private fun JobRow(job: JobListing, workerScore: Double, isCurrent: Boolean, onApply: (String) -> Unit) {
    val dailyPay = job.salaryPerPointUsd * workerScore
    OledCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(job.roleTitle, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (job.isStateJob) OledBadge("SOE", AccentGold)
                }
                Text("${job.companyName} (${job.countryIso})", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Pay: $${String.format("%.2f", job.salaryPerPointUsd)}/pt (~${formatUsd(dailyPay)}/day) • Min INT ${job.minIntelligence}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
            }
            OledPrimaryButton(
                text = if (isCurrent) "Active" else "Apply",
                enabled = !isCurrent,
                onClick = { onApply(job.id) },
                accent = if (isCurrent) AccentGreen else AccentCyan
            )
        }
    }
}

@Composable
private fun BusinessesTab(
    state: GameState,
    onFoundCompany: (String, BusinessType, PowerFuel?) -> Unit,
    onHireNpc: (String, String) -> Unit
) {
    var bizName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BusinessType.FARM) }
    var selectedFuel by remember { mutableStateOf(PowerFuel.SOLAR) }
    var hireTargetCompanyId by remember { mutableStateOf<String?>(null) }

    val playerCompanies = state.companies.filter { it.ownerId == state.player.id }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard {
                OledSectionHeader("Found New Enterprise", "Start Farms, Mines, Fabs, Banks, Media or Power Plants", Icons.Filled.Business, AccentGold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bizName,
                    onValueChange = { bizName = it },
                    label = { Text("Corporation Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BusinessType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text("${t.displayName} (${formatUsd(t.baseCostUsd)})", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (selectedType == BusinessType.POWER_PLANT) {
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PowerFuel.entries.forEach { f ->
                            FilterChip(
                                selected = selectedFuel == f,
                                onClick = { selectedFuel = f },
                                label = { Text(f.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OledPrimaryButton(
                    text = "Incorporate (${formatUsd(selectedType.baseCostUsd)})",
                    onClick = {
                        onFoundCompany(bizName, selectedType, selectedFuel)
                        bizName = ""
                    },
                    enabled = bizName.isNotBlank() && state.player.cashUsd >= selectedType.baseCostUsd,
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentGold
                )
            }
        }

        item { Text("Your Portfolio Companies (${playerCompanies.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
        items(playerCompanies, key = { it.id }) { comp ->
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CorporateLogo(comp.ticker)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${comp.name} [${comp.ticker}]", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${comp.type.displayName} • Eff ${comp.efficiency} • ${comp.employeesCount} Staff", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Valuation: ${formatUsd(comp.valuationUsd)} • Daily Profit: ${formatUsd(comp.dailyProfitUsd)}", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                    }
                    OledOutlinedButton("Hire NPC", { hireTargetCompanyId = comp.id })
                }
            }
        }
    }

    if (hireTargetCompanyId != null) {
        val candidates = state.npcs.filter { it.countryIso == state.player.countryIso && it.jobCompanyId == null }.take(15)
        AlertDialog(
            onDismissRequest = { hireTargetCompanyId = null },
            containerColor = OledCard,
            title = { Text("Recruit High/Low-Trait AI Citizens", style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(candidates, key = { it.id }) { npc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OledElevated)
                                .clickable {
                                    onHireNpc(hireTargetCompanyId!!, npc.id)
                                    hireTargetCompanyId = null
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${npc.name} (${npc.traitTier.name})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("EFF ${npc.efficiency} • INT ${npc.intelligence} • Fee: ${formatUsd(npc.efficiency * 25.0)}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                            }
                            OledBadge("HIRE", AccentGreen)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { hireTargetCompanyId = null }) { Text("Close", color = TextSecondary) } }
        )
    }
}

@Composable
private fun MarketsAndContractsTab(
    state: GameState,
    onSellCommodity: (CommodityGood, Int, Boolean, Boolean) -> Unit,
    onCreateContract: (String, CommodityGood, Int, Double, Boolean) -> Unit
) {
    val prov = state.provinces.firstOrNull { it.id == state.player.currentRegionId }
    var isBlackMarket by remember { mutableStateOf(false) }
    var settleBitcoin by remember { mutableStateOf(true) }
    var targetIso by remember { mutableStateOf("US") }
    var customPriceStr by remember { mutableStateOf("250") }
    var selectedGood by remember { mutableStateOf(CommodityGood.GRAIN) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Regional Market: ${prov?.capitalCity ?: state.player.currentRegionId}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(if (isBlackMarket) "Black Market (${String.format("%.2fx", prov?.blackMarketPremium ?: 1.35)} premium)" else "White Market (${String.format("%.2fx", prov?.whiteMarketIndex ?: 1.0)} legal tender)", style = MaterialTheme.typography.bodySmall, color = if (isBlackMarket) AccentBitcoin else AccentGreen)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !isBlackMarket, onClick = { isBlackMarket = false }, label = { Text("White") })
                        FilterChip(selected = isBlackMarket, onClick = { isBlackMarket = true }, label = { Text("Black (₿)") })
                    }
                }
                if (isBlackMarket) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = settleBitcoin, onCheckedChange = { settleBitcoin = it })
                        Text("Settle in untraceable Bitcoin (BTC)", style = MaterialTheme.typography.bodySmall, color = AccentBitcoin)
                    }
                }
            }
        }

        items(CommodityGood.entries, key = { it.name }) { good ->
            val qty = state.playerInventory[good.name] ?: 0
            val mult = if (isBlackMarket) (prov?.blackMarketPremium ?: 1.35) else (prov?.whiteMarketIndex ?: 1.0)
            val price = good.basePriceUsd * mult
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(good.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Held: $qty units • Price: ${formatUsd(price)}/unit", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                    }
                    OledPrimaryButton(
                        text = "Sell 5",
                        enabled = qty >= 5,
                        onClick = { onSellCommodity(good, 5, isBlackMarket, settleBitcoin) },
                        accent = if (isBlackMarket) AccentBitcoin else AccentGreen
                    )
                }
            }
        }

        item {
            OledCard {
                Text("Negotiate International Trade Contract", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetIso,
                        onValueChange = { targetIso = it.uppercase().take(3) },
                        label = { Text("Partner ISO") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customPriceStr,
                        onValueChange = { customPriceStr = it },
                        label = { Text("Negotiated $/u") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommodityGood.entries.forEach { g ->
                        FilterChip(selected = selectedGood == g, onClick = { selectedGood = g; customPriceStr = g.basePriceUsd.roundToInt().toString() }, label = { Text(g.name, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(Modifier.height(6.dp))
                OledPrimaryButton(
                    text = "Sign 5-Day Contract (10 units)",
                    onClick = {
                        val p = customPriceStr.toDoubleOrNull() ?: selectedGood.basePriceUsd
                        onCreateContract(targetIso, selectedGood, 10, p, isBlackMarket)
                    },
                    enabled = (state.playerInventory[selectedGood.name] ?: 0) >= 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(state.contracts, key = { it.id }) { c ->
            OledCard {
                Text("${c.sellerCountryIso} → ${c.buyerCountryIso}: ${c.quantity} ${c.good.displayName} @ ${formatUsd(c.unitPriceUsd)}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Status: ${c.status} • ${c.activeDaysRemaining}d left • ${if (c.settleInBitcoin) "BTC Settlement" else "Forex Wire"}", style = MaterialTheme.typography.labelSmall, color = AccentGold)
            }
        }
    }
}

@Composable
private fun StockMarketTab(
    state: GameState,
    onBuyStock: (String, Long) -> Unit,
    onSellStock: (String, Long) -> Unit
) {
    var selectedTicker by remember { mutableStateOf(state.stocks.firstOrNull()?.ticker ?: "RELIANCE") }
    val selectedStock = state.stocks.firstOrNull { it.ticker == selectedTicker } ?: state.stocks.first()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CorporateLogo(selectedStock.ticker, size = 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${selectedStock.name} ($${selectedStock.ticker})", style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (selectedStock.hasMajorityControl) OledBadge(">50% CONTROL", AccentGold)
                        }
                        Text("${selectedStock.sector} • Div ${selectedStock.dividendYieldPct}% • Held: ${selectedStock.playerShares} (${String.format("%.2f%%", selectedStock.playerOwnershipPct)})", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatUsd(selectedStock.currentPriceUsd), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        val chg = selectedStock.changePct
                        Text("${if (chg >= 0) "+" else ""}${String.format("%.2f%%", chg)}", style = MaterialTheme.typography.labelSmall, color = if (chg >= 0) AccentGreen else AccentRed)
                    }
                }
                Spacer(Modifier.height(8.dp))
                StockChartCanvas(candles = selectedStock.candles)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order Book Bid: ${formatUsd(selectedStock.bidPriceUsd)}", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                    Text("Order Book Ask: ${formatUsd(selectedStock.askPriceUsd)}", style = MaterialTheme.typography.labelSmall, color = AccentRed)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OledPrimaryButton("Buy 1", { onBuyStock(selectedStock.ticker, 1) }, Modifier.weight(1f), accent = AccentGreen)
                    OledPrimaryButton("Buy 25", { onBuyStock(selectedStock.ticker, 25) }, Modifier.weight(1f), accent = AccentCyan)
                    OledOutlinedButton("Sell 1", { onSellStock(selectedStock.ticker, 1) }, Modifier.weight(1f), accent = AccentRed)
                }
            }
        }

        items(state.stocks, key = { it.ticker }) { asset ->
            val chg = asset.changePct
            OledCard(
                borderColor = if (asset.ticker == selectedTicker) AccentCyan else OledBorder,
                onClick = { selectedTicker = asset.ticker }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CorporateLogo(asset.ticker, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(asset.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            if (asset.hasMajorityControl) OledBadge("MAJORITY", AccentGold)
                        }
                        Text("${asset.ticker} • ${asset.countryIso} • Held: ${asset.playerShares}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatUsd(asset.currentPriceUsd), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        Text("${if (chg >= 0) "+" else ""}${String.format("%.2f%%", chg)}", style = MaterialTheme.typography.labelSmall, color = if (chg >= 0) AccentGreen else AccentRed)
                    }
                }
            }
        }
    }
}
