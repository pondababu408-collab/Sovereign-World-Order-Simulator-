package com.example

import com.example.data.NpcGenerator
import com.example.data.WorldCatalog
import com.example.data.WorldGeoPolygons
import com.example.engine.SimulationEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class SimulationEngineTest {

    @Test
    fun worldCatalogContainsAtLeast195CountriesAnd46Laws() {
        val countries = WorldCatalog.buildAllCountries("IN")
        assertTrue("Expected >= 195 countries, got ${countries.size}", countries.size >= 195)
        val laws = WorldCatalog.buildAllLaws()
        assertTrue("Expected >= 45 laws, got ${laws.size}", laws.size >= 45)
        val chapters = WorldCatalog.buildConstitutionalChapters()
        assertEquals(10, chapters.size)
    }

    @Test
    fun npcGeneratorCreates420PlusCitizensAcrossTraitTiers() {
        val careerState = NpcGenerator.buildInitialCampaign(
            playerName = "Test Citizen",
            playerHandle = "test_citizen",
            homeCountryIso = "IN",
            homeRegionId = "IN_DL",
            startingMode = StartingMode.CAREER,
            attributes = CharacterAttributes(),
            ideology = IdeologyProfile()
        )
        assertTrue("Expected >= 420 NPCs, got ${careerState.npcs.size}", careerState.npcs.size >= 420)
        assertEquals(0.0, careerState.player.cashUsd, 0.001)
        assertFalse(SimulationEngine.hasPortfolioAccess(careerState, MinistryTag.CENTRAL_BANK))
        assertTrue(careerState.npcs.any { it.traitTier == TraitTier.HIGH })
        assertTrue(careerState.npcs.any { it.traitTier == TraitTier.MID })
        assertTrue(careerState.npcs.any { it.traitTier == TraitTier.LOW })
        assertTrue("Expected seeded bonds", careerState.bonds.isNotEmpty())
        assertTrue("Expected seeded real estate", careerState.realEstate.isNotEmpty())
        assertTrue("Expected seeded patents", careerState.patents.isNotEmpty())
    }

    @Test
    fun sandboxModeUnlocksExecutiveAndAdvancing24HoursTriggersDayRollover() {
        val sandboxState = NpcGenerator.buildInitialCampaign(
            playerName = "President Vance",
            playerHandle = "pres_vance",
            homeCountryIso = "US",
            homeRegionId = "US_DC",
            startingMode = StartingMode.SANDBOX,
            attributes = CharacterAttributes(),
            ideology = IdeologyProfile()
        )
        assertTrue(SimulationEngine.hasPortfolioAccess(sandboxState, MinistryTag.HEAD_OF_STATE))
        assertTrue(SimulationEngine.hasPortfolioAccess(sandboxState, MinistryTag.CENTRAL_BANK))

        val ownedBond = sandboxState.bonds.first().copy(unitsHeld = 5, daysRemaining = 10)
        val ownedProp = sandboxState.realEstate.first().copy(owned = true)
        val ownedPatent = sandboxState.patents.first().copy(ownerId = sandboxState.player.id)
        val seeded = sandboxState.copy(
            bonds = listOf(ownedBond) + sandboxState.bonds.drop(1),
            realEstate = listOf(ownedProp) + sandboxState.realEstate.drop(1),
            patents = listOf(ownedPatent) + sandboxState.patents.drop(1)
        )

        val nextDayState = SimulationEngine.advanceTick(seeded, hoursDelta = 24, realSecondsElapsed = 5)
        assertEquals(seeded.date.totalDays() + 1, nextDayState.date.totalDays())
        assertEquals(400, nextDayState.player.energy)
        assertTrue("Expected cash payout from owned assets", nextDayState.player.cashUsd > seeded.player.cashUsd)
    }

    @Test
    fun worldGeoPolygonsContainLandmassesAndAccuratePointInRingHitTesting() {
        assertTrue("Expected >= 40 vector country polygons", WorldGeoPolygons.polygons.size >= 40)
        val usPoly = WorldGeoPolygons.polygons.first { it.iso == "US" }
        val hitUs = usPoly.rings.any { ring ->
            WorldGeoPolygons.pointInRing(
                WorldGeoPolygons.lonToNormX(-98f),
                WorldGeoPolygons.latToNormY(39f),
                ring
            )
        }
        assertTrue("Expected (-98, 39) to hit US polygon", hitUs)

        val hitOcean = usPoly.rings.any { ring ->
            WorldGeoPolygons.pointInRing(
                WorldGeoPolygons.lonToNormX(0f),
                WorldGeoPolygons.latToNormY(0f),
                ring
            )
        }
        assertFalse("Expected (0, 0) Atlantic ocean not to hit US polygon", hitOcean)

        val geoAsset = java.io.File("src/main/assets/world_countries.geojson")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/assets/world_countries.geojson")
        assertTrue("Expected bundled world_countries.geojson asset to exist", geoAsset.exists())
        val parsed = WorldGeoPolygons.parseGeoJson(geoAsset.readText())
        assertTrue("Expected >= 50 parsed GeoJSON country features, got ${parsed.size}", parsed.size >= 50)
        val japan = parsed.first { it.iso == "JP" }
        assertTrue("Expected MultiPolygon Japan to parse 3 island rings", japan.rings.size >= 3)
    }

    @Test
    fun economyHubContainsAll12CardsAndOledColors() {
        val routes = com.example.ui.screens.EconomySubRoute.entries
        assertEquals(12, routes.size)
        val expectedTitles = listOf(
            "Online banking",
            "Stocks",
            "Bilateral debt",
            "Bonds",
            "B2B platform",
            "B2C market",
            "Market share",
            "Foreign exchange",
            "Crypto exchange",
            "Derivatives",
            "Intellectual property",
            "Forbes 100"
        )
        assertEquals(expectedTitles, routes.map { it.title })
        assertTrue(routes.all { it.description.isNotBlank() })
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0D0D0D), com.example.ui.theme.OledCard)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF222222), com.example.ui.theme.OledBorder)
    }
}
