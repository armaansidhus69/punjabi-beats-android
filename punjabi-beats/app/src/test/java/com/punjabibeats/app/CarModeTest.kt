package com.punjabibeats.app

import com.punjabibeats.app.ui.screens.car.CabinProfile
import com.punjabibeats.app.ui.screens.car.CarDashboardState
import com.punjabibeats.app.ui.screens.car.CarDspState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarModeTest {

    @Test
    fun cabinProfilesContainAllExpectedVehicleTypes() {
        val profiles = CabinProfile.entries
        assertEquals("Must have 4 cabin profiles", 4, profiles.size)

        val ids = profiles.map { it.id }
        assertTrue("Must have SUV Thar profile", ids.contains("suv_thar"))
        assertTrue("Must have Sedan Cruiser profile", ids.contains("sedan_cruiser"))
        assertTrue("Must have Gedhi Hatch profile", ids.contains("gedhi_hatch"))
        assertTrue("Must have Custom Boombox profile", ids.contains("custom_boombox"))
    }

    @Test
    fun defaultCarDashboardStateHasValidTelemetry() {
        val state = CarDashboardState()
        assertEquals("PB 31 • THAR 4X4", state.vehicleName)
        assertEquals("GT ROAD • BYPASS", state.roadName)
        assertTrue("Speed must be positive", state.speedKmh > 0)
        assertEquals(80, state.speedLimitKmh)
        assertTrue("Must have quick picks", state.quickPicks.isNotEmpty())
    }

    @Test
    fun carDspStateAlignmentAndPhaseDefaultsMatchStitchSpec() {
        val state = CarDspState()
        assertEquals(CabinProfile.SUV_THAR, state.cabinProfile)
        assertEquals(1.15f, state.driverDelayMs, 0.01f)
        assertEquals(180, state.subwooferPhaseDeg)
        assertEquals(6.5f, state.gaddi808SlamDb, 0.01f)
        assertTrue("Speed noise leveler should be enabled by default", state.highwayNoiseLevelerEnabled)
    }

    @Test
    fun dspSubwooferPhaseCanToggleBetween0And180() {
        val stateZero = CarDspState(subwooferPhaseDeg = 0)
        assertEquals(0, stateZero.subwooferPhaseDeg)

        val stateInvert = CarDspState(subwooferPhaseDeg = 180)
        assertEquals(180, stateInvert.subwooferPhaseDeg)
    }
}
