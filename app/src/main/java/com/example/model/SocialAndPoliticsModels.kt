package com.example.model

import com.squareup.moshi.JsonClass

enum class TraitTier(val label: String) {
    LOW("Entry Citizen (10–35)"),
    MID("Mid-Tier Pro (36–70)"),
    HIGH("Elite Prodigy (71–99)")
}

enum class NpcAmbition(val label: String) {
    OLIGARCH("Corporate Oligarch"),
    POLITICAL_CLIMBER("Parliamentary Climber"),
    MINISTERIAL_HAWK("Ministerial Strategist"),
    REVOLUTIONARY("Regime Challenger"),
    INDUSTRIAL_TYCOON("Industrial Tycoon"),
    FINANCIER("Market Speculator"),
    DIPLOMAT("Global Envoy")
}

@JsonClass(generateAdapter = true)
data class NpcCharacter(
    val id: String,
    val name: String,
    val handle: String,
    val countryIso: String,
    val regionId: String,
    val traitTier: TraitTier,
    val traits: List<String>,
    val ambition: NpcAmbition,
    val level: Int,
    val intelligence: Int,
    val strength: Int,
    val endurance: Int,
    val efficiency: Int,
    val cashUsd: Double,
    val netWorthUsd: Double,
    val partyId: String? = null,
    val officeHeld: MinistryTag? = null,
    val isHeadOfState: Boolean = false,
    val isParliamentMember: Boolean = false,
    val ownedCompanyIds: List<String> = emptyList(),
    val jobCompanyId: String? = null,
    val lastAction: String = "Monitoring domestic markets",
    val avatarSeed: Int
)

@JsonClass(generateAdapter = true)
data class PollOption(
    val id: String,
    val text: String,
    val votes: Int = 0
)

@JsonClass(generateAdapter = true)
data class SocialReply(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val timestampStr: String
)

@JsonClass(generateAdapter = true)
data class SocialPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val authorBadge: String?,
    val countryIso: String,
    val content: String,
    val attachmentTag: String? = null, // e.g., "CHART:NVDA", "DECREE", "POLL"
    val timestampStr: String,
    val likes: Int = 0,
    val reposts: Int = 0,
    val likedByPlayer: Boolean = false,
    val repostedByPlayer: Boolean = false,
    val pollOptions: List<PollOption> = emptyList(),
    val playerVotedOptionId: String? = null,
    val replies: List<SocialReply> = emptyList()
)

enum class MailType {
    PARLIAMENT_INVITE,
    MINISTRY_OFFER,
    PARTY_COFOUND_INVITE,
    NOBLE_TITLE_GRANT,
    TRADE_PROPOSAL,
    SUMMIT_INVITATION,
    SYSTEM_DISPATCH
}

@JsonClass(generateAdapter = true)
data class OfficialMail(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val countryIso: String,
    val subject: String,
    val body: String,
    val type: MailType,
    val ministryTag: MinistryTag? = null,
    val partyId: String? = null,
    val nobleTitle: String? = null,
    val timestampStr: String,
    val isRead: Boolean = false,
    val status: String = "PENDING" // PENDING, ACCEPTED, DECLINED
)

@JsonClass(generateAdapter = true)
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val category: String, // ECONOMY, POLITICS, MILITARY, SYSTEM
    val timestampStr: String,
    val isRead: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PoliticalParty(
    val id: String,
    val name: String,
    val acronym: String,
    val countryIso: String,
    val ideology: String,
    val leaderId: String,
    val seatsCount: Int,
    val memberIds: List<String>,
    val treasuryUsd: Double,
    val approvalPct: Double
)

@JsonClass(generateAdapter = true)
data class LawItem(
    val id: String,
    val code: String,
    val title: String,
    val category: String, // Fiscal, Labor, Trade, Defense, Energy, Civil, Justice, Monetary
    val requiredMinistry: MinistryTag,
    val currentValue: String,
    val proposedOptions: List<String>,
    val description: String
)

@JsonClass(generateAdapter = true)
data class ConstitutionalChapter(
    val chapterNumber: Int,
    val title: String,
    val currentProvision: String,
    val proposedProvision: String,
    val isReformed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ParliamentBill(
    val id: String,
    val countryIso: String,
    val lawId: String?,
    val chapterNumber: Int?,
    val title: String,
    val proposerId: String,
    val proposerName: String,
    val currentText: String,
    val proposedText: String,
    val votesFor: Int,
    val votesAgainst: Int,
    val votesAbstain: Int,
    val playerVoted: String? = null, // "FOR", "AGAINST", "ABSTAIN"
    val hoursRemaining: Int,
    val status: String // "ACTIVE", "PASSED", "REJECTED"
)

@JsonClass(generateAdapter = true)
data class GameState(
    val campaignStarted: Boolean = false,
    val startingMode: StartingMode = StartingMode.CAREER,
    val date: GameDate = GameDate(),
    val speed: GameSpeed = GameSpeed.X1,
    val player: PlayerCharacter = PlayerCharacter(),
    val countries: List<Country> = emptyList(),
    val provinces: List<Province> = emptyList(),
    val npcs: List<NpcCharacter> = emptyList(),
    val companies: List<Company> = emptyList(),
    val jobListings: List<JobListing> = emptyList(),
    val stocks: List<StockAsset> = emptyList(),
    val contracts: List<TradeContract> = emptyList(),
    val powerPlants: List<PowerPlantNode> = emptyList(),
    val transmissionLines: List<TransmissionLine> = emptyList(),
    val substations: List<SubstationNode> = emptyList(),
    val parties: List<PoliticalParty> = emptyList(),
    val laws: List<LawItem> = emptyList(),
    val constitutionalChapters: List<ConstitutionalChapter> = emptyList(),
    val activeBills: List<ParliamentBill> = emptyList(),
    val supranationalOrgs: List<SupranationalOrg> = emptyList(),
    val covertOps: List<CovertOperation> = emptyList(),
    val conflicts: List<MilitaryConflict> = emptyList(),
    val socialPosts: List<SocialPost> = emptyList(),
    val officialMails: List<OfficialMail> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val knowledgeNodes: List<KnowledgeNode> = emptyList(),
    val playerInventory: Map<String, Int> = emptyMap(), // CommodityGood.name -> qty
    val lastEventBanner: String = "World Order Simulation Ready • Offline Single-Player Engine Active"
)
