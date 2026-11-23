package com.example.data

import com.example.model.*
import kotlin.math.abs

object WorldCatalog {

    fun buildAllCountries(playerHomeIso: String): List<Country> {
        return (countrySeedsPart1 + countrySeedsPart2).map { it.toCountry(playerHomeIso) }
    }

    fun buildProvinces(countries: List<Country>): List<Province> {
        val list = mutableListOf<Province>()
        // Dedicated key sub-regions & enclaves
        val customRegions = listOf(
            Province("IN_DL", "National Capital Territory of Delhi", "IN", "New Delhi", false, 33.8, 0.712f, 0.352f, 18, 1.0, 1.28, 6200, 6800, true, defaultGoodsPrices()),
            Province("IN_MH", "Maharashtra", "IN", "Mumbai", false, 126.0, 0.702f, 0.392f, 24, 1.06, 1.32, 14500, 15200, true, defaultGoodsPrices(1.04)),
            Province("IN_KA", "Karnataka", "IN", "Bengaluru", false, 68.0, 0.714f, 0.415f, 16, 1.03, 1.25, 8900, 9600, true, defaultGoodsPrices(1.02)),
            Province("IN_GJ", "Gujarat", "IN", "Gandhinagar", false, 71.0, 0.695f, 0.375f, 15, 0.98, 1.24, 9200, 10800, true, defaultGoodsPrices(0.97)),
            Province("US_DC", "District of Columbia", "US", "Washington, D.C.", false, 0.7, 0.278f, 0.286f, 10, 1.12, 1.35, 3100, 3500, true, defaultGoodsPrices(1.10)),
            Province("US_NY", "New York", "US", "New York City", false, 19.6, 0.285f, 0.272f, 22, 1.15, 1.40, 18200, 19400, true, defaultGoodsPrices(1.14)),
            Province("US_CA", "California", "US", "Sacramento", false, 39.0, 0.168f, 0.295f, 24, 1.18, 1.38, 26500, 27900, true, defaultGoodsPrices(1.15)),
            Province("US_TX", "Texas", "US", "Austin", false, 30.5, 0.215f, 0.325f, 19, 1.02, 1.28, 34000, 36500, true, defaultGoodsPrices(0.98)),
            Province("CN_BJ", "Beijing Municipality", "CN", "Beijing", false, 21.8, 0.805f, 0.288f, 18, 1.02, 1.42, 14800, 16200, true, defaultGoodsPrices(0.96)),
            Province("CN_SH", "Shanghai", "CN", "Shanghai", false, 24.8, 0.818f, 0.328f, 20, 1.08, 1.38, 17900, 19200, true, defaultGoodsPrices(1.01)),
            Province("CN_GD", "Guangdong", "CN", "Guangzhou", false, 126.5, 0.798f, 0.365f, 22, 1.00, 1.34, 28500, 30100, true, defaultGoodsPrices(0.94)),
            Province("FR_IDF", "Île-de-France", "FR", "Paris", false, 12.3, 0.505f, 0.232f, 18, 1.10, 1.30, 11200, 13400, true, defaultGoodsPrices(1.08)),
            Province("FR_ARA", "Auvergne-Rhône-Alpes", "FR", "Lyon", false, 8.1, 0.512f, 0.246f, 12, 1.04, 1.26, 8400, 11200, true, defaultGoodsPrices(1.02)),
            Province("ES_MD", "Community of Madrid", "ES", "Madrid", false, 6.8, 0.487f, 0.274f, 15, 1.05, 1.29, 6400, 7100, true, defaultGoodsPrices(1.02)),
            Province("ES_CT", "Catalonia", "ES", "Barcelona", false, 7.9, 0.500f, 0.268f, 16, 1.07, 1.32, 7100, 7600, true, defaultGoodsPrices(1.04)),
            Province("ES_CE", "Ceuta (Enclave)", "ES", "Ceuta", true, 0.08, 0.484f, 0.300f, 6, 0.96, 1.48, 420, 480, true, defaultGoodsPrices(0.95)),
            Province("ES_ML", "Melilla (Enclave)", "ES", "Melilla", true, 0.08, 0.491f, 0.302f, 6, 0.96, 1.48, 410, 460, true, defaultGoodsPrices(0.95)),
            Province("AD_AD", "Andorra (Pyrenees)", "AD", "Andorra la Vella", true, 0.08, 0.503f, 0.264f, 6, 0.95, 1.30, 520, 610, true, defaultGoodsPrices(0.92)),
            Province("GI_GI", "Gibraltar (Enclave)", "GI", "Gibraltar", true, 0.03, 0.484f, 0.296f, 6, 1.06, 1.44, 380, 420, true, defaultGoodsPrices(1.05)),
            Province("SG_SG", "Singapore City-State", "SG", "Singapore", true, 5.9, 0.788f, 0.495f, 14, 1.12, 1.26, 6800, 7400, true, defaultGoodsPrices(1.08)),
            Province("RU_MOW", "Moscow Federal City", "RU", "Moscow", false, 13.1, 0.605f, 0.175f, 18, 0.98, 1.45, 15200, 17800, true, defaultGoodsPrices(0.93)),
            Province("RU_KGD", "Kaliningrad (Exclave)", "RU", "Kaliningrad", true, 1.0, 0.556f, 0.196f, 7, 0.94, 1.50, 1200, 1450, true, defaultGoodsPrices(0.95)),
            Province("CU_HA", "La Habana", "CU", "Havana", false, 2.1, 0.266f, 0.364f, 12, 0.85, 1.65, 1800, 1650, true, defaultGoodsPrices(0.88)),
            Province("CU_SC", "Santiago de Cuba", "CU", "Santiago", false, 1.0, 0.280f, 0.372f, 8, 0.82, 1.68, 950, 880, true, defaultGoodsPrices(0.85))
        )
        list.addAll(customRegions)

        val coveredIsos = customRegions.map { it.countryIso }.toSet()
        countries.forEach { c ->
            if (c.iso !in coveredIsos) {
                val h = abs(c.iso.hashCode())
                val demand = (c.gdpBillionsUsd * 8.5).toInt().coerceIn(250, 28000)
                val supply = (demand * (0.95 + (h % 25) * 0.01)).toInt()
                val isMicro = c.populationMillions < 0.5
                list.add(
                    Province(
                        id = "${c.iso}_CAP",
                        name = "${c.capital} Capital Region",
                        countryIso = c.iso,
                        capitalCity = c.capital,
                        isMicroStateOrEnclave = isMicro,
                        populationMillions = (c.populationMillions * 0.35).coerceAtLeast(0.01),
                        mapX = c.mapX,
                        mapY = c.mapY,
                        localPopsCount = (6 + (h % 14)),
                        whiteMarketIndex = 0.92 + (h % 26) * 0.01,
                        blackMarketPremium = 1.24 + (h % 35) * 0.01,
                        powerDemandMw = demand,
                        powerSupplyMw = supply,
                        gridConnected = true,
                        goodsPricesLocal = defaultGoodsPrices(0.90 + (h % 25) * 0.01)
                    )
                )
            }
        }
        return list
    }

