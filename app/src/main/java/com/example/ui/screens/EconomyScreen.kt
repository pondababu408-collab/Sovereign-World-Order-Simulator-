package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

enum class EconomySubRoute(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
) {
    ONLINE_BANKING("Online banking", "Bank accounts, wire ledger, reserves & SWIFT status.", Icons.Filled.AccountBalance, AccentGreen),
    STOCKS("Stocks", "Global stock market, charts, order books & >50% control.", Icons.AutoMirrored.Filled.ShowChart, AccentCyan),
    BILATERAL_DEBT("Bilateral debt", "Manage financial assets, sovereign loans & credit portfolio.", Icons.Filled.RequestQuote, AccentGold),
    BONDS("Bonds", "Sovereign & corporate bonds with yields and maturities.", Icons.AutoMirrored.Filled.ReceiptLong, AccentGreen),
    B2B_PLATFORM("B2B platform", "Wholesale trade, founding firms & 3-tier power grid contracts.", Icons.Filled.PrecisionManufacturing, AccentCyan),
    B2C_MARKET("B2C market", "Regional White & Black markets in Legal Tender or Bitcoin.", Icons.Filled.Storefront, AccentBitcoin),
    MARKET_SHARE("Market share", "Weekly & monthly seller ranking across top enterprises.", Icons.Filled.PieChart, AccentPurple),
    FOREIGN_EXCHANGE("Foreign exchange", "Live Forex currency pairs & cross-border swaps.", Icons.Filled.CurrencyExchange, AccentCyan),
    CRYPTO_EXCHANGE("Crypto exchange", "Decentralized Bitcoin wallet & untraceable trading.", Icons.Filled.CurrencyBitcoin, AccentBitcoin),
    DERIVATIVES("Derivatives", "Leveraged futures, calls, puts & short-selling contracts.", Icons.Filled.CandlestickChart, AccentPurple),
    INTELLECTUAL_PROPERTY("Intellectual property", "Register R&D patents and earn daily royalties.", Icons.Filled.Lightbulb, AccentGold),
    FORBES_100("Forbes 100", "Live ranking of the 100 wealthiest players & AI NPCs.", Icons.Filled.EmojiEvents, AccentGold)
}

@Composable
fun EconomyScreen(
    state: GameState,
    initialSubRoute: EconomySubRoute? = null,
    onFoundCompany: (String, BusinessType, PowerFuel?) -> Unit,
    onHireNpc: (String, String) -> Unit,
    onBuyStock: (String, Long) -> Unit,
    onSellStock: (String, Long) -> Unit,
    onSellCommodity: (CommodityGood, Int, Boolean, Boolean) -> Unit,
    onCreateContract: (String, CommodityGood, Int, Double, Boolean) -> Unit,
    onBuildSubstationOrUpgrade: (String) -> Unit,
    onBuildTransmission: (String, String, String) -> Unit,
    onUpdateCentralBank: (Double, Double, Double, Boolean) -> Unit,
    onBuyBond: (String) -> Unit,
    onIssueBilateralLoan: (String, Double) -> Unit,
    onOpenDerivative: (String, String, Double, Int) -> Unit,
    onCloseDerivative: (String) -> Unit,
    onRegisterPatent: (String, String) -> Unit,
    onSwapForex: (String, Double) -> Unit
) {
    var activeSub by remember(initialSubRoute) { mutableStateOf(initialSubRoute) }

    BackHandler(enabled = activeSub != null) {
        activeSub = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (activeSub == null) {
            WorldOrderHubHeader(
                title = "Economy",
                quote = "\"If I owe you a pound, I have a problem; But if I owe you a million, the problem is yours.\" - John Maynard Keynes"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(EconomySubRoute.entries, key = { it.name }) { route ->
                    HubMenuCard(
                        title = route.title,
                        description = route.description,
                        icon = route.icon,
                        accent = route.accent,
                        onClick = { activeSub = route },
                        modifier = Modifier.testTag("econ_card_${route.name.lowercase()}")
                    )
                }
            }
        } else {
            WorldOrderHubHeader(
                title = activeSub!!.title,
                quote = "Cash: ${formatUsd(state.player.cashUsd)} • ₿ ${String.format("%.4f", state.player.bitcoinBalance)}",
                onBack = { activeSub = null }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (activeSub!!) {
                    EconomySubRoute.ONLINE_BANKING -> OnlineBankingSubScreen(state, onUpdateCentralBank)
                    EconomySubRoute.STOCKS -> StocksSubScreen(state, onBuyStock, onSellStock)
                    EconomySubRoute.BILATERAL_DEBT -> BilateralDebtSubScreen(state, onIssueBilateralLoan)
                    EconomySubRoute.BONDS -> BondsSubScreen(state, onBuyBond)
                    EconomySubRoute.B2B_PLATFORM -> B2BPlatformSubScreen(
                        state = state,
                        onFoundCompany = onFoundCompany,
                        onHireNpc = onHireNpc,
                        onCreateContract = onCreateContract,
                        onBuildSubstationOrUpgrade = onBuildSubstationOrUpgrade,
                        onBuildTransmission = onBuildTransmission
                    )
                    EconomySubRoute.B2C_MARKET -> B2CMarketSubScreen(state, onSellCommodity)
                    EconomySubRoute.MARKET_SHARE -> MarketShareSubScreen(state)
                    EconomySubRoute.FOREIGN_EXCHANGE -> ForexSubScreen(state, onSwapForex)
                    EconomySubRoute.CRYPTO_EXCHANGE -> CryptoExchangeSubScreen(state, onBuyStock, onSellStock)
                    EconomySubRoute.DERIVATIVES -> DerivativesSubScreen(state, onOpenDerivative, onCloseDerivative)
                    EconomySubRoute.INTELLECTUAL_PROPERTY -> IntellectualPropertySubScreen(state, onRegisterPatent)
                    EconomySubRoute.FORBES_100 -> Forbes100SubScreen(state)
                }
            }
        }
    }
}

