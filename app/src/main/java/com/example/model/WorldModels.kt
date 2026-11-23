package com.example.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Country(
    val iso: String,
    val name: String,
    val capital: String,
    val continent: String,
    val currencyCode: String,
    val currencySymbol: String,
    val forexPerUsd: Double, // How many local units per 1 USD
    val govType: GovType,
    val economicSystem: EconomicSystem,
    val powerStructure: PowerStructure,
    val upperChamberName: String,
    val lowerChamberName: String,
    val centralBankName: String,
    val intelAgencyName: String,
    val gdpBillionsUsd: Double,
    val populationMillions: Double,
    val treasuryBillionsUsd: Double,
    val stability: Int, // 0..100
    val inflationPct: Double,
    val taxRatePct: Double,
    val interestRatePct: Double,
    val reserveRatioPct: Double,
    val m0BillionsUsd: Double,
    val m1BillionsUsd: Double,
    val swiftConnected: Boolean,
    val bilateralRelation: Int, // -100..+100 relative to player's home country
    val visaExemption: Boolean,
    val allianceBloc: String,
    val nationalAgenda: String,
    val mapX: Float, // Normalized 0f..1f on equirectangular world map
    val mapY: Float, // Normalized 0f..1f
    val headOfStateNpcId: String? = null,
    val ministerAssignments: Map<String, String> = emptyMap(), // MinistryTag.name -> characterId ("player_01" or npcId)
    val rationQuotaKgPerPop: Int = 0, // Used in Central Planning Committee
    val wageFloorMultiplier: Double = 1.0,
    val fiveYearQuotaTargetGdp: Double = 0.0,
    val spyNetworkStrength: Int = 0 // Player's covert network 0..100 in this country
)

@JsonClass(generateAdapter = true)
data class Province(
    val id: String,
    val name: String,
    val countryIso: String,
    val capitalCity: String,
    val isMicroStateOrEnclave: Boolean = false,
    val populationMillions: Double,
    val mapX: Float, // 0f..1f
    val mapY: Float, // 0f..1f
    val localPopsCount: Int,
    val whiteMarketIndex: Double, // 1.0 = baseline
    val blackMarketPremium: Double, // e.g., 1.35x
    val powerDemandMw: Int,
    val powerSupplyMw: Int,
    val gridConnected: Boolean = true,
    val goodsPricesLocal: Map<String, Double> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class SupranationalOrg(
    val id: String,
    val name: String,
    val acronym: String,
    val focus: String,
    val memberIsos: List<String>,
    val treasuryBillions: Double,
    val activeResolution: String
)

@JsonClass(generateAdapter = true)
data class CovertOperation(
    val id: String,
    val targetCountryIso: String,
    val type: CovertOpType,
    val status: String,
    val successChancePct: Int,
    val costUsd: Double,
    val launchedDay: Int
)

enum class CovertOpType(val label: String, val requiredMinistry: MinistryTag) {
    BUILD_SPY_NETWORK("Establish Spy Network", MinistryTag.INTELLIGENCE),
    COUP_INSTIGATION("Instigate Regime Coup", MinistryTag.INTELLIGENCE),
    CURRENCY_SHORT_SABOTAGE("Currency Short-Selling Sabotage", MinistryTag.INTELLIGENCE),
    ARMS_SMUGGLING("Black-Market Arms Smuggling", MinistryTag.DEFENSE),
    COUNTER_INTEL_PURGE("Counter-Intelligence Purge", MinistryTag.SECURITY)
}

@JsonClass(generateAdapter = true)
data class MilitaryConflict(
    val id: String,
    val aggressorIso: String,
    val defenderIso: String,
    val intensity: String, // Trade War, Proxy Skirmish, Border Clash, Full Mobilization
    val aggressorStrength: Int,
    val defenderStrength: Int,
    val frontlineProvinceId: String,
    val startedDay: Int
)

@JsonClass(generateAdapter = true)
data class SummitOffer(
    val id: String,
    val targetCountryIso: String,
    val visaExemptionProposed: Boolean,
    val armsDealMillionsUsd: Double,
    val currencySwapMillionsUsd: Double,
    val foreignAidMillionsUsd: Double,
    val allianceInvite: Boolean,
    val counterOfferMessage: String = ""
)