    private fun defaultGoodsPrices(mult: Double = 1.0): Map<String, Double> {
        return CommodityGood.entries.associate { it.name to (it.basePriceUsd * mult) }
    }

    fun buildInitialStocks(): List<StockAsset> {
        data class SeedStock(
            val ticker: String, val name: String, val iso: String,
            val sector: String, val price: Double, val shares: Long, val div: Double, val crypto: Boolean = false
        )
        val seeds = listOf(
            SeedStock("RELIANCE", "Reliance Industries", "IN", "Energy & Telecom", 34.80, 1_000_000L, 1.4),
            SeedStock("TCS", "Tata Consultancy Services", "IN", "IT & Enterprise", 49.20, 800_000L, 2.1),
            SeedStock("AAPL", "Apple Inc.", "US", "Consumer Tech", 228.50, 1_500_000L, 0.5),
            SeedStock("NVDA", "NVIDIA Corp.", "US", "Semiconductors & AI", 146.40, 2_000_000L, 0.1),
            SeedStock("LMT", "Lockheed Martin", "US", "Aerospace & Defense", 542.00, 350_000L, 2.4),
            SeedStock("MC", "LVMH Moët Hennessy", "FR", "Luxury Conglomerate", 685.00, 450_000L, 1.9),
            SeedStock("AIR", "Airbus SE", "FR", "Aerospace & Aviation", 162.30, 600_000L, 1.5),
            SeedStock("BBVA", "Banco Bilbao Vizcaya", "ES", "Commercial Banking", 11.40, 2_500_000L, 4.8),
            SeedStock("SAN", "Banco Santander", "ES", "Global Banking", 5.20, 4_000_000L, 4.2),
            SeedStock("IBE", "Iberdrola S.A.", "ES", "Electrical Grids", 14.90, 2_200_000L, 4.1),
            SeedStock("TM", "Toyota Motor Corp.", "JP", "Automotive & Robotics", 188.60, 1_100_000L, 2.6),
            SeedStock("ARAMCO", "Saudi Aramco", "SA", "Hydrocarbons", 7.85, 5_000_000L, 5.4),
            SeedStock("BTC", "Bitcoin (BTC)", "GLOBAL", "Decentralized Crypto", 94_200.00, 21_000_000L, 0.0, true)
        )

        return seeds.mapIndexed { idx, s ->
            val candles = mutableListOf<CandlePoint>()
            var prev = s.price * 0.92
            for (d in 1..18) {
                val drift = (((idx * 7 + d * 13) % 11) - 5) * 0.008
                val close = (prev * (1.0 + drift)).coerceAtLeast(1.0)
                val high = maxOf(prev, close) * 1.018
                val low = minOf(prev, close) * 0.984
                candles.add(CandlePoint(d, prev, high, low, close))
                prev = close
            }
            candles.add(CandlePoint(19, prev, maxOf(prev, s.price) * 1.01, minOf(prev, s.price) * 0.99, s.price))
            StockAsset(
                ticker = s.ticker,
                name = s.name,
                countryIso = s.iso,
                sector = s.sector,
                currentPriceUsd = s.price,
                previousPriceUsd = prev,
                totalShares = s.shares,
                playerShares = 0L,
                npcMajorityHolderId = null,
                dividendYieldPct = s.div,
                isCrypto = s.crypto,
                candles = candles,
                bidPriceUsd = s.price * 0.997,
                askPriceUsd = s.price * 1.003
            )
        }
    }

