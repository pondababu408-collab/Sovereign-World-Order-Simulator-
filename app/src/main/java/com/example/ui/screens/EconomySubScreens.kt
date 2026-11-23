package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun OnlineBankingSubScreen(
    state: GameState,
    onUpdateCentralBank: (Double, Double, Double, Boolean) -> Unit
) {
    val home = state.countries.first { it.iso == state.player.countryIso }
    var showCentralBank by remember { mutableStateOf(false) }

    if (showCentralBank) {
        Column(modifier = Modifier.fillMaxSize()) {
            OledOutlinedButton("← Back to Commercial Account", { showCentralBank = false }, accent = AccentCyan)
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                CentralBankTab(state, onUpdateCentralBank)
            }
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard(borderColor = AccentGreen.copy(alpha = 0.55f)) {
                Text("Personal Sovereign Account", style = MaterialTheme.typography.titleSmall, color = TextDescription)
                Spacer(Modifier.height(4.dp))
                Text(formatUsd(state.player.cashUsd), style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Text(
                    "Local Currency: ${formatLocalCurrency(state.player.cashLocal, home.currencySymbol, home.currencyCode)} • FX ${String.format("%.2f", home.forexPerUsd)}/USD",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentCyan
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bank Reserves: $${String.format("%.1fB", home.m0BillionsUsd)}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    OledBadge(if (home.swiftConnected) "SWIFT ACTIVE" else "SWIFT SANCTIONED", if (home.swiftConnected) AccentGreen else AccentRed)
                }
            }
        }

        item {
            OledCard(onClick = { showCentralBank = true }, borderColor = AccentCyan.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(home.centralBankName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Inspect M0/M1 supply, interest rate (${String.format("%.2f%%", home.interestRatePct)}), & SWIFT status", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                    }
                    OledBadge("CENTRAL BANK", AccentCyan)
                }
            }
        }

        item { Text("Recent Ledger & Wire Transfers", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
        if (state.bankTransactions.isEmpty()) {
            item { OledCard { Text("No bank transactions recorded yet.", style = MaterialTheme.typography.bodySmall, color = TextDescription) } }
        } else {
            items(state.bankTransactions, key = { it.id }) { tx ->
                val isCredit = tx.amountUsd >= 0.0
                OledCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("${tx.type} • ${tx.timestampStr}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                        }
                        Text(
                            text = "${if (isCredit) "+" else ""}${formatUsd(tx.amountUsd)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCredit) AccentGreen else AccentRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StocksSubScreen(
    state: GameState,
    onBuyStock: (String, Long) -> Unit,
    onSellStock: (String, Long) -> Unit
) {
    val equities = remember(state.stocks) { state.stocks.filter { !it.isCrypto } }
    var selectedTicker by remember { mutableStateOf(equities.firstOrNull()?.ticker ?: "RELIANCE") }
    val selectedStock = equities.firstOrNull { it.ticker == selectedTicker } ?: equities.firstOrNull()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedStock != null) {
            item {
                OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CorporateLogo(selectedStock.ticker, size = 42.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${selectedStock.name} ($${selectedStock.ticker})", style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (selectedStock.hasMajorityControl) OledBadge(">50% CEO", AccentGold)
                            }
                            Text("${selectedStock.sector} • Div ${selectedStock.dividendYieldPct}% • Held: ${selectedStock.playerShares} (${String.format("%.2f%%", selectedStock.playerOwnershipPct)})", style = MaterialTheme.typography.labelSmall, color = TextDescription)
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
                        Text("Bid: ${formatUsd(selectedStock.bidPriceUsd)}", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                        Text("Ask: ${formatUsd(selectedStock.askPriceUsd)}", style = MaterialTheme.typography.labelSmall, color = AccentRed)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OledPrimaryButton("Buy 1", { onBuyStock(selectedStock.ticker, 1) }, Modifier.weight(1f), accent = AccentGreen)
                        OledPrimaryButton("Buy 25", { onBuyStock(selectedStock.ticker, 25) }, Modifier.weight(1f), accent = AccentCyan)
                        OledOutlinedButton("Sell 1", { onSellStock(selectedStock.ticker, 1) }, Modifier.weight(1f), accent = AccentRed)
                    }
                }
            }
        }

        items(equities, key = { it.ticker }) { asset ->
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
                        Text("${asset.ticker} • ${asset.countryIso} • Held: ${asset.playerShares}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
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

@Composable
fun BilateralDebtSubScreen(
    state: GameState,
    onIssueLoan: (String, Double) -> Unit
) {
    var borrowerIso by remember { mutableStateOf("FR") }
    var amountStr by remember { mutableStateOf("5000") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard(borderColor = AccentGold.copy(alpha = 0.5f)) {
                Text("Issue Bilateral Sovereign Loan", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Lend capital to foreign states at 6.2% fixed interest to expand diplomatic leverage.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = borrowerIso, onValueChange = { borrowerIso = it.uppercase().take(3) }, label = { Text("ISO") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount USD") }, singleLine = true, modifier = Modifier.weight(2f))
                }
                Spacer(Modifier.height(8.dp))
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                OledPrimaryButton(
                    text = "Disburse Sovereign Loan",
                    onClick = { onIssueLoan(borrowerIso, amt) },
                    enabled = amt > 0.0 && state.player.cashUsd >= amt,
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentGold
                )
            }
        }

        item { Text("Active Bilateral Credit Facilities (${state.bilateralLoans.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
        items(state.bilateralLoans, key = { it.id }) { loan ->
            OledCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${loan.lenderName} → ${loan.borrowerIso}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${loan.interestRatePct}% APY • ${loan.remainingDays}d to maturity", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatUsd(loan.principalUsd), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AccentGreen)
                        OledBadge(loan.status, AccentCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun BondsSubScreen(
    state: GameState,
    onBuyBond: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard {
                Text("Sovereign & Corporate Fixed Income", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Daily coupon interest settles directly into your commercial bank account until maturity.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
            }
        }
        items(state.bonds, key = { it.id }) { bond ->
            OledCard(borderColor = if (bond.unitsHeld > 0) AccentGreen.copy(alpha = 0.45f) else OledBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(bond.issuerName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            OledBadge(if (bond.isSovereign) "SOVEREIGN" else "CORP", if (bond.isSovereign) AccentGold else AccentCyan)
                        }
                        Text("Yield: ${bond.yieldPct}% • Face: ${formatUsd(bond.faceValueUsd)} • Term: ${bond.maturityDays}d", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                        if (bond.unitsHeld > 0) {
                            Text("Held: ${bond.unitsHeld} units • Matures in ${bond.daysRemaining}d", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                        }
                    }
                    OledPrimaryButton(
                        text = "Buy 1",
                        enabled = state.player.cashUsd >= bond.faceValueUsd,
                        onClick = { onBuyBond(bond.id) },
                        accent = AccentGreen
                    )
                }
            }
        }
    }
}

@Composable
fun MarketShareSubScreen(state: GameState) {
    val sorted = remember(state.companies) { state.companies.sortedByDescending { it.valuationUsd }.take(30) }
    val totalVal = sorted.sumOf { it.valuationUsd }.coerceAtLeast(1.0)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard {
                Text("Public Market Share Leaderboard", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Comparing cumulative output valuation and regional dominance across top enterprises.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
            }
        }
        itemsIndexed(sorted, key = { _, c -> c.id }) { idx, comp ->
            val sharePct = (comp.valuationUsd / totalVal) * 100.0
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${idx + 1}", style = MaterialTheme.typography.labelMedium, color = AccentGold, modifier = Modifier.width(28.dp))
                    CorporateLogo(comp.ticker, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(comp.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${comp.countryIso} • ${comp.type.displayName} • Eff ${comp.efficiency}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${String.format("%.1f%%", sharePct)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
                        Text(formatUsd(comp.valuationUsd), style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun ForexSubScreen(
    state: GameState,
    onSwapForex: (String, Double) -> Unit
) {
    var swapAmountStr by remember { mutableStateOf("1000") }
    val pairs = remember(state.countries) {
        state.countries.filter { it.iso in setOf("IN", "FR", "CN", "GB", "JP", "RU", "BR", "CU", "DE", "AE", "SA", "CH") }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard {
                Text("Live Foreign Exchange Desk", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Convert USD into major sovereign currencies & capture macro rate spreads.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = swapAmountStr,
                    onValueChange = { swapAmountStr = it },
                    label = { Text("USD Allocation per FX Trade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        items(pairs, key = { it.iso }) { c ->
            val amt = swapAmountStr.toDoubleOrNull() ?: 1000.0
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("USD / ${c.currencyCode} (${c.name})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Rate: ${String.format("%.3f", c.forexPerUsd)} ${c.currencyCode} • Inflation ${String.format("%.1f%%", c.inflationPct)}", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                    }
                    OledPrimaryButton(
                        text = "Swap",
                        enabled = amt > 0.0 && state.player.cashUsd >= amt,
                        onClick = { onSwapForex(c.iso, amt) },
                        accent = AccentCyan
                    )
                }
            }
        }
    }
}

@Composable
fun CryptoExchangeSubScreen(
    state: GameState,
    onBuyStock: (String, Long) -> Unit,
    onSellStock: (String, Long) -> Unit
) {
    val btc = state.stocks.firstOrNull { it.ticker == "BTC" }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard(borderColor = AccentBitcoin.copy(alpha = 0.6f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CorporateLogo("BTC", size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bitcoin Decentralized Network (BTC)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Untraceable • Immune to State & SWIFT Freezes", style = MaterialTheme.typography.labelSmall, color = AccentBitcoin)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Cold Wallet: ₿ ${String.format("%.5f BTC", state.player.bitcoinBalance)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = AccentBitcoin)
                if (btc != null) {
                    Text("Spot Price: ${formatUsd(btc.currentPriceUsd)} • Held Shares: ${btc.playerShares}", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                    Spacer(Modifier.height(8.dp))
                    StockChartCanvas(candles = btc.candles, accent = AccentBitcoin)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OledPrimaryButton("Buy 1 BTC Unit", { onBuyStock("BTC", 1) }, Modifier.weight(1f), accent = AccentBitcoin)
                        OledOutlinedButton("Sell 1 BTC Unit", { onSellStock("BTC", 1) }, Modifier.weight(1f), accent = AccentRed)
                    }
                }
            }
        }
    }
}

@Composable
fun DerivativesSubScreen(
    state: GameState,
    onOpenDerivative: (String, String, Double, Int) -> Unit,
    onCloseDerivative: (String) -> Unit
) {
    var selectedTicker by remember { mutableStateOf(state.stocks.firstOrNull()?.ticker ?: "NVDA") }
    var direction by remember { mutableStateOf("LONG_CALL") }
    var leverage by remember { mutableIntStateOf(5) }
    val notional = 10_000.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard {
                Text("Futures, Options & Short-Selling", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Open ${leverage}x leveraged positions. Margin required: ${formatUsd(notional / leverage)}.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.stocks.take(8).forEach { s ->
                        FilterChip(selected = selectedTicker == s.ticker, onClick = { selectedTicker = s.ticker }, label = { Text(s.ticker, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = direction == "LONG_CALL", onClick = { direction = "LONG_CALL" }, label = { Text("LONG CALL") })
                    FilterChip(selected = direction == "SHORT_PUT", onClick = { direction = "SHORT_PUT" }, label = { Text("SHORT PUT") })
                    FilterChip(selected = leverage == 5, onClick = { leverage = 5 }, label = { Text("5x") })
                    FilterChip(selected = leverage == 10, onClick = { leverage = 10 }, label = { Text("10x") })
                }
                Spacer(Modifier.height(8.dp))
                OledPrimaryButton(
                    text = "Open $direction ($${(notional / leverage).roundToInt()} Margin)",
                    onClick = { onOpenDerivative(selectedTicker, direction, notional, leverage) },
                    enabled = state.player.cashUsd >= (notional / leverage),
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentPurple
                )
            }
        }

        item { Text("Open Derivative Positions (${state.derivatives.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
        items(state.derivatives, key = { it.id }) { d ->
            val currentPrice = state.stocks.firstOrNull { it.ticker == d.underlyingTicker }?.currentPriceUsd ?: d.entryPriceUsd
            val pctMove = (currentPrice - d.entryPriceUsd) / d.entryPriceUsd
            val signedMove = if (d.direction == "LONG_CALL") pctMove else -pctMove
            val pnl = d.notionalUsd * signedMove
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${d.underlyingTicker} • ${d.leverage}x ${d.direction}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Entry: ${formatUsd(d.entryPriceUsd)} • Now: ${formatUsd(currentPrice)} • ${d.expiryDays}d left", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${if (pnl >= 0) "+" else ""}${formatUsd(pnl)}", style = MaterialTheme.typography.titleSmall, color = if (pnl >= 0) AccentGreen else AccentRed)
                        Spacer(Modifier.height(4.dp))
                        OledOutlinedButton("Close", { onCloseDerivative(d.id) }, accent = AccentGold)
                    }
                }
            }
        }
    }
}

@Composable
fun IntellectualPropertySubScreen(
    state: GameState,
    onRegisterPatent: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("Semiconductors") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard {
                Text("Register Sovereign Patent ($8,000)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Monetize industrial R&D blueprints and collect daily licensing royalties.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Patent Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Semiconductors", "Defense", "Clean Energy", "Pharma").forEach { sec ->
                        FilterChip(selected = sector == sec, onClick = { sector = sec }, label = { Text(sec, style = MaterialTheme.typography.labelSmall) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OledPrimaryButton(
                    text = "File Patent ($8,000)",
                    onClick = { onRegisterPatent(title, sector); title = "" },
                    enabled = title.isNotBlank() && state.player.cashUsd >= 8_000.0,
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentCyan
                )
            }
        }
        items(state.patents, key = { it.id }) { pat ->
            OledCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pat.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${pat.sector} • ${pat.licensedCount} active corporate licenses", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                    }
                    OledBadge("+${formatUsd(pat.dailyRoyaltyUsd)}/d", AccentGreen)
                }
            }
        }
    }
}

@Composable
fun Forbes100SubScreen(state: GameState) {
    var countryFilter by remember { mutableStateOf("ALL") }
    var showLocalCurrency by remember { mutableStateOf(false) }
    val home = state.countries.first { it.iso == state.player.countryIso }

    val entries = remember(state.npcs, state.player, countryFilter) {
        val all = listOf(
            Triple(state.player.name, "@${state.player.handle} (${state.player.countryIso})", state.player.cashUsd + state.player.bitcoinBalance * 94200.0)
        ) + state.npcs.filter { countryFilter == "ALL" || it.countryIso == countryFilter }
            .map { Triple(it.name, "@${it.handle} (${it.countryIso}) • ${it.ambition.label}", it.netWorthUsd) }
        all.sortedByDescending { it.third }.take(100)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = countryFilter == "ALL", onClick = { countryFilter = "ALL" }, label = { Text("Global") })
                        FilterChip(selected = countryFilter == home.iso, onClick = { countryFilter = home.iso }, label = { Text(home.iso) })
                    }
                    FilterChip(selected = showLocalCurrency, onClick = { showLocalCurrency = !showLocalCurrency }, label = { Text(if (showLocalCurrency) home.currencyCode else "USD") })
                }
            }
        }
        itemsIndexed(entries) { idx, (name, sub, wealthUsd) ->
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${idx + 1}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (idx < 3) AccentGold else TextSecondary, modifier = Modifier.width(34.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(sub, style = MaterialTheme.typography.labelSmall, color = TextDescription, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val displayWealth = if (showLocalCurrency) formatLocalCurrency(wealthUsd * home.forexPerUsd, home.currencySymbol, home.currencyCode) else formatUsd(wealthUsd)
                    Text(displayWealth, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AccentGreen)
                }
            }
        }
    }
}
