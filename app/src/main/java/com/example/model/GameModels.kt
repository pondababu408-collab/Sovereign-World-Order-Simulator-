package com.example.model

import com.squareup.moshi.JsonClass

enum class NavDestination(val label: String) {
    HOME("Inicio"),
    EXPLORER("Explorer"),
    WORLD_MAP("World Map"),
    ECONOMY("Economy"),
    ORGANIZATIONS("Orgs"),
    CONFLICT("Conflict"),
    INBOX("Messages"),
    NOTIFICATIONS("Alerts"),
    AWARDS("Awards"),
    PERSONAL("Personal"),
    PROFILE("Profile"),
    MORE("More"),
    // Sub-destinations accessible from More 4x4 Grid or contextual buttons
    UNIVERSITY("University"),
    KNOWLEDGE("Knowledge"),
    RANKING("Ranking"),
    SUPRANATIONAL("Supranational"),
    MARKET("Market"),
    TUTORIAL("Tutorial"),
    RULES("Rules"),
    SOCIETY("Society"),
    ADMINISTRATION("Admin"),
    SETTINGS("Settings")
}

enum class GameSpeed(val label: String, val hoursPerTick: Int, val delayMs: Long) {
    PAUSED("||", 0, 500L),
    X1("1x", 1, 950L),
    X2("2x", 2, 650L),
    X5("5x", 4, 420L),
    X10("10x", 8, 280L)
}

enum class StartingMode(val title: String, val subtitle: String) {
    CAREER("Career Mode", "Start at 0.00 cash as a regular citizen. Build stats, get hired on LinkedOut, found companies, and climb to power."),
    SANDBOX("Sandbox Mode", "Start as Head of State with full executive authority, sovereign treasury access, and cabinet control.")
}

@JsonClass(generateAdapter = true)
data class GameDate(
    val year: Int = 2026,
    val month: Int = 11, // 1..12
    val day: Int = 23,   // 1..30 (normalized 30-day calendar for predictable strategic timers)
    val hour: Int = 11   // 0..23
) {
    fun totalHours(): Long = ((year - 2026L) * 360L + (month - 1L) * 30L + (day - 1L)) * 24L + hour
    fun totalDays(): Int = ((year - 2026) * 360 + (month - 1) * 30 + (day - 1))

    fun advanceHours(delta: Int): GameDate {
        if (delta <= 0) return this
        var h = hour + delta
        var d = day
        var m = month
        var y = year
        while (h >= 24) {
            h -= 24
            d += 1
            if (d > 30) {
                d = 1
                m += 1
                if (m > 12) {
                    m = 1
                    y += 1
                }
            }
        }
        return GameDate(y, m, d, h)
    }

    fun formatted(): String {
        val monthName = when (month) {
            1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
            5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
            9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
        }
        val hourStr = hour.toString().padStart(2, '0')
        return "$monthName $day, $year • $hourStr:00"
    }
}

enum class StatType(val displayName: String) {
    INTELLIGENCE("Intelligence"),
    STRENGTH("Strength"),
    ENDURANCE("Endurance")
}

@JsonClass(generateAdapter = true)
data class TrainingJob(
    val stat: StatType,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val xpReward: Int
)

enum class UniversityFaculty(val displayName: String, val tag: String) {
    ECONOMICS("Economics", "ECON"),
    POLITICAL_SCIENCE("Political Science", "POLI"),
    LAW("Constitutional Law", "LAW"),
    ENGINEERING("Industrial Engineering", "ENG"),
    MILITARY("Strategic & Military", "MIL")
}

@JsonClass(generateAdapter = true)
data class KnowledgeNode(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val minLevel: Int,
    val durationDays: Int, // 20 days for one-time mastery, 15 days for repeatable
    val isRepeatable: Boolean,
    val completionCount: Int = 0,
    val bonusSummary: String
)

@JsonClass(generateAdapter = true)
data class ActiveResearch(
    val nodeId: String,
    val remainingHours: Int,
    val totalHours: Int
)