    fun buildAllLaws(): List<LawItem> = listOf(
        LawItem("L01", "TAX-01", "Corporate Income Tax Rate", "Fiscal", MinistryTag.TREASURY, "25% Flat", listOf("12% Pro-Business", "25% Flat", "40% Progressive"), "Sets corporate profit taxation."),
        LawItem("L02", "TAX-02", "Personal Income Tax", "Fiscal", MinistryTag.TREASURY, "20% Tiered", listOf("10% Low Tax", "20% Tiered", "45% Wealth Tax"), "Affects Pop savings and consumption."),
        LawItem("L03", "TAX-03", "Value Added Tax (VAT)", "Fiscal", MinistryTag.TREASURY, "15% Standard", listOf("5% Essential", "15% Standard", "24% High VAT"), "Regulates domestic retail prices."),
        LawItem("L04", "TAX-04", "Capital Gains Levy", "Fiscal", MinistryTag.TREASURY, "15%", listOf("0% Tax Haven", "15%", "35% Speculation Tax"), "Taxes stock and crypto trading profits."),
        LawItem("L05", "TAX-05", "Sovereign Wealth Allocation", "Fiscal", MinistryTag.TREASURY, "5% Surplus", listOf("0% None", "5% Surplus", "20% Aggressive SWF"), "Channels treasury into SOE share acquisitions."),
        LawItem("L06", "MON-01", "Central Bank Mandate", "Monetary", MinistryTag.CENTRAL_BANK, "Dual Mandate", listOf("Price Stability Only", "Dual Mandate", "State Credit Direction"), "Defines monetary policy independence."),
        LawItem("L07", "MON-02", "Crypto Legal Status", "Monetary", MinistryTag.CENTRAL_BANK, "Taxed Asset", listOf("Legal Tender", "Taxed Asset", "Banned / Black Market Only"), "Controls Bitcoin legality on domestic exchanges."),
        LawItem("L08", "MON-03", "Capital Controls", "Monetary", MinistryTag.CENTRAL_BANK, "Free Flow", listOf("Free Flow", "10% Exit Fee", "Strict Peg & Permit"), "Limits foreign currency conversion."),
        LawItem("L09", "MON-04", "Fractional Reserve Floor", "Monetary", MinistryTag.CENTRAL_BANK, "10%", listOf("4% Expansionary", "10%", "25% Conservative"), "Governs commercial bank lending capacity."),
        LawItem("L10", "MON-05", "SWIFT Gateway Policy", "Monetary", MinistryTag.CENTRAL_BANK, "Open SWIFT", listOf("Open SWIFT", "Screened Transfers", "Bilateral Clearing Only"), "Controls international payment routing."),
        LawItem("L11", "ECO-01", "Economic System Charter", "Economy", MinistryTag.ECONOMY, "Mixed Economy", listOf("Free Market", "Mixed Economy", "State Capitalism", "Planned Economy"), "Shifts national model; unlocks Central Planning if Planned."),
        LawItem("L12", "ECO-02", "Industrial Subsidies", "Economy", MinistryTag.ECONOMY, "Targeted Tech", listOf("None", "Targeted Tech", "Heavy Manufacturing Blitz"), "Boosts semiconductor and factory output."),
        LawItem("L13", "ECO-03", "Import Tariffs", "Economy", MinistryTag.ECONOMY, "8% MFN", listOf("0% Free Trade", "8% MFN", "35% Protectionist"), "Adjusts international contract duties."),
        LawItem("L14", "ECO-04", "Antitrust Ownership Cap", "Economy", MinistryTag.ECONOMY, "No Cap", listOf("No Cap", "49% Foreign Cap", "State Majority Required"), "Restricts foreign or private >50% control."),
        LawItem("L15", "ECO-05", "Special Economic Zones", "Economy", MinistryTag.ECONOMY, "Coastal SEZs", listOf("Disabled", "Coastal SEZs", "Nationwide Deregulated"), "Accelerates new company startups."),
        LawItem("L16", "LAB-01", "Minimum Wage Statute", "Labor", MinistryTag.ECONOMY, "Regional Index", listOf("Market Determined", "Regional Index", "High Living Wage"), "Affects job listing baseline pay."),
        LawItem("L17", "LAB-02", "Workweek Standard", "Labor", MinistryTag.ECONOMY, "40 Hours", listOf("35 Hours", "40 Hours", "48 Hours Crunch"), "Balances Pop productivity and morale."),
        LawItem("L18", "LAB-03", "Trade Union Rights", "Labor", MinistryTag.SECURITY, "Legal Bargaining", listOf("Banned", "Legal Bargaining", "Co-Determination Boards"), "Influences strikes and stability."),
        LawItem("L19", "ENE-01", "Nuclear Power Directive", "Energy", MinistryTag.ENERGY, "Authorized", listOf("Moratorium", "Authorized", "Fast-Track Reactors"), "Allows nuclear plants on regional grids."),
        LawItem("L20", "ENE-02", "Carbon & Coal Levy", "Energy", MinistryTag.ENERGY, "$25/ton", listOf("$0 Exempt", "$25/ton", "$90/ton Decarbonize"), "Impacts Coal and Gas plant profitability."),
        LawItem("L21", "ENE-03", "Grid Transmission Open Access", "Energy", MinistryTag.ENERGY, "Regulated Wheeling", listOf("State Monopoly", "Regulated Wheeling", "Private Merchant Grids"), "Governs regional electrical transmission lines."),
        LawItem("L22", "ENE-04", "Strategic Fuel Reserves", "Energy", MinistryTag.ENERGY, "60-Day Buffer", listOf("30-Day", "60-Day Buffer", "180-Day War Reserve"), "Protects against energy blackouts."),
        LawItem("L23", "DEF-01", "Conscription Policy", "Defense", MinistryTag.DEFENSE, "Volunteer Force", listOf("Volunteer Force", "Selective Draft", "Universal Conscription"), "Scales active military manpower."),
        LawItem("L24", "DEF-02", "Defense Budget Floor", "Defense", MinistryTag.DEFENSE, "2.0% GDP", listOf("1.0% Pacifist", "2.0% GDP", "5.0% Wartime Surge"), "Funds defense plants and readiness."),
        LawItem("L25", "DEF-03", "Arms Export Licensing", "Defense", MinistryTag.DEFENSE, "Allied Only", listOf("Embargoed", "Allied Only", "Open Commercial Sales"), "Permits international weapons trade contracts."),
        LawItem("L26", "DEF-04", "Nuclear Doctrine", "Defense", MinistryTag.DEFENSE, "No First Use", listOf("Non-Nuclear", "No First Use", "Launch-On-Warning"), "Deterrence posture during major crises."),
        LawItem("L27", "FOR-01", "Visa & Border Regime", "Foreign", MinistryTag.FOREIGN_AFFAIRS, "Bilateral Treaties", listOf("Open Borders", "Bilateral Treaties", "Closed Fortress"), "Controls travel access for foreign executives."),
        LawItem("L28", "FOR-02", "Foreign Aid Mandate", "Foreign", MinistryTag.FOREIGN_AFFAIRS, "0.3% GNI", listOf("0% America/Nation First", "0.3% GNI", "1.0% Diplomatic Leverage"), "Improves bilateral relations over time."),
        LawItem("L29", "FOR-03", "Sanctions Compliance", "Foreign", MinistryTag.FOREIGN_AFFAIRS, "UN Mandated", listOf("Neutral Non-Sanction", "UN Mandated", "Secondary Sanctions"), "Enforces trade restrictions."),
        LawItem("L30", "INT-01", "Domestic Signals Intelligence", "Intelligence", MinistryTag.INTELLIGENCE, "Warrant Required", listOf("Strict Privacy", "Warrant Required", "Mass Metadata Dragnet"), "Boosts counter-intel vs civil liberty."),
        LawItem("L31", "INT-02", "Covert Foreign Action Charter", "Intelligence", MinistryTag.INTELLIGENCE, "Authorized", listOf("Prohibited", "Authorized", "Unrestricted Black Ops"), "Enables coup instigation & sabotage."),
        LawItem("L32", "SEC-01", "Internal Security Powers", "Security", MinistryTag.SECURITY, "Standard Policing", listOf("Community Oversight", "Standard Policing", "Preventive Detention"), "Reduces coup risk."),
        LawItem("L33", "SEC-02", "Media & Social Feed Law", "Security", MinistryTag.SECURITY, "Pluralist Press", listOf("Free Speech", "Pluralist Press", "State Censorship"), "Controls NPC social feed sentiment."),
        LawItem("L34", "SEC-03", "Firearms & Militia Statute", "Security", MinistryTag.SECURITY, "Licensed Only", listOf("Banned", "Licensed Only", "Permissive Carry"), "Impacts internal insurgency risk."),
        LawItem("L35", "PRI-01", "Mining Royalties", "Primary", MinistryTag.PRIMARY_SECTOR, "12% Royalty", listOf("4% Extraction Boom", "12% Royalty", "Nationalized Mines"), "Governs Uranium, Coal, Gas, Iron mines."),
        LawItem("L36", "PRI-02", "Agricultural Price Supports", "Primary", MinistryTag.PRIMARY_SECTOR, "Floor Guarantee", listOf("None", "Floor Guarantee", "State Grain Monopoly"), "Stabilizes food supply and farms."),
        LawItem("L37", "PRI-03", "Uranium Export Control", "Primary", MinistryTag.PRIMARY_SECTOR, "IAEA Safeguards", listOf("Free Export", "IAEA Safeguards", "Strategic Ban"), "Restricts enriched uranium sales."),
        LawItem("L38", "EDU-01", "Higher Education Tuition", "Education", MinistryTag.EDUCATION, "Public Subsidized", listOf("Private Tuition", "Public Subsidized", "Free Universal"), "Scales University XP efficiency."),
        LawItem("L39", "EDU-02", "National R&D Grants", "Education", MinistryTag.EDUCATION, "2.2% GDP", listOf("0.8% Minimal", "2.2% GDP", "4.5% Moonshot"), "Accelerates Knowledge Tree research."),
        LawItem("L40", "HLT-01", "Healthcare Coverage", "Health", MinistryTag.HEALTH, "Universal Insurance", listOf("Private Only", "Universal Insurance", "Single-Payer NHS"), "Boosts Pop endurance & longevity."),
        LawItem("L41", "HLT-02", "Pandemic Bio-Surveillance", "Health", MinistryTag.HEALTH, "Active Readiness", listOf("Minimal", "Active Readiness", "Mandatory Quarantine"), "Mitigates random health crises."),
        LawItem("L42", "CIV-01", "Electoral Financing", "Civil", MinistryTag.HEAD_OF_STATE, "Capped Donations", listOf("Public Only", "Capped Donations", "Unlimited Corporate PACs"), "Shapes oligarch political influence."),
        LawItem("L43", "CIV-02", "Anti-Corruption Tribunal", "Civil", MinistryTag.SECURITY, "Independent Commission", listOf("Weak Oversight", "Independent Commission", "Zero-Tolerance Purge"), "Controls black market & bribery."),
        LawItem("L44", "CIV-03", "Noble Titles & Honors Act", "Civil", MinistryTag.HEAD_OF_STATE, "Ceremonial Orders", listOf("Abolished", "Ceremonial Orders", "Hereditary Peerage"), "Enables granting Noble Titles via Correo."),
        LawItem("L45", "CIV-04", "Emergency Decree Powers", "Civil", MinistryTag.HEAD_OF_STATE, "30-Day Limit", listOf("Prohibited", "30-Day Limit", "Indefinite Rule by Decree"), "Lets Head of State bypass chamber votes."),
        LawItem("L46", "CIV-05", "Sovereign Data & AI Act", "Civil", MinistryTag.ECONOMY, "Balanced Guardrails", listOf("Open Innovation", "Balanced Guardrails", "State Compute Monopoly"), "Impacts tech valuations.")
    )

