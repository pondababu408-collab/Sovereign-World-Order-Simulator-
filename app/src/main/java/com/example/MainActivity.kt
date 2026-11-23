package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NavDestination
import com.example.ui.components.LeftIconRail
import com.example.ui.components.TopClockBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorldOrderTheme {
                WorldOrderRootApp()
            }
        }
    }
}

@Composable
fun WorldOrderRootApp(vm: GameViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val currentNav by vm.currentNav.collectAsStateWithLifecycle()
    val saveSlots by vm.saveSlots.collectAsStateWithLifecycle()
    val selectedProvinceId by vm.selectedProvinceId.collectAsStateWithLifecycle()
    val selectedCountryIso by vm.selectedCountryIso.collectAsStateWithLifecycle()
    val inspectedNpcId by vm.inspectedNpcId.collectAsStateWithLifecycle()
    val toastMsg by vm.toastMessage.collectAsStateWithLifecycle()

    var forceSetupMode by remember { mutableStateOf(false) }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2600L)
            vm.clearToast()
        }
    }

    BackHandler(enabled = state.campaignStarted && !forceSetupMode && (inspectedNpcId != null || currentNav != NavDestination.HOME)) {
        vm.navigateBack()
    }

    if (!state.campaignStarted || forceSetupMode) {
        SetupScreen(
            saveSlots = saveSlots,
            onStartCampaign = { name, handle, iso, regId, mode, attrs, ideo ->
                vm.startNewCampaign(name, handle, iso, regId, mode, attrs, ideo)
                forceSetupMode = false
            },
            onLoadSlot = { slotId ->
                vm.loadGameFromSlot(slotId)
                forceSetupMode = false
            },
            onDeleteSlot = { slotId -> vm.deleteSaveSlot(slotId) }
        )
        return
    }

    val unreadMails = state.officialMails.count { !it.isRead && it.status == "PENDING" }
    val unreadAlerts = state.notifications.count { !it.isRead }

    Scaffold(
        containerColor = OledBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopClockBar(
                    state = state,
                    saveSlots = saveSlots,
                    onSpeedSelected = vm::setGameSpeed,
                    onSaveToSlot = vm::saveGameToSlot,
                    onLoadFromSlot = vm::loadGameFromSlot,
                    onDeleteSlot = vm::deleteSaveSlot,
                    onOpenNewGame = { forceSetupMode = true }
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LeftIconRail(
                        currentNav = currentNav,
                        unreadMailCount = unreadMails,
                        unreadAlertsCount = unreadAlerts,
                        onSelect = vm::navigateTo
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .navigationBarsPadding()
                    ) {
                        when (currentNav) {
                            NavDestination.HOME -> SocialFeedScreen(
                                state = state,
                                onCreatePost = vm::createSocialPost,
                                onLikePost = vm::toggleLikePost,
                                onRepostPost = vm::toggleRepost,
                                onVotePoll = vm::voteInPoll,
                                onAddReply = vm::addReplyToPost,
                                onInspectNpc = vm::inspectNpc
                            )
                            NavDestination.EXPLORER -> ExplorerScreen(
                                state = state,
                                initialTab = 0,
                                onInspectNpc = vm::inspectNpc
                            )
                            NavDestination.RANKING -> ExplorerScreen(
                                state = state,
                                initialTab = 2,
                                onInspectNpc = vm::inspectNpc
                            )
                            NavDestination.WORLD_MAP -> WorldMapScreen(
                                state = state,
                                selectedProvinceId = selectedProvinceId,
                                selectedCountryIso = selectedCountryIso,
                                onSelectProvince = vm::selectProvince,
                                onSelectCountry = vm::selectCountry,
                                onTravelToRegion = vm::travelToRegion,
                                onProposeSummit = vm::proposeSummitDeal
                            )
                            NavDestination.ECONOMY -> EconomyScreen(
                                state = state,
                                initialSubRoute = null,
                                onFoundCompany = vm::foundCompany,
                                onHireNpc = vm::hireNpcForCompany,
                                onBuyStock = vm::buyStock,
                                onSellStock = vm::sellStock,
                                onSellCommodity = vm::sellCommodityOnMarket,
                                onCreateContract = vm::createTradeContract,
                                onBuildSubstationOrUpgrade = vm::buildSubstationOrUpgrade,
                                onBuildTransmission = vm::buildTransmissionLine,
                                onUpdateCentralBank = vm::updateCentralBankPolicy,
                                onBuyBond = vm::buyBond,
                                onIssueBilateralLoan = vm::issueBilateralLoan,
                                onOpenDerivative = vm::openDerivative,
                                onCloseDerivative = vm::closeDerivative,
                                onRegisterPatent = vm::registerPatent,
                                onSwapForex = vm::swapForex
                            )
                            NavDestination.MARKET -> EconomyScreen(
                                state = state,
                                initialSubRoute = EconomySubRoute.B2C_MARKET,
                                onFoundCompany = vm::foundCompany,
                                onHireNpc = vm::hireNpcForCompany,
                                onBuyStock = vm::buyStock,
                                onSellStock = vm::sellStock,
                                onSellCommodity = vm::sellCommodityOnMarket,
                                onCreateContract = vm::createTradeContract,
                                onBuildSubstationOrUpgrade = vm::buildSubstationOrUpgrade,
                                onBuildTransmission = vm::buildTransmissionLine,
                                onUpdateCentralBank = vm::updateCentralBankPolicy,
                                onBuyBond = vm::buyBond,
                                onIssueBilateralLoan = vm::issueBilateralLoan,
                                onOpenDerivative = vm::openDerivative,
                                onCloseDerivative = vm::closeDerivative,
                                onRegisterPatent = vm::registerPatent,
                                onSwapForex = vm::swapForex
                            )
                            NavDestination.ORGANIZATIONS -> OrganizationsScreen(
                                state = state,
                                onApplyJob = vm::applyForJob,
                                onFoundCompany = vm::foundCompany,
                                onFoundParty = vm::foundPoliticalParty
                            )
                            NavDestination.SUPRANATIONAL -> SupranationalOrgsScreen(state = state)
                            NavDestination.RULES -> RulesAndAdminScreen(
                                state = state,
                                initialSubRoute = null,
                                onAppointMinister = vm::appointMinister,
                                onProposeLaw = vm::proposeLawBill,
                                onProposeConstitution = vm::proposeConstitutionalReform,
                                onCastVote = vm::castParliamentVote,
                                onUpdatePlanning = vm::updateCentralPlanning,
                                onBuildSubstationOrUpgrade = vm::buildSubstationOrUpgrade,
                                onBuildTransmission = vm::buildTransmissionLine
                            )
                            NavDestination.ADMINISTRATION -> RulesAndAdminScreen(
                                state = state,
                                initialSubRoute = RulesAdminSubRoute.CABINET_MINISTRIES,
                                onAppointMinister = vm::appointMinister,
                                onProposeLaw = vm::proposeLawBill,
                                onProposeConstitution = vm::proposeConstitutionalReform,
                                onCastVote = vm::castParliamentVote,
                                onUpdatePlanning = vm::updateCentralPlanning,
                                onBuildSubstationOrUpgrade = vm::buildSubstationOrUpgrade,
                                onBuildTransmission = vm::buildTransmissionLine
                            )
                            NavDestination.SOCIETY -> SocietyScreen(state = state)
                            NavDestination.TUTORIAL -> TutorialScreen()
                            NavDestination.CONFLICT -> ConflictScreen(
                                state = state,
                                onLaunchCovertOp = vm::launchCovertOp,
                                onLaunchConflict = vm::launchConflictAction,
                                onBuyHardware = vm::buyMilitaryHardware,
                                onHirePmc = vm::hirePmcBattalion
                            )
                            NavDestination.INBOX -> InboxScreen(
                                state = state,
                                onRespondMail = vm::respondToMail,
                                onSendDirectMessage = vm::sendDirectMessage
                            )
                            NavDestination.NOTIFICATIONS -> NotificationsScreen(
                                state = state,
                                onMarkAllRead = vm::markAllNotificationsRead
                            )
                            NavDestination.AWARDS -> AwardsScreen(state = state)
                            NavDestination.PERSONAL -> PersonalHubScreen(
                                state = state,
                                onClaimWorkDiscipline = vm::claimWorkDiscipline,
                                onStartTraining = vm::startStatTraining,
                                onStudy = { vm.studyUniversity(it) },
                                onBoostAraucitas = vm::boostUniversityWithAraucitas,
                                onStartResearch = vm::startKnowledgeResearch,
                                onBuyRealEstate = vm::buyRealEstate
                            )
                            NavDestination.PROFILE -> ProfileScreen(
                                state = state,
                                onClaimWorkDiscipline = vm::claimWorkDiscipline,
                                onStartTraining = vm::startStatTraining,
                                onOpenUniversity = { vm.navigateTo(NavDestination.UNIVERSITY) },
                                onOpenKnowledge = { vm.navigateTo(NavDestination.KNOWLEDGE) }
                            )
                            NavDestination.UNIVERSITY -> UniversityScreen(
                                state = state,
                                onStudy = { vm.studyUniversity(it) },
                                onBoostAraucitas = vm::boostUniversityWithAraucitas
                            )
                            NavDestination.KNOWLEDGE -> KnowledgeTreeScreen(
                                state = state,
                                onStartResearch = vm::startKnowledgeResearch
                            )
                            NavDestination.MORE -> MoreGridScreen(
                                state = state,
                                onNavigate = vm::navigateTo
                            )
                            NavDestination.SETTINGS -> SettingsScreen(
                                state = state,
                                saveSlots = saveSlots,
                                onSpeedSelected = vm::setGameSpeed,
                                onSaveToSlot = vm::saveGameToSlot,
                                onLoadFromSlot = vm::loadGameFromSlot,
                                onDeleteSlot = vm::deleteSaveSlot,
                                onOpenNewCampaign = { forceSetupMode = true }
                            )
                        }
                    }
                }
            }

            // Crisp floating OLED toast banner
            toastMsg?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, start = 68.dp, end = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OledElevated)
                        .border(1.dp, AccentCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }

            // NPC Inspector Modal
            inspectedNpcId?.let { id ->
                val npc = state.npcs.firstOrNull { it.id == id }
                if (npc != null) {
                    NpcProfileModal(
                        npc = npc,
                        state = state,
                        isFollowing = npc.id in state.player.followingNpcIds,
                        onToggleFollow = { vm.toggleFollowNpc(npc.id) },
                        onDismiss = { vm.inspectNpc(null) }
                    )
                }
            }
        }
    }
}