enum class MinistryTag(val code: String, val title: String, val description: String) {
    HEAD_OF_STATE("HEAD_OF_STATE", "Head of State", "Executive veto, decree power, and cabinet appointments"),
    TREASURY("TREASURY", "Ministry of Treasury", "Tax rates, sovereign debt, and budget allocations"),
    ECONOMY("ECONOMY", "Ministry of Economy", "Industrial subsidies, tariffs, and trade licensing"),
    DEFENSE("DEFENSE", "Ministry of Defense", "Armed forces mobilization, defense procurement, and treaties"),
    FOREIGN_AFFAIRS("FOREIGN_AFFAIRS", "Foreign Affairs", "Visa treaties, embassies, summits, and alliance blocs"),
    ENERGY("ENERGY", "Ministry of Energy", "National grid regulation, power permits, and fuel reserves"),
    INTELLIGENCE("INTELLIGENCE", "Intelligence Directorate", "Espionage networks, covert sabotage, and counter-intel"),
    SECURITY("SECURITY", "Internal Security", "Internal policing, anti-coup sweeps, and martial order"),
    HEALTH("HEALTH", "Ministry of Health", "Public healthcare spending and demographic resilience"),
    EDUCATION("EDUCATION", "Ministry of Education", "University grants, literacy, and R&D multipliers"),
    PRIMARY_SECTOR("PRIMARY_SECTOR", "Primary Sector", "Mining concessions, agricultural quotas, and extraction"),
    CENTRAL_BANK("CENTRAL_BANK", "Central Bank Governor", "M0/M1 supply, interest rates, reserve ratio, SWIFT"),
    CENTRAL_PLANNING("CENTRAL_PLANNING", "Central Planning Committee", "SOE price fixing, Pop Ration Quotas, 1-yr/5-yr plans")
}

enum class EconomicSystem(val label: String) {
    MARKET("Free Market"),
    MIXED("Mixed Economy"),
    STATE_CAPITALISM("State Capitalism"),
    PLANNED("Planned Economy")
}

enum class GovType(val label: String) {
    PARLIAMENTARY("Parliamentary Republic"),
    SEMI_PRESIDENTIAL("Semi-Presidential Republic"),
    PRESIDENTIAL("Presidential Republic"),
    ONE_PARTY_SOCIALIST("One-Party Socialist State"),
    ABSOLUTE_MONARCHY("Absolute Monarchy")
}

enum class PowerStructure(val label: String) {
    FEDERAL("Federal Decentralized"),
    UNITARY("Unitary Centralized"),
    CONFEDERAL("Confederal Union"),
    AUTOCRATIC("Autocratic Vertical")
}

@JsonClass(generateAdapter = true)
data class CharacterAttributes(
    val culture: String = "Cosmopolitan",
    val belief: String = "Secular Pragmatism",
    val gender: String = "Non-Binary / Custom"
)

@JsonClass(generateAdapter = true)
data class IdeologyProfile(
    val primary: String = "Technocratic Realism",
    val secondary: String = "Mercantilist Growth",
    val economicSystem: EconomicSystem = EconomicSystem.MIXED,
    val formOfGovernment: GovType = GovType.PARLIAMENTARY,
    val powerStructure: PowerStructure = PowerStructure.UNITARY
)

@JsonClass(generateAdapter = true)
data class PlayerCharacter(
    val id: String = "player_01",
    val name: String = "Aarav Vance",
    val handle: String = "aarav_vance",
    val countryIso: String = "IN",
    val regionId: String = "IN_DL",
    val currentCountryIso: String = "IN",
    val currentRegionId: String = "IN_DL",
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNext: Int = 100,
    val araucitas: Int = 3,
    val cashLocal: Double = 0.0,
    val cashUsd: Double = 0.0,
    val bitcoinBalance: Double = 0.0,
    val intelligence: Int = 14,
    val strength: Int = 12,
    val endurance: Int = 12,
    val efficiency: Int = 15,
    val charisma: Int = 14,
    val energy: Int = 400,
    val maxEnergy: Int = 400,
    val workStreak: Int = 0,
    val lastWorkDay: Int = -1,
    val activeTraining: TrainingJob? = null,
    val activeResearch: ActiveResearch? = null,
    val facultyLevels: Map<String, Int> = mapOf(
        "ECONOMICS" to 1,
        "POLITICAL_SCIENCE" to 1,
        "LAW" to 1,
        "ENGINEERING" to 1,
        "MILITARY" to 1
    ),
    val completedKnowledgeIds: Map<String, Int> = emptyMap(),
    val attributes: CharacterAttributes = CharacterAttributes(),
    val ideology: IdeologyProfile = IdeologyProfile(),
    val officesHeld: List<MinistryTag> = emptyList(),
    val isParliamentMember: Boolean = false,
    val partyId: String? = null,
    val nobleTitles: List<String> = emptyList(),
    val currentJobId: String? = null,
    val portfolioShares: Map<String, Double> = emptyMap(), // ticker -> share count
    val followingNpcIds: List<String> = emptyList(),
    val avatarSeed: Int = 42
)