    fun buildConstitutionalChapters(): List<ConstitutionalChapter> = listOf(
        ConstitutionalChapter(1, "Sovereignty & State Form", "Power vested in the electorate under constitutional supremacy.", "Reaffirm unitary sovereignty with binding digital referenda."),
        ConstitutionalChapter(2, "Bill of Fundamental Rights", "Protected speech, assembly, and due process guarantees.", "Expand digital privacy and algorithmic transparency rights."),
        ConstitutionalChapter(3, "Executive Presidency & Cabinet", "Executive powers shared with parliamentary confidence.", "Grant Head of State direct ministerial appointment & line-item veto."),
        ConstitutionalChapter(4, "Bicameral / Unicameral Legislature", "Standard legislative voting with 50%+1 simple majority.", "Require 60% supermajority for fiscal deficits and war powers."),
        ConstitutionalChapter(5, "Judiciary & Constitutional Court", "Independent high court with lifetime judicial tenure.", "Introduce 12-year non-renewable term limits and fast-track review."),
        ConstitutionalChapter(6, "Federalism & Provincial Autonomy", "Regional provinces manage local municipal grids and policing.", "Elevate fiscal devolution to 35% of national tax receipts."),
        ConstitutionalChapter(7, "Economic Charter & Property", "Private enterprise protected alongside strategic public utilities.", "Establish Sovereign Wealth mandate or Planned Quota supremacy."),
        ConstitutionalChapter(8, "Central Bank & Monetary Integrity", "Monetary authority insulated from short-term fiscal deficits.", "Enshrine hard inflation ceiling of 3.0% and reserve audit rules."),
        ConstitutionalChapter(9, "National Defense & Intelligence", "Civilian supremacy over armed forces and intelligence directorate.", "Create joint parliamentary oversight committee for covert ops."),
        ConstitutionalChapter(10, "Constitutional Amendment Rules", "Amendments require 2/3 chamber approval.", "Add mandatory national plebiscite within 30 in-game days.")
    )

