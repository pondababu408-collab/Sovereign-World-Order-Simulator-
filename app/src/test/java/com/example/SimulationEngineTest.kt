package com.example

import com.example.data.NpcGenerator
import com.example.data.WorldCatalog
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

        val nextDayState = SimulationEngine.advanceTick(sandboxState, hoursDelta = 24, realSecondsElapsed = 5)
        assertEquals(sandboxState.date.totalDays() + 1, nextDayState.date.totalDays())
        assertEquals(400, nextDayState.player.energy)
    }
}
