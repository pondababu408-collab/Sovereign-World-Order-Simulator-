package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NpcGenerator
import com.example.data.SaveGameRepository
import com.example.data.SaveSlotSummary
import com.example.data.WorldOrderDatabase
import com.example.engine.SimulationEngine
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WorldOrderDatabase.getInstance(application)
    private val saveRepo = SaveGameRepository(db.saveSlotDao())

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private val _currentNav = MutableStateFlow(NavDestination.HOME)
    val currentNav: StateFlow<NavDestination> = _currentNav.asStateFlow()

    private val _navBackStack = MutableStateFlow<List<NavDestination>>(emptyList())

    private val _selectedProvinceId = MutableStateFlow<String?>(null)
    val selectedProvinceId: StateFlow<String?> = _selectedProvinceId.asStateFlow()

    private val _selectedCountryIso = MutableStateFlow<String?>(null)
    val selectedCountryIso: StateFlow<String?> = _selectedCountryIso.asStateFlow()

    private val _inspectedNpcId = MutableStateFlow<String?>(null)
    val inspectedNpcId: StateFlow<String?> = _inspectedNpcId.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val saveSlots: StateFlow<List<SaveSlotSummary>> = saveRepo.allSlotsFlow
        .map { entities ->
            val byId = entities.associateBy { it.slotId }
            (1..5).map { id ->
                val e = byId[id]
                if (e == null) SaveSlotSummary(slotId = id, isEmpty = true)
                else SaveSlotSummary(
                    slotId = id,
                    isEmpty = false,
                    campaignName = e.campaignName,
                    countryName = e.countryName,
                    dateFormatted = e.dateFormatted,
                    playerLevel = e.playerLevel,
                    netWorthUsd = e.netWorthUsd,
                    savedAtMillis = e.savedAtMillis
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), (1..5).map { SaveSlotSummary(it, true) })

    private var tickerJob: Job? = null

    init {
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val st = _uiState.value
                delay(st.speed.delayMs)
                if (st.campaignStarted && st.speed != GameSpeed.PAUSED) {
                    _uiState.update { current ->
                        SimulationEngine.advanceTick(
                            state = current,
                            hoursDelta = current.speed.hoursPerTick,
                            realSecondsElapsed = 1
                        )
                    }
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun startNewCampaign(
        playerName: String,
        playerHandle: String,
        countryIso: String,
        regionId: String,
        mode: StartingMode,
        attributes: CharacterAttributes,
        ideology: IdeologyProfile
    ) {
        val fresh = NpcGenerator.buildInitialCampaign(
            playerName = playerName,
            playerHandle = playerHandle,
            homeCountryIso = countryIso,
            homeRegionId = regionId,
            startingMode = mode,
            attributes = attributes,
            ideology = ideology
        )
        _uiState.value = fresh
        _currentNav.value = NavDestination.HOME
        _navBackStack.value = emptyList()
        _selectedCountryIso.value = countryIso
        _selectedProvinceId.value = regionId
        showToast("Campaign launched in ${fresh.countries.firstOrNull { it.iso == countryIso }?.name ?: countryIso}")
    }

    fun saveGameToSlot(slotId: Int) {
        viewModelScope.launch {
            val st = _uiState.value
            if (!st.campaignStarted) return@launch
            saveRepo.saveToSlot(slotId, st)
            showToast("Campaign saved to Slot $slotId")
        }
    }

    fun loadGameFromSlot(slotId: Int) {
        viewModelScope.launch {
            val loaded = saveRepo.loadFromSlot(slotId)
            if (loaded != null) {
                _uiState.value = loaded
                _currentNav.value = NavDestination.HOME
                _selectedCountryIso.value = loaded.player.countryIso
                _selectedProvinceId.value = loaded.player.regionId
                showToast("Loaded Slot $slotId (${loaded.date.formatted()})")
            } else {
                showToast("Slot $slotId is empty or corrupted")
            }
        }
    }

    fun deleteSaveSlot(slotId: Int) {
        viewModelScope.launch {
            saveRepo.deleteSlot(slotId)
            showToast("Deleted Slot $slotId")
        }
    }

    fun setGameSpeed(speed: GameSpeed) {
        _uiState.update { it.copy(speed = speed) }
    }

    fun navigateTo(dest: NavDestination) {
        if (_currentNav.value != dest) {
            _navBackStack.update { (it + _currentNav.value).takeLast(12) }
            _currentNav.value = dest
        }
    }

    fun navigateBack(): Boolean {
        if (_inspectedNpcId.value != null) {
            _inspectedNpcId.value = null
            return true
        }
        val stack = _navBackStack.value
        return if (stack.isNotEmpty()) {
            _currentNav.value = stack.last()
            _navBackStack.value = stack.dropLast(1)
            true
        } else if (_currentNav.value != NavDestination.HOME) {
            _currentNav.value = NavDestination.HOME
            true
        } else {
            false
        }
    }

    fun inspectNpc(npcId: String?) {
        _inspectedNpcId.value = npcId
    }

    fun selectProvince(provinceId: String?) {
        _selectedProvinceId.value = provinceId
        val prov = _uiState.value.provinces.firstOrNull { it.id == provinceId }
        if (prov != null) {
            _selectedCountryIso.value = prov.countryIso
        }
    }

    fun selectCountry(iso: String?) {
        _selectedCountryIso.value = iso
        val firstProv = _uiState.value.provinces.firstOrNull { it.countryIso == iso }
        if (firstProv != null) _selectedProvinceId.value = firstProv.id
    }

    // ================= PROFILE, TRAINING, UNIVERSITY & KNOWLEDGE =================

    fun claimWorkDiscipline() {
        _uiState.update { st ->
            val today = st.date.totalDays()
            if (st.player.lastWorkDay == today) {
                showToast("Work Discipline already logged today")
                return@update st
            }
            val newStreak = if (st.player.lastWorkDay == today - 1 || st.player.lastWorkDay == -1) {
                st.player.workStreak + 1
            } else 1
            val xpGain = (35.0 * (1.0 + (newStreak - 1) * 0.25)).roundToInt()
            val (xpPlayer, leveled) = SimulationEngine.grantXp(
                st.player.copy(workStreak = newStreak, lastWorkDay = today),
                xpGain
            )
            showToast("Work Discipline! Streak: $newStreak • +$xpGain XP" + if (leveled) " • LEVEL UP!" else "")
            st.copy(player = xpPlayer)
        }
    }

    fun startStatTraining(stat: StatType) {
        _uiState.update { st ->
            if (st.player.activeTraining != null) {
                showToast("Single-slot training already in progress")
                return@update st
            }
            val currentVal = when (stat) {
                StatType.INTELLIGENCE -> st.player.intelligence
                StatType.STRENGTH -> st.player.strength
                StatType.ENDURANCE -> st.player.endurance
            }
            val durationSec = (8 + (currentVal / 8)).coerceIn(8, 35)
            val job = TrainingJob(
                stat = stat,
                totalSeconds = durationSec,
                remainingSeconds = durationSec,
                xpReward = 45 + st.player.level * 5
            )
            showToast("Started ${stat.displayName} training (${durationSec}s)")
            st.copy(player = st.player.copy(activeTraining = job))
        }
    }

    fun studyUniversity(faculty: UniversityFaculty, energyCost: Int = 50) {
        _uiState.update { st ->
            if (st.player.energy < energyCost) {
                showToast("Not enough daily Energy (${st.player.energy}/400). Wait for midnight or use ⚡ Araucitas.")
                return@update st
            }
            val curLvl = st.player.facultyLevels[faculty.name] ?: 1
            val updatedFaculties = st.player.facultyLevels.toMutableMap()
            updatedFaculties[faculty.name] = curLvl + 1
            val statBoosted = when (faculty) {
                UniversityFaculty.ECONOMICS, UniversityFaculty.LAW -> st.player.copy(intelligence = st.player.intelligence + 1, efficiency = st.player.efficiency + 1)
                UniversityFaculty.POLITICAL_SCIENCE -> st.player.copy(charisma = st.player.charisma + 2)
                UniversityFaculty.ENGINEERING -> st.player.copy(efficiency = st.player.efficiency + 2)
                UniversityFaculty.MILITARY -> st.player.copy(strength = st.player.strength + 1, endurance = st.player.endurance + 1)
            }
            val (xpP, leveled) = SimulationEngine.grantXp(
                statBoosted.copy(
                    energy = st.player.energy - energyCost,
                    facultyLevels = updatedFaculties
                ),
                40
            )
            showToast("Studied ${faculty.displayName} (+40 XP)" + if (leveled) " • LEVEL UP!" else "")
            st.copy(player = xpP)
        }
    }

    fun boostUniversityWithAraucitas(faculty: UniversityFaculty) {
        _uiState.update { st ->
            if (st.player.araucitas < 1) {
                showToast("No Araucitas gems available (earned on Level-Up)")
                return@update st
            }
            val updatedFaculties = st.player.facultyLevels.toMutableMap()
            updatedFaculties[faculty.name] = (updatedFaculties[faculty.name] ?: 1) + 2
            val (xpP, leveled) = SimulationEngine.grantXp(
                st.player.copy(
                    araucitas = st.player.araucitas - 1,
                    facultyLevels = updatedFaculties,
                    intelligence = st.player.intelligence + 2,
                    efficiency = st.player.efficiency + 1
                ),
                120
            )
            showToast("⚡ Araucita Boost: ${faculty.displayName} +120 XP!" + if (leveled) " • LEVEL UP!" else "")
            st.copy(player = xpP)
        }
    }

    fun startKnowledgeResearch(nodeId: String) {
        _uiState.update { st ->
            if (st.player.activeResearch != null) {
                showToast("Research slot already active")
                return@update st
            }
            val node = st.knowledgeNodes.firstOrNull { it.id == nodeId } ?: return@update st
            if (st.player.level < node.minLevel) {
                showToast("Requires Level ${node.minLevel}")
                return@update st
            }
            if (!node.isRepeatable && node.completionCount > 0) {
                showToast("One-Time Mastery already completed")
                return@update st
            }
            val hours = node.durationDays * 24
            showToast("Started ${node.title} (${node.durationDays} in-game days)")
            st.copy(player = st.player.copy(activeResearch = ActiveResearch(nodeId, hours, hours)))
        }
    }

    // ================= SOCIAL FEED (INICIO) & MAILBOX (CORREO) =================

    fun createSocialPost(content: String, pollOptionsText: List<String> = emptyList(), attachmentTag: String? = null) {
        if (content.isBlank()) return
        _uiState.update { st ->
            val pollOpts = pollOptionsText.filter { it.isNotBlank() }.mapIndexed { idx, txt ->
                PollOption("p_opt_$idx", txt.trim(), 0)
            }
            val newPost = SocialPost(
                id = "post_p_${System.currentTimeMillis()}",
                authorId = st.player.id,
                authorName = st.player.name,
                authorHandle = st.player.handle,
                authorBadge = st.player.officesHeld.firstOrNull()?.code ?: "CITIZEN",
                countryIso = st.player.countryIso,
                content = content.trim(),
                attachmentTag = if (pollOpts.isNotEmpty()) "POLL" else attachmentTag,
                timestampStr = st.date.formatted(),
                pollOptions = pollOpts
            )
            val (xpP, _) = SimulationEngine.grantXp(st.player, 10)
            st.copy(player = xpP, socialPosts = listOf(newPost) + st.socialPosts)
        }
        showToast("Published to Inicio feed (+10 XP)")
    }

    fun toggleLikePost(postId: String) {
        _uiState.update { st ->
            st.copy(socialPosts = st.socialPosts.map { p ->
                if (p.id == postId) {
                    val next = !p.likedByPlayer
                    p.copy(likedByPlayer = next, likes = if (next) p.likes + 1 else (p.likes - 1).coerceAtLeast(0))
                } else p
            })
        }
    }

    fun toggleRepost(postId: String) {
        _uiState.update { st ->
            st.copy(socialPosts = st.socialPosts.map { p ->
                if (p.id == postId) {
                    val next = !p.repostedByPlayer
                    p.copy(repostedByPlayer = next, reposts = if (next) p.reposts + 1 else (p.reposts - 1).coerceAtLeast(0))
                } else p
            })
        }
    }

    fun voteInPoll(postId: String, optionId: String) {
        _uiState.update { st ->
            st.copy(socialPosts = st.socialPosts.map { p ->
                if (p.id == postId && p.playerVotedOptionId == null) {
                    p.copy(
                        playerVotedOptionId = optionId,
                        pollOptions = p.pollOptions.map { opt ->
                            if (opt.id == optionId) opt.copy(votes = opt.votes + 1) else opt
                        }
                    )
                } else p
            })
        }
    }

    fun addReplyToPost(postId: String, replyText: String) {
        if (replyText.isBlank()) return
        _uiState.update { st ->
            st.copy(socialPosts = st.socialPosts.map { p ->
                if (p.id == postId) {
                    val reply = SocialReply(
                        id = "rep_${System.currentTimeMillis()}",
                        authorId = st.player.id,
                        authorName = st.player.name,
                        authorHandle = st.player.handle,
                        text = replyText.trim(),
                        timestampStr = st.date.formatted()
                    )
                    p.copy(replies = p.replies + reply)
                } else p
            })
        }
    }

    fun toggleFollowNpc(npcId: String) {
        _uiState.update { st ->
            val current = st.player.followingNpcIds
            val next = if (npcId in current) current - npcId else current + npcId
            st.copy(player = st.player.copy(followingNpcIds = next))
        }
    }

    fun respondToMail(mailId: String, accept: Boolean) {
        _uiState.update { st ->
            val mail = st.officialMails.firstOrNull { it.id == mailId } ?: return@update st
            if (mail.status != "PENDING") return@update st
            var player = st.player
            var countries = st.countries
            if (accept) {
                when (mail.type) {
                    MailType.PARLIAMENT_INVITE -> {
                        player = player.copy(
                            isParliamentMember = true,
                            partyId = mail.partyId ?: player.partyId
                        )
                        showToast("Seated in Parliament!")
                    }
                    MailType.MINISTRY_OFFER -> {
                        val tag = mail.ministryTag
                        if (tag != null) {
                            player = player.copy(officesHeld = (player.officesHeld + tag).distinct())
                            countries = countries.map { c ->
                                if (c.iso == player.countryIso) {
                                    c.copy(ministerAssignments = c.ministerAssignments + (tag.name to player.id))
                                } else c
                            }
                            showToast("Appointed ${tag.title}! Portfolio unlocked.")
                        }
                    }
                    MailType.NOBLE_TITLE_GRANT -> {
                        val title = mail.nobleTitle
                        if (title != null) {
                            player = player.copy(nobleTitles = (player.nobleTitles + title).distinct())
                            showToast("Noble Title conferred: $title")
                        }
                    }
                    MailType.PARTY_COFOUND_INVITE -> {
                        player = player.copy(partyId = mail.partyId)
                        showToast("Joined political coalition")
                    }
                    else -> {}
                }
            } else {
                showToast("Invitation declined")
            }
            val updatedMails = st.officialMails.map { m ->
                if (m.id == mailId) m.copy(isRead = true, status = if (accept) "ACCEPTED" else "DECLINED") else m
            }
            st.copy(player = player, countries = countries, officialMails = updatedMails)
        }
    }

    fun markAllNotificationsRead() {
        _uiState.update { st ->
            st.copy(notifications = st.notifications.map { it.copy(isRead = true) })
        }
    }

    // ================= ECONOMY HUB: LINKEDOUT, COMPANIES, MARKETS, STOCKS, GRID =================

    fun applyForJob(jobId: String) {
        _uiState.update { st ->
            val job = st.jobListings.firstOrNull { it.id == jobId } ?: return@update st
            if (st.player.intelligence < job.minIntelligence) {
                showToast("Requires Intelligence ${job.minIntelligence} (You have ${st.player.intelligence})")
                return@update st
            }
            val updatedJobs = st.jobListings.map { j ->
                if (j.id == jobId) j.copy(filledById = st.player.id)
                else if (j.filledById == st.player.id) j.copy(filledById = null)
                else j
            }
            showToast("Hired as ${job.roleTitle} at ${job.companyName}!")
            st.copy(player = st.player.copy(currentJobId = jobId), jobListings = updatedJobs)
        }
    }

    fun foundCompany(name: String, type: BusinessType, powerFuel: PowerFuel? = null) {
        if (name.isBlank()) return
        _uiState.update { st ->
            val cost = type.baseCostUsd
            if (st.player.cashUsd < cost) {
                showToast("Insufficient funds: Need $${cost.roundToInt()} USD")
                return@update st
            }
            val home = st.countries.firstOrNull { it.iso == st.player.countryIso } ?: return@update st
            val compId = "comp_p_${System.currentTimeMillis()}"
            val ticker = name.filter { it.isLetterOrDigit() }.uppercase().take(4).ifBlank { "CORP" } + (10..99).random()
            val newCompany = Company(
                id = compId,
                name = name.trim(),
                ticker = ticker,
                ownerId = st.player.id,
                countryIso = st.player.countryIso,
                regionId = st.player.regionId,
                type = type,
                powerFuel = if (type == BusinessType.POWER_PLANT) (powerFuel ?: PowerFuel.SOLAR) else null,
                cashUsd = cost * 0.25,
                valuationUsd = cost * 1.15,
                totalShares = 10_000L,
                playerShares = 10_000L,
                efficiency = 50 + (st.player.intelligence / 3),
                employeesCount = 4
            )
            val newStock = StockAsset(
                ticker = ticker,
                name = name.trim(),
                countryIso = st.player.countryIso,
                sector = type.sector,
                currentPriceUsd = (cost * 1.15) / 10_000.0,
                previousPriceUsd = (cost * 1.15) / 10_000.0,
                totalShares = 10_000L,
                playerShares = 10_000L,
                npcMajorityHolderId = st.player.id,
                dividendYieldPct = 2.5,
                linkedCompanyId = compId,
                candles = listOf(CandlePoint(st.date.totalDays(), (cost * 1.15) / 10_000.0, (cost * 1.2) / 10_000.0, (cost * 1.1) / 10_000.0, (cost * 1.15) / 10_000.0)),
                bidPriceUsd = ((cost * 1.15) / 10_000.0) * 0.99,
                askPriceUsd = ((cost * 1.15) / 10_000.0) * 1.01
            )
            val newPlants = if (type == BusinessType.POWER_PLANT) {
                st.powerPlants + PowerPlantNode(
                    id = "pp_$compId",
                    companyId = compId,
                    companyName = name.trim(),
                    countryIso = st.player.countryIso,
                    regionId = st.player.regionId,
                    fuel = powerFuel ?: PowerFuel.SOLAR,
                    capacityMw = 1500,
                    activeOutputMw = 1200
                )
            } else st.powerPlants

            val (xpP, _) = SimulationEngine.grantXp(
                st.player.copy(
                    cashUsd = st.player.cashUsd - cost,
                    cashLocal = (st.player.cashUsd - cost) * home.forexPerUsd
                ),
                85
            )
            showToast("Founded ${name.trim()} ($ticker)! (+85 XP)")
            st.copy(
                player = xpP,
                companies = listOf(newCompany) + st.companies,
                stocks = st.stocks + newStock,
                powerPlants = newPlants
            )
        }
    }

    fun hireNpcForCompany(companyId: String, npcId: String) {
        _uiState.update { st ->
            val comp = st.companies.firstOrNull { it.id == companyId && it.ownerId == st.player.id } ?: return@update st
            val npc = st.npcs.firstOrNull { it.id == npcId } ?: return@update st
            val signingFee = (npc.efficiency * 25.0)
            if (st.player.cashUsd < signingFee) {
                showToast("Need $${signingFee.roundToInt()} signing bonus")
                return@update st
            }
            val home = st.countries.first { it.iso == st.player.countryIso }
            val updatedCompanies = st.companies.map { c ->
                if (c.id == companyId) {
                    val boost = (npc.efficiency / 15).coerceAtLeast(1)
                    c.copy(employeesCount = c.employeesCount + 1, efficiency = (c.efficiency + boost).coerceAtMost(120))
                } else c
            }
            val updatedNpcs = st.npcs.map { n ->
                if (n.id == npcId) n.copy(jobCompanyId = companyId, lastAction = "Hired by ${comp.name}") else n
            }
            showToast("Hired ${npc.name} (${npc.traitTier.name} Trait) -> +Efficiency!")
            st.copy(
                player = st.player.copy(
                    cashUsd = st.player.cashUsd - signingFee,
                    cashLocal = (st.player.cashUsd - signingFee) * home.forexPerUsd
                ),
                companies = updatedCompanies,
                npcs = updatedNpcs
            )
        }
    }

    fun buyStock(ticker: String, shareUnits: Long) {
        if (shareUnits <= 0) return
        _uiState.update { st ->
            val asset = st.stocks.firstOrNull { it.ticker == ticker } ?: return@update st
            val totalCost = asset.askPriceUsd * shareUnits
            if (st.player.cashUsd < totalCost) {
                showToast("Insufficient USD ($${totalCost.roundToInt()} required)")
                return@update st
            }
            val home = st.countries.first { it.iso == st.player.countryIso }
            var player = st.player.copy(
                cashUsd = st.player.cashUsd - totalCost,
                cashLocal = (st.player.cashUsd - totalCost) * home.forexPerUsd
            )
            if (asset.isCrypto) {
                player = player.copy(bitcoinBalance = player.bitcoinBalance + (totalCost / asset.currentPriceUsd))
            }
            val updatedStocks = st.stocks.map { s ->
                if (s.ticker == ticker) {
                    val newShares = (s.playerShares + shareUnits).coerceAtMost(s.totalShares)
                    s.copy(playerShares = newShares, currentPriceUsd = s.currentPriceUsd * 1.003)
                } else s
            }
            val (xpP, _) = SimulationEngine.grantXp(player, 12)
            showToast("Bought $shareUnits $ticker @ $${"%.2f".format(asset.askPriceUsd)}")
            st.copy(player = xpP, stocks = updatedStocks)
        }
    }

    fun sellStock(ticker: String, shareUnits: Long) {
        if (shareUnits <= 0) return
        _uiState.update { st ->
            val asset = st.stocks.firstOrNull { it.ticker == ticker } ?: return@update st
            if (asset.playerShares < shareUnits) {
                showToast("Not enough shares held")
                return@update st
            }
            val proceeds = asset.bidPriceUsd * shareUnits
            val home = st.countries.first { it.iso == st.player.countryIso }
            val updatedStocks = st.stocks.map { s ->
                if (s.ticker == ticker) {
                    s.copy(playerShares = (s.playerShares - shareUnits).coerceAtLeast(0L), currentPriceUsd = (s.currentPriceUsd * 0.997).coerceAtLeast(0.5))
                } else s
            }
            showToast("Sold $shareUnits $ticker for $${"%.2f".format(proceeds)}")
            st.copy(
                player = st.player.copy(
                    cashUsd = st.player.cashUsd + proceeds,
                    cashLocal = (st.player.cashUsd + proceeds) * home.forexPerUsd
                ),
                stocks = updatedStocks
            )
        }
    }

    fun sellCommodityOnMarket(good: CommodityGood, qty: Int, isBlackMarket: Boolean, settleInBitcoin: Boolean) {
        if (qty <= 0) return
        _uiState.update { st ->
            val held = st.playerInventory[good.name] ?: 0
            if (held < qty) {
                showToast("Insufficient ${good.displayName} in warehouse ($held available)")
                return@update st
            }
            val prov = st.provinces.firstOrNull { it.id == st.player.currentRegionId }
            val home = st.countries.first { it.iso == st.player.countryIso }
            val mult = if (isBlackMarket) (prov?.blackMarketPremium ?: 1.35) else (prov?.whiteMarketIndex ?: 1.0)
            val totalUsd = good.basePriceUsd * mult * qty
            val btcPrice = st.stocks.firstOrNull { it.ticker == "BTC" }?.currentPriceUsd ?: 94200.0
            val newInv = st.playerInventory.toMutableMap()
            newInv[good.name] = held - qty

            val updatedPlayer = if (isBlackMarket && settleInBitcoin) {
                st.player.copy(bitcoinBalance = st.player.bitcoinBalance + (totalUsd / btcPrice))
            } else {
                st.player.copy(
                    cashUsd = st.player.cashUsd + totalUsd,
                    cashLocal = (st.player.cashUsd + totalUsd) * home.forexPerUsd
                )
            }
            val (xpP, _) = SimulationEngine.grantXp(updatedPlayer, 18)
            showToast(
                if (isBlackMarket && settleInBitcoin) "Black Market Sold $qty ${good.displayName} -> +${"%.4f".format(totalUsd / btcPrice)} BTC"
                else "Sold $qty ${good.displayName} on ${if (isBlackMarket) "Black" else "White"} Market (+$${totalUsd.roundToInt()})"
            )
            st.copy(player = xpP, playerInventory = newInv)
        }
    }

    fun createTradeContract(buyerIso: String, good: CommodityGood, qty: Int, customPriceUsd: Double, isBlackMarket: Boolean) {
        if (qty <= 0 || customPriceUsd <= 0.0) return
        _uiState.update { st ->
            val held = st.playerInventory[good.name] ?: 0
            if (held < qty) {
                showToast("Need $qty ${good.displayName} in warehouse")
                return@update st
            }
            val newInv = st.playerInventory.toMutableMap()
            newInv[good.name] = held - qty
            val contract = TradeContract(
                id = "tc_${System.currentTimeMillis()}",
                sellerId = st.player.id,
                sellerName = st.player.name,
                buyerCountryIso = buyerIso,
                sellerCountryIso = st.player.countryIso,
                good = good,
                quantity = qty,
                unitPriceUsd = customPriceUsd,
                marketPriceUsd = good.basePriceUsd,
                isBlackMarket = isBlackMarket,
                settleInBitcoin = isBlackMarket,
                activeDaysRemaining = 5,
                status = "ACTIVE"
            )
            showToast("Signed 5-day Trade Contract with $buyerIso ($qty ${good.name})")
            st.copy(playerInventory = newInv, contracts = listOf(contract) + st.contracts)
        }
    }

    fun buildSubstationOrUpgrade(companyId: String) {
        _uiState.update { st ->
            val comp = st.companies.firstOrNull { it.id == companyId && it.ownerId == st.player.id }
            if (comp == null) {
                showToast("Found or own a company in this region first")
                return@update st
            }
            val existing = st.substations.firstOrNull { it.companyId == companyId && it.regionId == st.player.currentRegionId }
            val cost = if (existing == null) 8_500.0 else (existing.level * 6_500.0)
            if (st.player.cashUsd < cost) {
                showToast("Need $${cost.roundToInt()} USD for Substation")
                return@update st
            }
            val home = st.countries.first { it.iso == st.player.countryIso }
            val updatedSubs = if (existing == null) {
                st.substations + SubstationNode(
                    id = "sub_${System.currentTimeMillis()}",
                    regionId = st.player.currentRegionId,
                    companyId = companyId,
                    companyName = comp.name,
                    level = 1,
                    capacityMw = 1200
                )
            } else {
                st.substations.map { s ->
                    if (s.id == existing.id) s.copy(level = s.level + 1, capacityMw = s.capacityMw + 1000) else s
                }
            }
            showToast(if (existing == null) "Built Regional Substation (1200 MW)" else "Upgraded Substation to Lv.${existing.level + 1}")
            st.copy(
                player = st.player.copy(
                    cashUsd = st.player.cashUsd - cost,
                    cashLocal = (st.player.cashUsd - cost) * home.forexPerUsd
                ),
                substations = updatedSubs
            )
        }
    }

    fun buildTransmissionLine(fromRegId: String, toRegId: String, companyId: String) {
        if (fromRegId == toRegId) return
        _uiState.update { st ->
            val cost = 12_000.0
            if (st.player.cashUsd < cost) {
                showToast("Need $12,000 USD to build Transmission Grid link")
                return@update st
            }
            val home = st.countries.first { it.iso == st.player.countryIso }
            val line = TransmissionLine("tx_${System.currentTimeMillis()}", fromRegId, toRegId, companyId, 2500)
            showToast("Transmission Grid connected ($fromRegId -> $toRegId)")
            st.copy(
                player = st.player.copy(cashUsd = st.player.cashUsd - cost, cashLocal = (st.player.cashUsd - cost) * home.forexPerUsd),
                transmissionLines = st.transmissionLines + line
            )
        }
    }

    // ================= STRICT MINISTRY ISOLATION: CENTRAL BANK, PLANNING, LAWS, COVERT OPS =================

    fun updateCentralBankPolicy(interestRateDelta: Double, reserveDelta: Double, m0PrintBillions: Double, toggleSwift: Boolean) {
        _uiState.update { st ->
            if (!SimulationEngine.hasPortfolioAccess(st, MinistryTag.CENTRAL_BANK)) {
                showToast("LOCKED: Requires [CENTRAL_BANK] Governor appointment")
                return@update st
            }
            val updated = st.countries.map { c ->
                if (c.iso == st.player.countryIso) {
                    val nextRate = (c.interestRatePct + interestRateDelta).coerceIn(0.25, 25.0)
                    val nextRes = (c.reserveRatioPct + reserveDelta).coerceIn(2.0, 50.0)
                    val nextM0 = (c.m0BillionsUsd + m0PrintBillions).coerceAtLeast(1.0)
                    val nextM1 = nextM0 * (100.0 / nextRes).coerceIn(1.5, 8.0)
                    val nextInf = (c.inflationPct + (m0PrintBillions * 0.08) - (interestRateDelta * 0.35)).coerceIn(0.2, 45.0)
                    c.copy(
                        interestRatePct = nextRate,
                        reserveRatioPct = nextRes,
                        m0BillionsUsd = nextM0,
                        m1BillionsUsd = nextM1,
                        inflationPct = nextInf,
                        swiftConnected = if (toggleSwift) !c.swiftConnected else c.swiftConnected
                    )
                } else c
            }
            showToast("Central Bank directive executed")
            st.copy(countries = updated)
        }
    }

    fun updateCentralPlanning(rationQuotaKg: Int, wageFloorMult: Double, fiveYearGdpTarget: Double) {
        _uiState.update { st ->
            val home = st.countries.first { it.iso == st.player.countryIso }
            if (home.economicSystem != EconomicSystem.PLANNED) {
                showToast("LOCKED: Country must adopt Planned Economy first")
                return@update st
            }
            if (!SimulationEngine.hasPortfolioAccess(st, MinistryTag.CENTRAL_PLANNING)) {
                showToast("LOCKED: Requires [CENTRAL_PLANNING] Committee seat")
                return@update st
            }
            val updated = st.countries.map { c ->
                if (c.iso == home.iso) {
                    c.copy(
                        rationQuotaKgPerPop = rationQuotaKg.coerceIn(0, 100),
                        wageFloorMultiplier = wageFloorMult.coerceIn(0.5, 3.0),
                        fiveYearQuotaTargetGdp = fiveYearGdpTarget.coerceAtLeast(c.gdpBillionsUsd)
                    )
                } else c
            }
            showToast("Central Planning Committee 5-Year Directive Updated")
            st.copy(countries = updated)
        }
    }

    fun appointMinister(tag: MinistryTag, assigneeId: String) {
        _uiState.update { st ->
            if (!SimulationEngine.hasPortfolioAccess(st, MinistryTag.HEAD_OF_STATE)) {
                showToast("LOCKED: Only Head of State can appoint or revoke Ministers")
                return@update st
            }
            val updatedCountries = st.countries.map { c ->
                if (c.iso == st.player.countryIso) {
                    c.copy(ministerAssignments = c.ministerAssignments + (tag.name to assigneeId))
                } else c
            }
            val newOffices = st.player.officesHeld.toMutableSet()
            if (assigneeId == st.player.id) newOffices.add(tag) else newOffices.remove(tag)
            val updatedNpcs = st.npcs.map { n ->
                if (n.id == assigneeId) n.copy(officeHeld = tag)
                else if (n.countryIso == st.player.countryIso && n.officeHeld == tag) n.copy(officeHeld = null)
                else n
            }
            showToast("Appointed ${tag.title} portfolio")
            st.copy(
                player = st.player.copy(officesHeld = newOffices.toList()),
                countries = updatedCountries,
                npcs = updatedNpcs
            )
        }
    }

    fun foundPoliticalParty(name: String, acronym: String, ideology: String) {
        if (name.isBlank() || acronym.isBlank()) return
        _uiState.update { st ->
            val cost = 15_000.0
            if (st.player.cashUsd < cost) {
                showToast("Need $15,000 USD to register a Political Party")
                return@update st
            }
            val home = st.countries.first { it.iso == st.player.countryIso }
            val party = PoliticalParty(
                id = "party_p_${System.currentTimeMillis()}",
                name = name.trim(),
                acronym = acronym.trim().uppercase().take(6),
                countryIso = home.iso,
                ideology = ideology.ifBlank { "Reformist" },
                leaderId = st.player.id,
                seatsCount = 6,
                memberIds = listOf(st.player.id),
                treasuryUsd = 5_000.0,
                approvalPct = 6.5
            )
            showToast("Founded ${party.name} [${party.acronym}]!")
            st.copy(
                player = st.player.copy(
                    cashUsd = st.player.cashUsd - cost,
                    cashLocal = (st.player.cashUsd - cost) * home.forexPerUsd,
                    partyId = party.id
                ),
                parties = listOf(party) + st.parties
            )
        }
    }

    fun proposeLawBill(law: LawItem, proposedOption: String) {
        _uiState.update { st ->
            if (!st.player.isParliamentMember && !SimulationEngine.hasPortfolioAccess(st, law.requiredMinistry) && !SimulationEngine.hasPortfolioAccess(st, MinistryTag.HEAD_OF_STATE)) {
                showToast("LOCKED: Requires Parliament Seat or [${law.requiredMinistry.code}]")
                return@update st
            }
            val bill = ParliamentBill(
                id = "bill_${System.currentTimeMillis()}",
                countryIso = st.player.countryIso,
                lawId = law.id,
                chapterNumber = null,
                title = "${law.code}: ${law.title}",
                proposerId = st.player.id,
                proposerName = st.player.name,
                currentText = law.currentValue,
                proposedText = proposedOption,
                votesFor = 18,
                votesAgainst = 10,
                votesAbstain = 4,
                playerVoted = "FOR",
                hoursRemaining = 24,
                status = "ACTIVE"
            )
            showToast("Bill tabled in legislature: ${law.title}")
            st.copy(activeBills = listOf(bill) + st.activeBills)
        }
    }

    fun proposeConstitutionalReform(chapter: ConstitutionalChapter) {
        _uiState.update { st ->
            if (!st.player.isParliamentMember && !SimulationEngine.hasPortfolioAccess(st, MinistryTag.HEAD_OF_STATE)) {
                showToast("LOCKED: Requires Parliament Seat or Head of State")
                return@update st
            }
            val bill = ParliamentBill(
                id = "const_${System.currentTimeMillis()}",
                countryIso = st.player.countryIso,
                lawId = null,
                chapterNumber = chapter.chapterNumber,
                title = "Constitutional Ch.${chapter.chapterNumber}: ${chapter.title}",
                proposerId = st.player.id,
                proposerName = st.player.name,
                currentText = chapter.currentProvision,
                proposedText = chapter.proposedProvision,
                votesFor = 28,
                votesAgainst = 14,
                votesAbstain = 6,
                playerVoted = "FOR",
                hoursRemaining = 36,
                status = "ACTIVE"
            )
            showToast("Constitutional Reform Ch.${chapter.chapterNumber} tabled")
            st.copy(activeBills = listOf(bill) + st.activeBills)
        }
    }

    fun castParliamentVote(billId: String, vote: String) {
        _uiState.update { st ->
            if (!st.player.isParliamentMember && !SimulationEngine.hasPortfolioAccess(st, MinistryTag.HEAD_OF_STATE)) {
                showToast("LOCKED: Only Parliament Members can vote")
                return@update st
            }
            val updatedBills = st.activeBills.map { b ->
                if (b.id == billId && b.playerVoted == null) {
                    val weight = 8 + (st.player.charisma / 10)
                    when (vote) {
                        "FOR" -> b.copy(votesFor = b.votesFor + weight, playerVoted = vote)
                        "AGAINST" -> b.copy(votesAgainst = b.votesAgainst + weight, playerVoted = vote)
                        else -> b.copy(votesAbstain = b.votesAbstain + weight, playerVoted = vote)
                    }
                } else b
            }
            showToast("Cast $vote vote in chamber")
            st.copy(activeBills = updatedBills)
        }
    }

    // ================= WORLD MAP, TRAVEL, SUMMITS & COVERT OPS =================

    fun travelToRegion(provinceId: String) {
        _uiState.update { st ->
            val targetProv = st.provinces.firstOrNull { it.id == provinceId } ?: return@update st
            val targetCountry = st.countries.firstOrNull { it.iso == targetProv.countryIso } ?: return@update st
            if (targetCountry.iso != st.player.countryIso && !targetCountry.visaExemption) {
                showToast("ENTRY DENIED: No Visa Exemption Treaty with ${targetCountry.name} (Relation: ${targetCountry.bilateralRelation})")
                return@update st
            }
            showToast("Traveled to ${targetProv.capitalCity}, ${targetCountry.name}")
            st.copy(
                player = st.player.copy(
                    currentCountryIso = targetCountry.iso,
                    currentRegionId = targetProv.id
                )
            )
        }
    }

    fun proposeSummitDeal(offer: SummitOffer): String {
        var outcomeMsg = ""
        _uiState.update { st ->
            if (!SimulationEngine.hasPortfolioAccess(st, MinistryTag.FOREIGN_AFFAIRS) && !SimulationEngine.hasPortfolioAccess(st, MinistryTag.HEAD_OF_STATE)) {
                outcomeMsg = "LOCKED: Requires [FOREIGN_AFFAIRS] or [HEAD_OF_STATE]"
                showToast(outcomeMsg)
                return@update st
            }
            val target = st.countries.firstOrNull { it.iso == offer.targetCountryIso } ?: return@update st
            val score = target.bilateralRelation +
                (if (offer.foreignAidMillionsUsd > 0) 20 else 0) +
                (if (offer.currencySwapMillionsUsd > 0) 15 else 0) +
                (if (offer.armsDealMillionsUsd > 0) 10 else 0) +
                (st.player.charisma / 3)
            val accepted = score >= 30
            if (accepted) {
                val updated = st.countries.map { c ->
                    if (c.iso == target.iso) {
                        c.copy(
                            bilateralRelation = (c.bilateralRelation + 22).coerceAtMost(100),
                            visaExemption = c.visaExemption || offer.visaExemptionProposed,
                            allianceBloc = if (offer.allianceInvite) "Allied Pact" else c.allianceBloc
                        )
                    } else c
                }
                outcomeMsg = "SUMMIT ACCEPTED by ${target.name}! Bilateral relations improved; treaties ratified."
                showToast("Summit Treaty Signed with ${target.name}!")
                st.copy(countries = updated)
            } else {
                outcomeMsg = "COUNTER-OFFER from ${target.name}: Relations (${target.bilateralRelation}) too strained. Include Foreign Aid or Currency Swap."
                showToast("Summit Counter-Offer Received")
                st
            }
        }
        return outcomeMsg
    }

    fun launchCovertOp(targetIso: String, type: CovertOpType) {
        _uiState.update { st ->
            if (!SimulationEngine.hasPortfolioAccess(st, type.requiredMinistry)) {
                showToast("LOCKED: Requires [${type.requiredMinistry.code}] portfolio")
                return@update st
            }
            val target = st.countries.firstOrNull { it.iso == targetIso } ?: return@update st
            val rng = Random(System.currentTimeMillis())
            val chance = (45 + st.player.intelligence / 2 + target.spyNetworkStrength / 3).coerceIn(20, 92)
            val succeeded = rng.nextInt(100) < chance
            val updatedCountries = st.countries.map { c ->
                if (c.iso == targetIso && succeeded) {
                    when (type) {
                        CovertOpType.BUILD_SPY_NETWORK -> c.copy(spyNetworkStrength = (c.spyNetworkStrength + 28).coerceAtMost(100))
                        CovertOpType.COUP_INSTIGATION -> c.copy(stability = (c.stability - 24).coerceAtLeast(5), bilateralRelation = (c.bilateralRelation - 25).coerceAtLeast(-100))
                        CovertOpType.CURRENCY_SHORT_SABOTAGE -> c.copy(forexPerUsd = c.forexPerUsd * 1.12, inflationPct = c.inflationPct + 2.4)
                        CovertOpType.ARMS_SMUGGLING -> c.copy(stability = (c.stability - 8).coerceAtLeast(5))
                        CovertOpType.COUNTER_INTEL_PURGE -> c.copy(stability = (c.stability + 12).coerceAtMost(100))
                    }
                } else c
            }
            val op = CovertOperation(
                id = "op_${System.currentTimeMillis()}",
                targetCountryIso = targetIso,
                type = type,
                status = if (succeeded) "SUCCESS" else "COMPROMISED",
                successChancePct = chance,
                costUsd = 25_000.0,
                launchedDay = st.date.totalDays()
            )
            showToast("${type.label} -> ${op.status}")
            st.copy(countries = updatedCountries, covertOps = listOf(op) + st.covertOps)
        }
    }

    fun launchConflictAction(targetIso: String, intensity: String) {
        _uiState.update { st ->
            if (!SimulationEngine.hasPortfolioAccess(st, MinistryTag.DEFENSE)) {
                showToast("LOCKED: Requires [DEFENSE] Minister appointment")
                return@update st
            }
            val prov = st.provinces.firstOrNull { it.countryIso == targetIso }?.id ?: "${targetIso}_CAP"
            val conflict = MilitaryConflict(
                id = "war_${System.currentTimeMillis()}",
                aggressorIso = st.player.countryIso,
                defenderIso = targetIso,
                intensity = intensity,
                aggressorStrength = 70 + st.player.strength / 2,
                defenderStrength = 65,
                frontlineProvinceId = prov,
                startedDay = st.date.totalDays()
            )
            val updated = st.countries.map { c ->
                if (c.iso == targetIso) c.copy(bilateralRelation = -95, stability = (c.stability - 12).coerceAtLeast(10)) else c
            }
            showToast("Strategic Escalation initiated against $targetIso ($intensity)")
            st.copy(countries = updated, conflicts = listOf(conflict) + st.conflicts)
        }
    }
}