@Composable
private fun B2BPlatformSubScreen(
    state: GameState,
    onFoundCompany: (String, BusinessType, PowerFuel?) -> Unit,
    onHireNpc: (String, String) -> Unit,
    onCreateContract: (String, CommodityGood, Int, Double, Boolean) -> Unit,
    onBuildSubstationOrUpgrade: (String) -> Unit,
    onBuildTransmission: (String, String, String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0: My Businesses, 1: Wholesale Contracts, 2: Grid Ops
    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Enterprises") })
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("B2B Contracts") })
            FilterChip(selected = mode == 2, onClick = { mode = 2 }, label = { Text("Grid Deals") })
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            when (mode) {
                0 -> BusinessesPanel(state, onFoundCompany, onHireNpc)
                1 -> WholesaleContractsPanel(state, onCreateContract)
                else -> ElectricalGridTab(state, onBuildSubstationOrUpgrade, onBuildTransmission)
            }
        }
    }
}

@Composable
private fun BusinessesPanel(
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
                Text("Found New Enterprise", style = MaterialTheme.typography.titleSmall, color = AccentGold)
                Text("Start Farms, Mines, Fabs, Banks, Media or Power Plants.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
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
                        Text("${comp.type.displayName} • Eff ${comp.efficiency} • ${comp.employeesCount} Staff", style = MaterialTheme.typography.bodySmall, color = TextDescription)
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
            title = { Text("Recruit AI Citizens", style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
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
private fun WholesaleContractsPanel(
    state: GameState,
    onCreateContract: (String, CommodityGood, Int, Double, Boolean) -> Unit
) {
    var isBlackMarket by remember { mutableStateOf(false) }
    var targetIso by remember { mutableStateOf("US") }
    var customPriceStr by remember { mutableStateOf("250") }
    var selectedGood by remember { mutableStateOf(CommodityGood.GRAIN) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard {
                Text("Negotiate Wholesale B2B Contract", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
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
                        label = { Text("Price $/u") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommodityGood.entries.forEach { g ->
                        FilterChip(
                            selected = selectedGood == g,
                            onClick = { selectedGood = g; customPriceStr = g.basePriceUsd.roundToInt().toString() },
                            label = { Text("${g.name} (${state.playerInventory[g.name] ?: 0})", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isBlackMarket, onCheckedChange = { isBlackMarket = it })
                    Text("Smuggle via Black Market (Settle in BTC)", style = MaterialTheme.typography.bodySmall, color = AccentBitcoin)
                }
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
private fun B2CMarketSubScreen(
    state: GameState,
    onSellCommodity: (CommodityGood, Int, Boolean, Boolean) -> Unit
) {
    val prov = state.provinces.firstOrNull { it.id == state.player.currentRegionId }
    var isBlackMarket by remember { mutableStateOf(false) }
    var settleBitcoin by remember { mutableStateOf(true) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Regional Retail: ${prov?.capitalCity ?: state.player.currentRegionId}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
    }
}

