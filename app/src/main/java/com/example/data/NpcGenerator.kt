package com.example.data

import com.example.model.*
import kotlin.random.Random

object NpcGenerator {

    private val firstNames = listOf(
        "Aarav", "Priya", "Vikram", "Ananya", "Rohan", "Meera", "Karan", "Divya",
        "Elena", "Marcus", "Lucas", "Sophia", "Mateo", "Camila", "Liam", "Aria",
        "Wei", "Lin", "Jun", "Hao", "Mei", "Kenji", "Yuki", "Sora",
        "Jean", "Claire", "Antoine", "Camille", "Carlos", "Lucía", "Javier", "Isabel",
        "Dmitry", "Natasha", "Sergei", "Aleksei", "Tariq", "Layla", "Omar", "Zainab",
        "Kwame", "Amara", "Chidi", "Nia", "Sven", "Freja", "Lukas", "Hannah"
    )

    private val lastNames = listOf(
        "Sharma", "Patel", "Mehta", "Kapoor", "Rao", "Singh", "Verma", "Nair",
        "Vance", "Sterling", "Mercer", "Kensington", "Moreau", "Dubois", "Laurent", "Lefèvre",
        "García", "Rodríguez", "Mendoza", "Navarro", "Alvarez", "Valdés", "Morales", "Cruz",
        "Zhang", "Wang", "Chen", "Liu", "Tanaka", "Sato", "Takahashi", "Kim",
        "Volkov", "Kuznetsov", "Sokolov", "Petrov", "Al-Mansoor", "Al-Harbi", "Haddad", "Farouk",
        "Mensah", "Okafor", "Diallo", "Lindqvist", "Weber", "Müller", "Rossi", "Conti"
    )

    private val traitPool = listOf(
        "Quant Mind", "Iron Whip", "Shadow Broker", "Grid Architect",
        "Populist Orator", "Frugal Laborer", "Crypto Maximalist", "Hawkish Strategist",
        "Central Bank Dove", "Supply Chain Shark", "Constitutional Scholar", "Union Organizer",
        "Venture Catalyst", "Black Market Fixer", "Diplomatic Envoy", "Energy Baron"
    )