    fun buildKnowledgeTree(): List<KnowledgeNode> = listOf(
        KnowledgeNode("K_MACRO_1", "Macroeconomic Foundations", "Master monetary velocity, M0/M1 multipliers, and bond yields.", "Mastery", 1, 20, false, 0, "+12% Corporate & Treasury ROI"),
        KnowledgeNode("K_DIPLO_1", "Vienna Convention Protocol", "Master bilateral summit counter-offers and treaty drafting.", "Mastery", 2, 20, false, 0, "+15 Summit Acceptance Odds"),
        KnowledgeNode("K_GRID_1", "High-Voltage Grid Architecture", "Master 3-tier Plant → Transmission → Substation scaling.", "Mastery", 3, 20, false, 0, "+20% Substation Capacity"),
        KnowledgeNode("K_INTEL_1", "Clandestine Tradecraft", "Master covert spy networks and counter-intelligence sweeps.", "Mastery", 4, 20, false, 0, "+18% Covert Op Success"),
        KnowledgeNode("K_LAW_1", "Constitutional Engineering", "Master bicameral whipping and constitutional reform.", "Mastery", 5, 20, false, 0, "+10% Parliamentary Whip Power"),
        KnowledgeNode("K_SPEC_TRADING", "Arbitrage & Forex Desk", "Repeatable specialization sharpening market & Forex timing.", "Specialization", 1, 15, true, 0, "+4% Trade & Stock Margin / stack"),
        KnowledgeNode("K_SPEC_EXEC", "Executive Leadership", "Repeatable specialization boosting company efficiency.", "Specialization", 2, 15, true, 0, "+3 Efficiency & +2 Charisma / stack"),
        KnowledgeNode("K_SPEC_MIL", "Joint Staff Doctrine", "Repeatable specialization improving strategic deterrence.", "Specialization", 3, 15, true, 0, "+5% Defense Readiness / stack")
    )

