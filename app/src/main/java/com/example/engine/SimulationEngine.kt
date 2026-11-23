package com.example.engine

import com.example.model.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

object SimulationEngine {

    fun hasPortfolioAccess(state: GameState, requiredTag: MinistryTag): Boolean {
        val home = state.countries.firstOrNull { it.iso == state.player.countryIso } ?: return false
        return home.ministerAssignments[requiredTag.name] == state.player.id ||
            requiredTag in state.player.officesHeld
    }

    fun grantXp(player: PlayerCharacter, amount: Int): Pair<PlayerCharacter, Boolean> {
        var xp = player.xp + amount
        var lvl = player.level
        var next = player.xpToNext
        var araucitas = player.araucitas
        var eff = player.efficiency
        var cha = player.charisma
        var leveledUp = false
        while (xp >= next) {
            xp -= next
            lvl += 1
            next = (lvl * 120)
            araucitas += 2
            eff = (eff + 1).coerceAtMost(150)
            cha = (cha + 1).coerceAtMost(150)
            leveledUp = true
        }
        return player.copy(
            level = lvl,
            xp = xp,
            xpToNext = next,
            araucitas = araucitas,
            efficiency = eff,
            charisma = cha
        ) to leveledUp
    }

    fun advanceTick(state: GameState, hoursDelta: Int, realSecondsElapsed: Int = 1): GameState {
        if (!state.campaignStarted || hoursDelta <= 0) return state

        val rng = Random(state.date.totalHours() + state.player.xp + 77L)
        val oldDate = state.date
        val newDate = oldDate.advanceHours(hoursDelta)
        val dayChanged = newDate.totalDays() > oldDate.totalDays()

        var player = state.player
        val notifications = state.notifications.toMutableList()
        val mails = state.officialMails.toMutableList()
        val posts = state.socialPosts.toMutableList()
        var banner = state.lastEventBanner

        // 1. Single-slot stat training real-time countdown
        player.activeTraining?.let { job ->
            val rem = job.remainingSeconds - realSecondsElapsed
            if (rem <= 0) {
                val updatedStatPlayer = when (job.stat) {
                    StatType.INTELLIGENCE -> player.copy(intelligence = player.intelligence + 2, efficiency = player.efficiency + 1)
                    StatType.STRENGTH -> player.copy(strength = player.strength + 2)
                    StatType.ENDURANCE -> player.copy(endurance = player.endurance + 2, efficiency = player.efficiency + 1)
                }.copy(activeTraining = null)
                val (xpPlayer, leveled) = grantXp(updatedStatPlayer, job.xpReward)
                player = xpPlayer
                notifications.add(
                    0,
                    NotificationItem(
                        id = "n_tr_${newDate.totalHours()}",
                        title = "${job.stat.displayName} Training Complete",
                        message = "+2 ${job.stat.displayName} & +${job.xpReward} XP" + if (leveled) " • LEVEL UP (+2 Araucitas)!" else "",
                        category = "SYSTEM",
                        timestampStr = newDate.formatted()
                    )
                )
            } else {
                player = player.copy(activeTraining = job.copy(remainingSeconds = rem))
            }
        }

        // 2. Knowledge Tree research countdown (in-game hours)
        var knowledgeNodes = state.knowledgeNodes
        player.activeResearch?.let { res ->
            val rem = res.remainingHours - hoursDelta
            if (rem <= 0) {
                val node = knowledgeNodes.firstOrNull { it.id == res.nodeId }
                knowledgeNodes = knowledgeNodes.map {
                    if (it.id == res.nodeId) it.copy(completionCount = it.completionCount + 1) else it
                }
                val updatedMap = player.completedKnowledgeIds.toMutableMap()
                updatedMap[res.nodeId] = (updatedMap[res.nodeId] ?: 0) + 1
                val (xpPlayer, _) = grantXp(
                    player.copy(
                        activeResearch = null,
                        completedKnowledgeIds = updatedMap,
                        intelligence = player.intelligence + 3,
                        efficiency = player.efficiency + 2
                    ),
                    150
                )
                player = xpPlayer
                notifications.add(
                    0,
                    NotificationItem(
                        id = "n_kn_${newDate.totalHours()}",
                        title = "Research Mastered: ${node?.title ?: res.nodeId}",
                        message = "${node?.bonusSummary ?: "Permanent strategic bonus applied."} (+150 XP)",
                        category = "SYSTEM",
                        timestampStr = newDate.formatted()
                    )
                )
            } else {
                player = player.copy(activeResearch = res.copy(remainingHours = rem))
            }
        }

        // 3. Stock market & BTC hourly micro-ticks
        val currentDay = newDate.totalDays()
        val updatedStocks = state.stocks.map { stock ->
            val vol = if (stock.isCrypto) 0.018 else 0.0085
            val drift = (rng.nextDouble() - 0.492) * vol
            val nextPrice = (stock.currentPriceUsd * (1.0 + drift)).coerceAtLeast(0.5)
            val newCandles = if (dayChanged) {
                val lastClose = stock.candles.lastOrNull()?.close ?: stock.currentPriceUsd
                val hi = maxOf(lastClose, nextPrice) * (1.0 + rng.nextDouble(0.002, 0.014))
                val lo = minOf(lastClose, nextPrice) * (1.0 - rng.nextDouble(0.002, 0.014))
                (stock.candles + CandlePoint(currentDay, lastClose, hi, lo, nextPrice)).takeLast(32)
            } else {
                stock.candles
            }
            stock.copy(
                previousPriceUsd = if (dayChanged) stock.currentPriceUsd else stock.previousPriceUsd,
                currentPriceUsd = nextPrice,
                bidPriceUsd = nextPrice * 0.997,
                askPriceUsd = nextPrice * 1.003,
                candles = newCandles
            )
        }

        // 4. Live AI Poll Voting on active polls every tick
        val updatedPosts = posts.map { post ->
            if (post.pollOptions.isNotEmpty() && rng.nextInt(100) < 70) {
                val idx = rng.nextInt(post.pollOptions.size)
                val newOpts = post.pollOptions.mapIndexed { i, opt ->
                    if (i == idx) opt.copy(votes = opt.votes + rng.nextInt(1, 5)) else opt
                }
                post.copy(pollOptions = newOpts)
            } else {
                post
            }
        }.toMutableList()

        // 5. Parliamentary Bill Countdown & AI Chamber Voting
        var updatedLaws = state.laws
        var updatedChapters = state.constitutionalChapters
        var updatedCountries = state.countries
        val updatedBills = state.activeBills.map { bill ->
            if (bill.status == "ACTIVE") {
                val rem = bill.hoursRemaining - hoursDelta
                val addFor = rng.nextInt(0, 3)
                val addAgainst = rng.nextInt(0, 3)
                val newFor = bill.votesFor + addFor
                val newAgainst = bill.votesAgainst + addAgainst
                if (rem <= 0) {
                    val passed = newFor > newAgainst
                    if (passed) {
                        if (bill.lawId != null) {
                            updatedLaws = updatedLaws.map { l ->
                                if (l.id == bill.lawId) l.copy(currentValue = bill.proposedText) else l
                            }
                            if (bill.lawId == "L11") {
                                val newEcon = when {
                                    bill.proposedText.contains("Planned", true) -> EconomicSystem.PLANNED
                                    bill.proposedText.contains("Free", true) -> EconomicSystem.MARKET
                                    bill.proposedText.contains("State", true) -> EconomicSystem.STATE_CAPITALISM
                                    else -> EconomicSystem.MIXED
                                }
                                updatedCountries = updatedCountries.map { c ->
                                    if (c.iso == bill.countryIso) c.copy(economicSystem = newEcon) else c
                                }
                            }
                        }
                        if (bill.chapterNumber != null) {
                            updatedChapters = updatedChapters.map { ch ->
                                if (ch.chapterNumber == bill.chapterNumber) {
                                    ch.copy(currentProvision = bill.proposedText, isReformed = true)
                                } else ch
                            }
                        }
                        notifications.add(
                            0,
                            NotificationItem(
                                id = "n_bill_${bill.id}",
                                title = "Legislation Passed: ${bill.title}",
                                message = "Chamber approved (${newFor}–${newAgainst}). Law enacted.",
                                category = "POLITICS",
                                timestampStr = newDate.formatted()
                            )
                        )
                    } else {
                        notifications.add(
                            0,
                            NotificationItem(
                                id = "n_bill_${bill.id}",
                                title = "Legislation Rejected: ${bill.title}",
                                message = "Defeated in chamber (${newFor}–${newAgainst}).",
                                category = "POLITICS",
                                timestampStr = newDate.formatted()
                            )
                        )
                    }
                    bill.copy(
                        votesFor = newFor,
                        votesAgainst = newAgainst,
                        hoursRemaining = 0,
                        status = if (passed) "PASSED" else "REJECTED"
                    )
                } else {
                    bill.copy(votesFor = newFor, votesAgainst = newAgainst, hoursRemaining = rem)
                }
            } else bill
        }

        // If NOT dayChanged, return fast tick state
        if (!dayChanged) {
            return state.copy(
                date = newDate,
                player = player,
                countries = updatedCountries,
                stocks = updatedStocks,
                laws = updatedLaws,
                constitutionalChapters = updatedChapters,
                activeBills = updatedBills,
                socialPosts = updatedPosts.take(60),
                notifications = notifications.take(50),
                knowledgeNodes = knowledgeNodes
            )
        }

        // ================= DAILY ROLLOVER SIMULATION =================
        val homeCountry = updatedCountries.firstOrNull { it.iso == player.countryIso } ?: updatedCountries.first()

        // A. Reset Daily 400 University Energy
        player = player.copy(energy = player.maxEnergy)

        // B. LinkedOut Daily Salary Payout
        player.currentJobId?.let { jId ->
            val job = state.jobListings.firstOrNull { it.id == jId }
            if (job != null) {
                val dailyPayUsd = job.salaryPerPointUsd * ((player.efficiency + player.intelligence) / 2.0)
                val (xpP, _) = grantXp(
                    player.copy(
                        cashUsd = player.cashUsd + dailyPayUsd,
                        cashLocal = player.cashLocal + (dailyPayUsd * homeCountry.forexPerUsd)
                    ),
                    25
                )
                player = xpP
            }
        }

        // C. Player Companies Production, Profit & Commodity Output
        val inv = state.playerInventory.toMutableMap()
        var dailyBizProfit = 0.0
        val updatedCompanies = state.companies.map { comp ->
            val baseProfit = (comp.valuationUsd * 0.008) * (comp.efficiency / 60.0)
            val variance = rng.nextDouble(0.82, 1.24)
            val profit = baseProfit * variance
            if (comp.ownerId == player.id) {
                dailyBizProfit += profit
                val producedGood = when (comp.type) {
                    BusinessType.FARM -> CommodityGood.GRAIN
                    BusinessType.MINE_URANIUM -> CommodityGood.URANIUM
                    BusinessType.MINE_COAL -> CommodityGood.COAL
                    BusinessType.MINE_GAS -> CommodityGood.GAS
                    BusinessType.MINE_IRON -> CommodityGood.IRON
                    BusinessType.SEMICONDUCTOR_PLANT -> CommodityGood.SEMICONDUCTORS
                    BusinessType.DEFENSE_PLANT -> CommodityGood.ARMAMENTS
                    else -> CommodityGood.CONSUMER_GOODS
                }
                inv[producedGood.name] = (inv[producedGood.name] ?: 0) + (2 + comp.efficiency / 25)
            }
            comp.copy(
                cashUsd = (comp.cashUsd + profit).coerceAtLeast(0.0),
                dailyProfitUsd = profit,
                valuationUsd = (comp.valuationUsd + profit * 0.65).coerceAtLeast(comp.type.baseCostUsd)
            )
        }
        if (dailyBizProfit > 0.0) {
            player = player.copy(
                cashUsd = player.cashUsd + dailyBizProfit,
                cashLocal = player.cashLocal + dailyBizProfit * homeCountry.forexPerUsd
            )
        }

        // D. Trade Contracts Daily Settlement & Random Market Events
        val updatedContracts = state.contracts.map { contract ->
            if (contract.status == "ACTIVE") {
                val rem = contract.activeDaysRemaining - 1
                val dailyInstallment = (contract.quantity * contract.unitPriceUsd) / 5.0
                if (contract.sellerId == player.id) {
                    if (contract.settleInBitcoin) {
                        val btcPrice = updatedStocks.firstOrNull { it.ticker == "BTC" }?.currentPriceUsd ?: 94200.0
                        player = player.copy(bitcoinBalance = player.bitcoinBalance + (dailyInstallment / btcPrice))
                    } else {
                        player = player.copy(
                            cashUsd = player.cashUsd + dailyInstallment,
                            cashLocal = player.cashLocal + dailyInstallment * homeCountry.forexPerUsd
                        )
                    }
                }
                if (rem <= 0) contract.copy(activeDaysRemaining = 0, status = "COMPLETED")
                else contract.copy(activeDaysRemaining = rem)
            } else contract
        }

        // E. Autonomous AI NPC Strategy Cycle (Jobs, Companies, Trading, Social Posts, Ambitions)
        val btcPrice = updatedStocks.firstOrNull { it.ticker == "BTC" }?.currentPriceUsd ?: 94200.0
        val updatedNpcs = state.npcs.mapIndexed { idx, npc ->
            if ((idx + currentDay) % 4 != 0) return@mapIndexed npc
            val roll = rng.nextInt(100)
            var cash = npc.cashUsd
            var nw = npc.netWorthUsd
            var actionText = npc.lastAction
            when {
                roll < 28 -> {
                    // Salary / Trade gain
                    val delta = (npc.efficiency * 18.0) * rng.nextDouble(0.7, 1.9)
                    cash += delta
                    nw += delta * 1.15
                    actionText = "Earned salary & trade yield (+$${delta.roundToInt()})"
                }
                roll < 52 -> {
                    // Stock / BTC speculation
                    val picked = updatedStocks[rng.nextInt(updatedStocks.size)]
                    val gain = (cash * 0.04) * rng.nextDouble(-0.4, 1.3)
                    cash = (cash + gain).coerceAtLeast(50.0)
                    nw = (nw + gain * 1.4).coerceAtLeast(cash)
                    actionText = "Traded $${picked.ticker} on exchange"
                }
                roll < 72 && npc.countryIso == homeCountry.iso -> {
                    // Social post or political statement
                    val phrases = listOf(
                        "Macro indicators in ${homeCountry.name} show ${if (homeCountry.inflationPct > 5) "inflation pressure — Central Bank must tighten M1" else "steady industrial expansion"}.",
                        "Looking to hire high-efficiency talent on LinkedOut for regional manufacturing & grid upgrades.",
                        "Bitcoin at $${btcPrice.roundToInt()} is reshaping cross-border black & private settlement flows.",
                        "The debate in ${homeCountry.lowerChamberName} over corporate taxation will define Q1 capital inflows."
                    )
                    if (updatedPosts.size < 45 || rng.nextInt(100) < 35) {
                        updatedPosts.add(
                            0,
                            SocialPost(
                                id = "post_${newDate.totalHours()}_${npc.id}",
                                authorId = npc.id,
                                authorName = npc.name,
                                authorHandle = npc.handle,
                                authorBadge = npc.officeHeld?.name ?: if (npc.traitTier == TraitTier.HIGH) "ELITE" else null,
                                countryIso = npc.countryIso,
                                content = phrases[rng.nextInt(phrases.size)],
                                timestampStr = newDate.formatted(),
                                likes = rng.nextInt(4, 65),
                                reposts = rng.nextInt(0, 18)
                            )
                        )
                    }
                    actionText = "Published political statement on Inicio"
                }
                roll >= 92 && npc.traitTier == TraitTier.HIGH -> {
                    actionText = "Lobbied ${homeCountry.lowerChamberName} for ${npc.ambition.label} agenda"
                }
            }
            npc.copy(cashUsd = cash, netWorthUsd = nw, lastAction = actionText)
        }

        // F. Career Progression Invitations via Official Mailbox (Correo)
        if (player.level >= 2 && mails.none { it.type == MailType.MINISTRY_OFFER && it.status == "PENDING" } && player.officesHeld.isEmpty()) {
            val hosNpc = updatedNpcs.firstOrNull { it.countryIso == homeCountry.iso && it.isHeadOfState }
                ?: updatedNpcs.firstOrNull { it.countryIso == homeCountry.iso }
            if (hosNpc != null && rng.nextInt(100) < 55) {
                val offeredTag = listOf(MinistryTag.ECONOMY, MinistryTag.ENERGY, MinistryTag.FOREIGN_AFFAIRS, MinistryTag.TREASURY).random(rng)
                mails.add(
                    0,
                    OfficialMail(
                        id = "mail_min_${newDate.totalHours()}",
                        senderId = hosNpc.id,
                        senderName = hosNpc.name,
                        senderRole = "Head of State • ${homeCountry.name}",
                        countryIso = homeCountry.iso,
                        subject = "Cabinet Appointment Offer: ${offeredTag.title}",
                        body = "Given your rising influence (Level ${player.level}), I formally offer you the portfolio of ${offeredTag.title}. Accepting unlocks exclusive [${offeredTag.code}] ministerial controls.",
                        type = MailType.MINISTRY_OFFER,
                        ministryTag = offeredTag,
                        timestampStr = newDate.formatted()
                    )
                )
                banner = "New Cabinet Offer in Correo: ${offeredTag.title}!"
            }
        }

        if (player.level >= 4 && player.nobleTitles.isEmpty() && mails.none { it.type == MailType.NOBLE_TITLE_GRANT }) {
            val hosNpc = updatedNpcs.firstOrNull { it.countryIso == homeCountry.iso } ?: updatedNpcs.first()
            mails.add(
                0,
                OfficialMail(
                    id = "mail_noble_${newDate.totalHours()}",
                    senderId = hosNpc.id,
                    senderName = hosNpc.name,
                    senderRole = "Chancellery of Honors • ${homeCountry.name}",
                    countryIso = homeCountry.iso,
                    subject = "Patent of Nobility: Grand Strategist of ${homeCountry.capital}",
                    body = "In recognition of your macroeconomic and civic service, the State confers upon you the title 'Baron of ${homeCountry.capital}'.",
                    type = MailType.NOBLE_TITLE_GRANT,
                    nobleTitle = "Baron of ${homeCountry.capital}",
                    timestampStr = newDate.formatted()
                )
            )
        }

        // G. Random Macro / Geopolitical Event Banner every 3 days
        if (currentDay % 3 == 0) {
            val randomCountry = updatedCountries[rng.nextInt(updatedCountries.size)]
            val macroEvents = listOf(
                "${randomCountry.centralBankName} adjusts liquidity corridor; ${randomCountry.currencyCode} FX reacts.",
                "Energy Grid surge reported in ${randomCountry.capital}; regional substations operating at 92% load.",
                "Autonomous AI syndicate acquires minority stake in ${updatedStocks[rng.nextInt(updatedStocks.size)].name}.",
                "${randomCountry.intelAgencyName} flags elevated cross-border covert signals."
            )
            banner = macroEvents[rng.nextInt(macroEvents.size)]
        }

        return state.copy(
            date = newDate,
            player = player,
            countries = updatedCountries,
            npcs = updatedNpcs,
            companies = updatedCompanies,
            stocks = updatedStocks,
            contracts = updatedContracts,
            laws = updatedLaws,
            constitutionalChapters = updatedChapters,
            activeBills = updatedBills,
            socialPosts = updatedPosts.take(60),
            officialMails = mails.take(40),
            notifications = notifications.take(50),
            knowledgeNodes = knowledgeNodes,
            playerInventory = inv,
            lastEventBanner = banner
        )
    }
}