    fun buildInitialCampaign(
        playerName: String,
        playerHandle: String,
        homeCountryIso: String,
        homeRegionId: String,
        startingMode: StartingMode,
        attributes: CharacterAttributes,
        ideology: IdeologyProfile
    ): GameState {
        val rng = Random(20261123)
        val rawCountries = WorldCatalog.buildAllCountries(homeCountryIso)
        val provinces = WorldCatalog.buildProvinces(rawCountries)
        val stocks = WorldCatalog.buildInitialStocks()
        val laws = WorldCatalog.buildAllLaws()
        val chapters = WorldCatalog.buildConstitutionalChapters()
        val knowledge = WorldCatalog.buildKnowledgeTree()
        val supranationals = WorldCatalog.buildSupranationalOrgs()

        val npcs = mutableListOf<NpcCharacter>()
        val companies = mutableListOf<Company>()
        val jobs = mutableListOf<JobListing>()
        val parties = mutableListOf<PoliticalParty>()
        val powerPlants = mutableListOf<PowerPlantNode>()
        val transmissionLines = mutableListOf<TransmissionLine>()
        val substations = mutableListOf<SubstationNode>()

        // Ensure 420+ AI NPCs across the world:
        // 55 in player's home country, and distributed across all other countries
        val priorityIsos = (listOf(homeCountryIso) + listOf("US", "CN", "IN", "FR", "ES", "RU", "CU", "DE", "GB", "JP", "BR", "SA", "SG", "AD", "TR", "KR", "ZA", "NG", "AR", "MX", "ID")).distinct()
        var npcIndex = 1

        fun createNpc(iso: String, regionId: String, forcedTier: TraitTier? = null): NpcCharacter {
            val tier = forcedTier ?: when (rng.nextInt(100)) {
                in 0..44 -> TraitTier.LOW
                in 45..82 -> TraitTier.MID
                else -> TraitTier.HIGH
            }
            val (minStat, maxStat) = when (tier) {
                TraitTier.LOW -> 10 to 35
                TraitTier.MID -> 36 to 70
                TraitTier.HIGH -> 71 to 99
            }
            val intel = rng.nextInt(minStat, maxStat + 1)
            val str = rng.nextInt(minStat, maxStat + 1)
            val end = rng.nextInt(minStat, maxStat + 1)
            val eff = ((intel + end) / 2 + rng.nextInt(-4, 6)).coerceIn(minStat, maxStat)
            val lvl = when (tier) {
                TraitTier.LOW -> rng.nextInt(1, 5)
                TraitTier.MID -> rng.nextInt(5, 14)
                TraitTier.HIGH -> rng.nextInt(14, 32)
            }
            val cash = when (tier) {
                TraitTier.LOW -> rng.nextDouble(120.0, 2_800.0)
                TraitTier.MID -> rng.nextDouble(8_000.0, 95_000.0)
                TraitTier.HIGH -> rng.nextDouble(250_000.0, 8_500_000.0)
            }
            val netWorth = cash * when (tier) {
                TraitTier.LOW -> 1.1
                TraitTier.MID -> 2.4
                TraitTier.HIGH -> 6.5
            }
            val fName = firstNames[rng.nextInt(firstNames.size)]
            val lName = lastNames[rng.nextInt(lastNames.size)]
            val fullName = "$fName $lName"
            val handle = "${fName.lowercase()}_${lName.lowercase().replace("-", "")}_${npcIndex}"
            val pickedTraits = listOf(
                traitPool[rng.nextInt(traitPool.size)],
                traitPool[rng.nextInt(traitPool.size)]
            ).distinct()
            val ambition = NpcAmbition.entries[rng.nextInt(NpcAmbition.entries.size)]
            val id = "npc_${npcIndex.toString().padStart(3, '0')}"
            npcIndex++
            return NpcCharacter(
                id = id,
                name = fullName,
                handle = handle,
                countryIso = iso,
                regionId = regionId,
                traitTier = tier,
                traits = pickedTraits,
                ambition = ambition,
                level = lvl,
                intelligence = intel,
                strength = str,
                endurance = end,
                efficiency = eff,
                cashUsd = cash,
                netWorthUsd = netWorth,
                avatarSeed = rng.nextInt(1, 10000)
            )
        }

        val provincesByIso = provinces.groupBy { it.countryIso }

        // 55 citizens in home country (rich mix of Low, Mid, High)
        val homeProvinces = provincesByIso[homeCountryIso].orEmpty()
        repeat(55) { i ->
            val reg = if (homeProvinces.isNotEmpty()) homeProvinces[i % homeProvinces.size].id else "${homeCountryIso}_CAP"
            val tier = when {
                i < 12 -> TraitTier.HIGH
                i < 32 -> TraitTier.MID
                else -> TraitTier.LOW
            }
            npcs.add(createNpc(homeCountryIso, reg, tier))
        }

        // Priority global powers (10 each)
        priorityIsos.filter { it != homeCountryIso }.forEach { iso ->
            val regs = provincesByIso[iso].orEmpty()
            repeat(10) { i ->
                val reg = if (regs.isNotEmpty()) regs[i % regs.size].id else "${iso}_CAP"
                val tier = if (i < 3) TraitTier.HIGH else if (i < 7) TraitTier.MID else TraitTier.LOW
                npcs.add(createNpc(iso, reg, tier))
            }
        }

        // Remaining countries (1-2 citizens each to exceed 420+ global Pops)
        rawCountries.filter { it.iso !in priorityIsos }.forEach { c ->
            val reg = provincesByIso[c.iso]?.firstOrNull()?.id ?: "${c.iso}_CAP"
            npcs.add(createNpc(c.iso, reg, TraitTier.HIGH))
            if (npcs.size < 450) {
                npcs.add(createNpc(c.iso, reg, if (rng.nextBoolean()) TraitTier.MID else TraitTier.LOW))
            }
        }

        // Assign political parties, ministries, and Heads of State per country
        val updatedCountries = rawCountries.map { c ->
            val localNpcs = npcs.filter { it.countryIso == c.iso }.sortedByDescending { it.level }
            val highNpcs = localNpcs.filter { it.traitTier == TraitTier.HIGH }.ifEmpty { localNpcs }

            // Create 3 political parties in home country & major countries (1 if One-Party Socialist)
            val partyCount = if (c.govType == GovType.ONE_PARTY_SOCIALIST) 1 else if (c.iso in priorityIsos) 3 else 1
            val countryParties = mutableListOf<PoliticalParty>()
            val partyArchetypes = listOf(
                Triple("National Sovereign Front", "NSF", "Nationalist Developmentalism"),
                Triple("Civic Technocratic Alliance", "CTA", "Technocratic Reform"),
                Triple("Popular Labor Coalition", "PLC", "Socialist Planning")
            )
            for (pIdx in 0 until partyCount) {
                val leader = highNpcs.getOrElse(pIdx) { highNpcs.first() }
                val arch = partyArchetypes[pIdx]
                val pName = if (c.govType == GovType.ONE_PARTY_SOCIALIST) "${c.name} Vanguard Party" else "${c.name} ${arch.first}"
                val pId = "party_${c.iso}_$pIdx"
                val seats = when {
                    partyCount == 1 -> 100
                    pIdx == 0 -> 44
                    pIdx == 1 -> 34
                    else -> 22
                }
                countryParties.add(
                    PoliticalParty(
                        id = pId,
                        name = pName,
                        acronym = "${c.iso}-${arch.second}",
                        countryIso = c.iso,
                        ideology = arch.third,
                        leaderId = leader.id,
                        seatsCount = seats,
                        memberIds = listOf(leader.id),
                        treasuryUsd = rng.nextDouble(40_000.0, 400_000.0),
                        approvalPct = seats.toDouble()
                    )
                )
            }
            parties.addAll(countryParties)

            val assignments = mutableMapOf<String, String>()
            val headId: String? = if (startingMode == StartingMode.SANDBOX && c.iso == homeCountryIso) {
                "player_01"
            } else {
                highNpcs.firstOrNull()?.id
            }
            if (headId != null) {
                assignments[MinistryTag.HEAD_OF_STATE.name] = headId
            }

            val otherTags = MinistryTag.entries.filter { it != MinistryTag.HEAD_OF_STATE }
            otherTags.forEachIndexed { idx, tag ->
                if (startingMode == StartingMode.SANDBOX && c.iso == homeCountryIso && idx < 4) {
                    // Player in Sandbox also holds key starter cabinet portfolios or appoints NPCs; let's hold HEAD_OF_STATE + TREASURY + FOREIGN_AFFAIRS + DEFENSE + ECONOMY + CENTRAL_BANK + INTELLIGENCE + CENTRAL_PLANNING so Sandbox lets user test everything immediately, while Career mode keeps ALL locked until appointed!
                }
                val candidate = localNpcs.getOrNull((idx + 1) % localNpcs.size.coerceAtLeast(1))
                if (candidate != null) {
                    assignments[tag.name] = if (startingMode == StartingMode.SANDBOX && c.iso == homeCountryIso) {
                        "player_01"
                    } else {
                        candidate.id
                    }
                }
            }

            c.copy(
                headOfStateNpcId = headId,
                ministerAssignments = assignments
            )
        }

        // Sync NPC offices, parliament seats, and party IDs
        val countryMap = updatedCountries.associateBy { it.iso }
        val partyByCountry = parties.groupBy { it.countryIso }
        val syncedNpcs = npcs.mapIndexed { idx, npc ->
            val c = countryMap[npc.countryIso]
            val heldTagName = c?.ministerAssignments?.entries?.firstOrNull { it.value == npc.id }?.key
            val heldTag = heldTagName?.let { runCatching { MinistryTag.valueOf(it) }.getOrNull() }
            val isHos = c?.headOfStateNpcId == npc.id
            val cParties = partyByCountry[npc.countryIso].orEmpty()
            val pId = cParties.getOrNull(idx % cParties.size.coerceAtLeast(1))?.id
            npc.copy(
                officeHeld = heldTag,
                isHeadOfState = isHos,
                isParliamentMember = isHos || heldTag != null || (npc.traitTier != TraitTier.LOW && idx % 3 == 0),
                partyId = pId
            )
        }.toMutableList()

        // Create initial NPC & State-Owned Enterprises + LinkedOut Job Listings + Electrical Grid Nodes
        var compIdx = 1
        priorityIsos.forEach { iso ->
            val c = countryMap[iso] ?: return@forEach
            val regs = provincesByIso[iso].orEmpty()
            val primaryReg = regs.firstOrNull()?.id ?: "${iso}_CAP"
            val highOwner = syncedNpcs.firstOrNull { it.countryIso == iso && it.traitTier == TraitTier.HIGH }

            // 1) National Power Utility (Power Plant + Substation)
            val plantCompId = "comp_${compIdx++}"
            val plantFuel = when (iso) {
                "FR", "US", "RU", "CN" -> PowerFuel.NUCLEAR
                "SA", "QA" -> PowerFuel.GAS
                "ES", "DE" -> PowerFuel.SOLAR
                else -> PowerFuel.COAL
            }
            val isSoe = c.economicSystem == EconomicSystem.PLANNED || c.economicSystem == EconomicSystem.STATE_CAPITALISM
            companies.add(
                Company(
                    id = plantCompId,
                    name = "${c.name} National Grid Corp",
                    ticker = "${iso}PWR",
                    ownerId = if (isSoe) "STATE_$iso" else (highOwner?.id ?: "STATE_$iso"),
                    countryIso = iso,
                    regionId = primaryReg,
                    type = BusinessType.POWER_PLANT,
                    powerFuel = plantFuel,
                    cashUsd = 185_000.0,
                    valuationUsd = 1_450_000.0,
                    stateShares = if (isSoe) 6_500L else 1_500L,
                    efficiency = 72,
                    employeesCount = 28,
                    dailyProfitUsd = 4_200.0,
                    isStateOwnedEnterprise = isSoe
                )
            )
            powerPlants.add(
                PowerPlantNode(
                    id = "pp_$plantCompId",
                    companyId = plantCompId,
                    companyName = "${c.name} National Grid Corp",
                    countryIso = iso,
                    regionId = primaryReg,
                    fuel = plantFuel,
                    capacityMw = 14000,
                    activeOutputMw = 11200
                )
            )
            substations.add(
                SubstationNode(
                    id = "sub_$plantCompId",
                    regionId = primaryReg,
                    companyId = plantCompId,
                    companyName = "${c.name} National Grid Corp",
                    level = 3,
                    capacityMw = 15000
                )
            )
            if (regs.size > 1) {
                transmissionLines.add(
                    TransmissionLine(
                        id = "tx_${iso}_1",
                        fromRegionId = regs[0].id,
                        toRegionId = regs[1].id,
                        ownerCompanyId = plantCompId,
                        maxTransferMw = 4500
                    )
                )
            }

            // 2) Industrial / Tech / Bank companies for LinkedOut Jobs
            val starterTypes = listOf(BusinessType.FACTORY, BusinessType.SEMICONDUCTOR_PLANT, BusinessType.COMMERCIAL_BANK, BusinessType.FARM)
            starterTypes.forEachIndexed { tIdx, bType ->
                val cId = "comp_${compIdx++}"
                val ownerNpc = syncedNpcs.filter { it.countryIso == iso }.getOrElse(tIdx + 1) { syncedNpcs.first() }
                val reg = if (regs.isNotEmpty()) regs[tIdx % regs.size].id else primaryReg
                val cName = "${c.iso} ${bType.sector} Group #${tIdx + 1}"
                val isState = c.economicSystem == EconomicSystem.PLANNED || (tIdx == 0 && c.economicSystem == EconomicSystem.STATE_CAPITALISM)
                companies.add(
                    Company(
                        id = cId,
                        name = cName,
                        ticker = "${iso}${bType.name.take(3)}${tIdx + 1}",
                        ownerId = if (isState) "STATE_$iso" else ownerNpc.id,
                        countryIso = iso,
                        regionId = reg,
                        type = bType,
                        cashUsd = 65_000.0 + tIdx * 22_000.0,
                        valuationUsd = bType.baseCostUsd * 4.2,
                        stateShares = if (isState) 7_000L else 0L,
                        efficiency = 60 + tIdx * 5,
                        employeesCount = 8 + tIdx * 4,
                        dailyProfitUsd = 1_200.0 + tIdx * 650.0,
                        warehouseInventory = mapOf(
                            CommodityGood.GRAIN.name to 40,
                            CommodityGood.IRON.name to 25,
                            CommodityGood.CONSUMER_GOODS.name to 30
                        ),
                        isStateOwnedEnterprise = isState
                    )
                )
                // Create LinkedOut Jobs (focus on home country + global opportunities)
                jobs.add(
                    JobListing(
                        id = "job_${cId}_1",
                        companyId = cId,
                        companyName = cName,
                        countryIso = iso,
                        regionId = reg,
                        roleTitle = when (bType) {
                            BusinessType.COMMERCIAL_BANK -> "Junior Macro & FX Analyst"
                            BusinessType.SEMICONDUCTOR_PLANT -> "Process Operations Engineer"
                            BusinessType.FARM -> "Agri-Logistics Coordinator"
                            else -> "Industrial Operations Associate"
                        },
                        salaryPerPointUsd = (3.5 + tIdx * 1.4),
                        minIntelligence = 10 + tIdx * 6,
                        isStateJob = isState
                    )
                )
            }
        }

        // Player Character setup (Career 0.00 cash vs Sandbox Head of State)
        val homeCountry = countryMap[homeCountryIso] ?: updatedCountries.first()
        val playerOffices = if (startingMode == StartingMode.SANDBOX) {
            MinistryTag.entries.toList()
        } else {
            emptyList()
        }
        val player = PlayerCharacter(
            id = "player_01",
            name = playerName.ifBlank { "Aarav Vance" },
            handle = playerHandle.ifBlank { "aarav_vance" },
            countryIso = homeCountryIso,
            regionId = homeRegionId,
            currentCountryIso = homeCountryIso,
            currentRegionId = homeRegionId,
            level = if (startingMode == StartingMode.SANDBOX) 15 else 1,
            xp = 0,
            xpToNext = if (startingMode == StartingMode.SANDBOX) 600 else 100,
            araucitas = if (startingMode == StartingMode.SANDBOX) 25 else 3,
            cashLocal = if (startingMode == StartingMode.SANDBOX) 5_000_000.0 * homeCountry.forexPerUsd else 0.0,
            cashUsd = if (startingMode == StartingMode.SANDBOX) 5_000_000.0 else 0.0,
            bitcoinBalance = if (startingMode == StartingMode.SANDBOX) 2.5 else 0.0,
            intelligence = if (startingMode == StartingMode.SANDBOX) 65 else 14,
            strength = if (startingMode == StartingMode.SANDBOX) 45 else 12,
            endurance = if (startingMode == StartingMode.SANDBOX) 55 else 12,
            efficiency = if (startingMode == StartingMode.SANDBOX) 68 else 15,
            charisma = if (startingMode == StartingMode.SANDBOX) 70 else 14,
            attributes = attributes,
            ideology = ideology,
            officesHeld = playerOffices,
            isParliamentMember = startingMode == StartingMode.SANDBOX,
            partyId = partyByCountry[homeCountryIso]?.firstOrNull()?.id,
            nobleTitles = if (startingMode == StartingMode.SANDBOX) listOf("Grand Chancellor of ${homeCountry.name}") else emptyList()
        )

        // Initial Social Posts & Live Interactive Polls
        val homeNpcs = syncedNpcs.filter { it.countryIso == homeCountryIso }
        val hosNpc = homeNpcs.firstOrNull { it.isHeadOfState } ?: homeNpcs.first()
        val oligarchNpc = homeNpcs.firstOrNull { it.traitTier == TraitTier.HIGH && !it.isHeadOfState } ?: homeNpcs.last()
        val usNpc = syncedNpcs.firstOrNull { it.countryIso == "US" && it.traitTier == TraitTier.HIGH } ?: syncedNpcs.first()

        val initialPosts = listOf(
            SocialPost(
                id = "post_1",
                authorId = hosNpc.id,
                authorName = hosNpc.name,
                authorHandle = hosNpc.handle,
                authorBadge = if (hosNpc.isHeadOfState) "HEAD OF STATE" else "MINISTER",
                countryIso = homeCountryIso,
                content = "${homeCountry.name} economic outlook for Q4 2026: ${homeCountry.centralBankName} holds rates at ${homeCountry.interestRatePct}%. Citizens are encouraged to enroll in University faculties & build domestic enterprises.",
                timestampStr = "Nov 23, 2026 • 10:15",
                likes = 148,
                reposts = 39
            ),
            SocialPost(
                id = "post_2",
                authorId = oligarchNpc.id,
                authorName = oligarchNpc.name,
                authorHandle = oligarchNpc.handle,
                authorBadge = "TYCOON",
                countryIso = homeCountryIso,
                content = "Polling the electorate: Should ${homeCountry.lowerChamberName} pivot toward aggressive industrial subsidies or slash corporate taxes to attract semiconductor fabs?",
                attachmentTag = "POLL",
                timestampStr = "Nov 23, 2026 • 10:42",
                likes = 94,
                reposts = 21,
                pollOptions = listOf(
                    PollOption("opt_1", "Slash Corporate Tax (12%)", 42),
                    PollOption("opt_2", "Targeted Tech Subsidies", 58),
                    PollOption("opt_3", "Transition to Planned Quotas", 19)
                )
            ),
            SocialPost(
                id = "post_3",
                authorId = usNpc.id,
                authorName = usNpc.name,
                authorHandle = usNpc.handle,
                authorBadge = "GLOBAL",
                countryIso = "US",
                content = "Watch \$NVDA and \$BTC volatility this week. Cross-border SWIFT liquidity is tightening while BRICS+ clearing volumes accelerate.",
                attachmentTag = "CHART:NVDA",
                timestampStr = "Nov 23, 2026 • 10:55",
                likes = 312,
                reposts = 87
            )
        )

        // Initial Official Mailbox (Correo)
        val initialMail = listOf(
            OfficialMail(
                id = "mail_1",
                senderId = hosNpc.id,
                senderName = hosNpc.name,
                senderRole = "${homeCountry.name} Chancellery",
                countryIso = homeCountryIso,
                subject = if (startingMode == StartingMode.CAREER) "Welcome to ${homeCountry.name}: Citizen Registry & LinkedOut" else "Executive Inauguration: ${homeCountry.name}",
                body = if (startingMode == StartingMode.CAREER) {
                    "You begin at 0.00 ${homeCountry.currencyCode}. Apply to an entry position on LinkedOut, maintain your Daily Work Discipline streak, and train Intelligence/Efficiency to earn a seat in ${homeCountry.lowerChamberName}."
                } else {
                    "You have assumed the office of Head of State of ${homeCountry.name}. All Ministerial Portfolios are active under your command."
                },
                type = MailType.SYSTEM_DISPATCH,
                timestampStr = "Nov 23, 2026 • 11:00"
            ),
            OfficialMail(
                id = "mail_2",
                senderId = oligarchNpc.id,
                senderName = oligarchNpc.name,
                senderRole = "Party Whip • ${partyByCountry[homeCountryIso]?.firstOrNull()?.acronym ?: homeCountryIso}",
                countryIso = homeCountryIso,
                subject = "Invitation: Join ${homeCountry.lowerChamberName} Backbench & Party Caucus",
                body = "Our caucus sees potential in your strategic profile. Accept this formal invitation to take a seat in ${homeCountry.lowerChamberName} and vote on active legislation.",
                type = MailType.PARLIAMENT_INVITE,
                partyId = partyByCountry[homeCountryIso]?.firstOrNull()?.id,
                timestampStr = "Nov 23, 2026 • 11:00"
            )
        )

        val initialNotifications = listOf(
            NotificationItem(
                id = "notif_1",
                title = "Campaign Initialized",
                message = "${homeCountry.name} (${startingMode.title}) • ${syncedNpcs.size} AI Citizens active globally.",
                category = "SYSTEM",
                timestampStr = "Nov 23, 2026 • 11:00"
            )
        )

        val initialBills = listOf(
            ParliamentBill(
                id = "bill_1",
                countryIso = homeCountryIso,
                lawId = "L12",
                chapterNumber = null,
                title = "Industrial Subsidies: Heavy Manufacturing Blitz",
                proposerId = hosNpc.id,
                proposerName = hosNpc.name,
                currentText = "Targeted Tech",
                proposedText = "Heavy Manufacturing Blitz",
                votesFor = 46,
                votesAgainst = 31,
                votesAbstain = 8,
                hoursRemaining = 36,
                status = "ACTIVE"
            )
        )

        val initialBonds = listOf(
            BondOffering("bnd_1", "${homeCountry.name} 30-Day Sovereign Note", homeCountry.iso, true, 5.4, 1_000.0, 30),
            BondOffering("bnd_2", "U.S. Treasury 60-Day T-Bill", "US", true, 4.6, 2_500.0, 60),
            BondOffering("bnd_3", "NVIDIA Corp. 45-Day Bond", "US", false, 6.8, 5_000.0, 45),
            BondOffering("bnd_4", "Reliance Infrastructure Bond", "IN", false, 7.5, 1_500.0, 45),
            BondOffering("bnd_5", "Airbus Eurozone Green Bond", "FR", false, 5.1, 3_000.0, 60)
        )

        val initialLoans = listOf(
            BilateralLoan("loan_1", "${homeCountry.centralBankName}", "LK", 500_000.0, 4.2, 45, "ACTIVE"),
            BilateralLoan("loan_2", "European Investment Bank", "UA", 1_200_000.0, 3.5, 90, "ACTIVE")
        )

        val initialPatents = listOf(
            PatentAsset("pat_1", "Solid-State Battery Cathode", "Energy", hosNpc.id, 12_000.0, 480.0, 3),
            PatentAsset("pat_2", "Sub-2nm EUV Lithography Mask", "High-Tech", usNpc.id, 28_000.0, 1_150.0, 5)
        )

        val initialRealEstate = listOf(
            RealEstateProperty("re_1", "Capital Diplomatic Penthouse", homeCountry.capital, homeCountry.iso, "Penthouse", 18_000.0, 220.0),
            RealEstateProperty("re_2", "Bandra-Kurla Financial Suite", "Mumbai", "IN", "Commercial Tower", 45_000.0, 620.0),
            RealEstateProperty("re_3", "Champs-Élysées Sovereign Villa", "Paris", "FR", "Villa", 120_000.0, 1_450.0),
            RealEstateProperty("re_4", "Manhattan Executive Lofts", "New York", "US", "Commercial Tower", 160_000.0, 2_100.0),
            RealEstateProperty("re_5", "Gulfstream Long-Range Jet", homeCountry.capital, homeCountry.iso, "Private Jet", 320_000.0, 3_600.0)
        )

        val initialTx = listOf(
            BankTransaction("tx_init", "Account Opened (${homeCountry.currencyCode} / USD)", player.cashUsd, "CREDIT", "Nov 23, 2026 • 11:00")
        )

        return GameState(
            campaignStarted = true,
            startingMode = startingMode,
            date = GameDate(2026, 11, 23, 11),
            speed = GameSpeed.X1,
            player = player,
            countries = updatedCountries,
            provinces = provinces,
            npcs = syncedNpcs,
            companies = companies,
            jobListings = jobs,
            stocks = stocks,
            contracts = emptyList(),
            powerPlants = powerPlants,
            transmissionLines = transmissionLines,
            substations = substations,
            parties = parties,
            laws = laws,
            constitutionalChapters = chapters,
            activeBills = initialBills,
            supranationalOrgs = supranationals,
            covertOps = emptyList(),
            conflicts = emptyList(),
            socialPosts = initialPosts,
            officialMails = initialMail,
            notifications = initialNotifications,
            knowledgeNodes = knowledge,
            bankTransactions = initialTx,
            bonds = initialBonds,
            bilateralLoans = initialLoans,
            patents = initialPatents,
            realEstate = initialRealEstate,
            lastEventBanner = "${homeCountry.name} • ${homeCountry.lowerChamberName} in session • ${syncedNpcs.size} AI Pops active"
        )
    }
}