    fun buildSupranationalOrgs(): List<SupranationalOrg> = listOf(
        SupranationalOrg("ORG_UNSC", "United Nations Security Council", "UNSC", "Global Peacekeeping & Sanctions Authority", listOf("US", "CN", "RU", "FR", "GB", "IN", "JP", "BR"), 18.4, "Resolution 2784: Critical Mineral Supply Corridor Security"),
        SupranationalOrg("ORG_BRICS", "BRICS+ Monetary & Development Bloc", "BRICS+", "Alternative Settlement & Sovereign Development", listOf("BR", "RU", "IN", "CN", "ZA", "EG", "ET", "IR", "AE", "SA"), 142.0, "BRICS Pay Cross-Border Local Currency Clearing Protocol"),
        SupranationalOrg("ORG_EU", "European Union", "EU", "Single Market, Schengen Visas & Eurozone", listOf("FR", "DE", "ES", "IT", "NL", "BE", "PL", "SE", "AT", "IE", "PT", "GR", "FI", "DK", "CZ", "RO", "HU"), 310.5, "European Grid Interconnector & Strategic Chips Act"),
        SupranationalOrg("ORG_NATO", "North Atlantic Treaty Organization", "NATO", "Collective Defense & Deterrence Alliance", listOf("US", "GB", "FR", "DE", "ES", "IT", "CA", "PL", "TR", "NL", "NO", "SE", "FI", "DK"), 88.0, "3.0% Defense Spending Readiness Standard"),
        SupranationalOrg("ORG_ASEAN", "Association of Southeast Asian Nations", "ASEAN", "Indo-Pacific Trade & Semiconductor Packaging", listOf("SG", "ID", "VN", "PH", "TH", "MY", "KH", "LA", "MM", "BN"), 44.2, "ASEAN Regional Power Grid Integration"),
        SupranationalOrg("ORG_AU", "African Union", "AU", "AfCFTA Free Trade & Continental Infrastructure", listOf("ZA", "NG", "EG", "ET", "KE", "MA", "DZ", "AO", "GH", "TZ"), 31.0, "Pan-African Payment & Settlement System Expansion"),
        SupranationalOrg("ORG_MERCOSUR", "Southern Common Market", "Mercosur", "South American Customs & Agro-Lithium Union", listOf("BR", "AR", "UY", "PY", "BO"), 22.8, "South American Energy & Lithium Corridor"),
        SupranationalOrg("ORG_IMF", "International Monetary Fund", "IMF", "Lender of Last Resort & SDR Liquidity", listOf("US", "IN", "CN", "DE", "JP", "GB", "FR", "ES", "BR", "CA"), 980.0, "Emergency Forex Stabilization Facility")
    )
}
