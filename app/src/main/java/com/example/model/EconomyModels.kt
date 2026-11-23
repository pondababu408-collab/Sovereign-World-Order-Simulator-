package com.example.model

import com.squareup.moshi.JsonClass

enum class BusinessType(
    val displayName: String,
    val baseCostUsd: Double,
    val dailyOutputUnit: String,
    val sector: String
) {
    FARM("Agri-Industrial Farm", 2_500.0, "Grain & Food (t)", "Primary"),
    MINE_URANIUM("Uranium Enrichment Mine", 18_000.0, "Yellowcake Uranium (kg)", "Mining"),
    MINE_COAL("Thermal Coal Mine", 7_500.0, "Coal (t)", "Mining"),
    MINE_GAS("Natural Gas Extraction", 12_000.0, "LNG (m³)", "Mining"),
    MINE_IRON("Iron Ore Quarry", 6_500.0, "Iron Ore (t)", "Mining"),
    FACTORY("Heavy Manufacturing Plant", 15_000.0, "Industrial Goods", "Industry"),
    SEMICONDUCTOR_PLANT("Semiconductor Fab", 45_000.0, "Silicon Wafers", "High-Tech"),
    DEFENSE_PLANT("Defense & Munitions Plant", 55_000.0, "Armaments", "Defense"),
    COMMERCIAL_BANK("Commercial Bank", 30_000.0, "Credit & Reserves", "Finance"),
    MEDIA_CORP("Media & Broadcast Network", 10_000.0, "Influence Reach", "Media"),
    POWER_PLANT("Power Generation Plant", 22_000.0, "Electricity (MWh)", "Energy")
}

enum class PowerFuel(val displayName: String) {
    NUCLEAR("Nuclear (Uranium)"),
    COAL("Coal Thermal"),
    GAS("Combined-Cycle Gas"),
    SOLAR("Solar Index"),
    WIND("Wind Turbine Index")
}

@JsonClass(generateAdapter = true)
data class Company(
    val id: String,
    val name: String,
    val ticker: String,
    val ownerId: String, // "player_01", npcId, or "STATE_<ISO>"
    val countryIso: String,
    val regionId: String,
    val type: BusinessType,
    val powerFuel: PowerFuel? = null,
    val cashUsd: Double,
    val valuationUsd: Double,
    val totalShares: Long = 10_000L,
    val playerShares: Long = 0L,
    val stateShares: Long = 0L,
    val efficiency: Int = 55,
    val employeesCount: Int = 5,
    val dailyProfitUsd: Double = 0.0,
    val warehouseInventory: Map<String, Int> = emptyMap(),
    val fixedPriceUsd: Double? = null, // Set by Central Planning Committee
    val isStateOwnedEnterprise: Boolean = false
)

@JsonClass(generateAdapter = true)
data class JobListing(
    val id: String,
    val companyId: String,
    val companyName: String,
    val countryIso: String,
    val regionId: String,
    val roleTitle: String,
    val salaryPerPointUsd: Double, // Multiplied by worker Efficiency/Intelligence
    val minIntelligence: Int,
    val isStateJob: Boolean = false,
    val filledById: String? = null
)

@JsonClass(generateAdapter = true)
data class CandlePoint(
    val day: Int,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
)

@JsonClass(generateAdapter = true)
data class StockAsset(
    val ticker: String,
    val name: String,
    val countryIso: String,
    val sector: String,
    val currentPriceUsd: Double,
    val previousPriceUsd: Double,
    val totalShares: Long,
    val playerShares: Long,
    val npcMajorityHolderId: String?,
    val dividendYieldPct: Double,
    val isCrypto: Boolean = false,
    val linkedCompanyId: String? = null,
    val candles: List<CandlePoint> = emptyList(),
    val bidPriceUsd: Double,
    val askPriceUsd: Double
) {
    val playerOwnershipPct: Double
        get() = if (totalShares > 0) (playerShares.toDouble() / totalShares.toDouble()) * 100.0 else 0.0
    val hasMajorityControl: Boolean
        get() = playerOwnershipPct > 50.0
    val changePct: Double
        get() = if (previousPriceUsd > 0.0) ((currentPriceUsd - previousPriceUsd) / previousPriceUsd) * 100.0 else 0.0
}

enum class CommodityGood(
    val displayName: String,
    val basePriceUsd: Double,
    val isBlackMarketPreferred: Boolean = false
) {
    GRAIN("Grain & Rations", 120.0),
    COAL("Thermal Coal", 180.0),
    GAS("Natural Gas", 260.0),
    IRON("Refined Steel & Iron", 240.0),
    URANIUM("Enriched Uranium", 1_450.0, true),
    SEMICONDUCTORS("Advanced Chips", 2_200.0),
    ARMAMENTS("Heavy Armaments", 3_800.0, true),
    CONSUMER_GOODS("Industrial Goods", 340.0)
}

@JsonClass(generateAdapter = true)
data class TradeContract(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val buyerCountryIso: String,
    val sellerCountryIso: String,
    val good: CommodityGood,
    val quantity: Int,
    val unitPriceUsd: Double,
    val marketPriceUsd: Double,
    val isBlackMarket: Boolean,
    val settleInBitcoin: Boolean,
    val activeDaysRemaining: Int,
    val status: String // "ACTIVE", "COMPLETED", "DISRUPTED"
)

@JsonClass(generateAdapter = true)
data class PowerPlantNode(
    val id: String,
    val companyId: String,
    val companyName: String,
    val countryIso: String,
    val regionId: String,
    val fuel: PowerFuel,
    val capacityMw: Int,
    val activeOutputMw: Int
)

@JsonClass(generateAdapter = true)
data class TransmissionLine(
    val id: String,
    val fromRegionId: String,
    val toRegionId: String,
    val ownerCompanyId: String,
    val maxTransferMw: Int
)

@JsonClass(generateAdapter = true)
data class SubstationNode(
    val id: String,
    val regionId: String,
    val companyId: String,
    val companyName: String,
    val level: Int,
    val capacityMw: Int
)
