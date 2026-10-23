package org.cbi.development.orbitalis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.cbi.orbitalis.core.orbit.SolarSystemProfile
import org.cbi.orbitalis.core.planet.CartesianState
import org.cbi.orbitalis.core.planet.Vector3

class EarthEntryForecastTest {
    @Test fun `descending forecast uses rotating surface position and velocity at crossing`() {
        val profile = SolarSystemProfile(11.0, earthSpinEpochTick = 0, selectedEarthWidth = 10000)
        val initial = profile.earthSurfaceFrame(12345).toParent(CartesianState(Vector3(500.0, 6000.0, 0.0), Vector3(0.0, -100.0, 0.0)))
        val prediction = EarthEntryForecast.calculate(profile, 12345, initial)
        assertTrue(prediction.willEnter)
        assertTrue(prediction.seconds in 1.0..100.0)
        assertEquals(2064.0, prediction.state.position.y, 0.001)
        assertTrue(prediction.state.velocity.y < -100.0)
        assertTrue(kotlin.math.abs(prediction.state.position.x) < 5000.0)
    }

    @Test fun `envelope and frame adapt to small Earth`() {
        val profile = SolarSystemProfile(11.0, earthSpinEpochTick = 0, selectedEarthWidth = 1000)
        val initial = profile.earthSurfaceFrame(4000).toParent(CartesianState(Vector3(0.0, 3000.0, 0.0), Vector3(0.0, -50.0, 0.0)))
        val result = EarthEntryForecast.calculate(profile, 4000, initial)
        assertTrue(result.willEnter)
        assertEquals(profile.geometry.earthEnvelope.halfExtents.y+profile.geometry.earthEnvelope.descentDistance,
            result.state.position.y, 0.001)
    }

    @Test fun `lateral crossing is detected without descending through top plane`() {
        val result = EarthEntryForecast.integrate(CartesianState(Vector3(100.0, 0.0, 0.0), Vector3(-10.0, 0.0, 0.0)),
            { _, _ -> Vector3.ZERO }, { state, _ -> state }, { p -> p.length() <= 20.0 })
        assertTrue(result.willEnter)
        assertEquals(8.0, result.seconds, 1e-6)
        assertEquals(20.0, result.state.position.x, 1e-6)
    }

    @Test fun `outbound path ends at bounded horizon rather than manufacturing entry`() {
        val result = EarthEntryForecast.integrate(CartesianState(Vector3(100.0, 0.0, 0.0), Vector3(10.0, 0.0, 0.0)),
            { _, _ -> Vector3.ZERO }, { state, _ -> state }, { p -> p.length() <= 20.0 })
        assertFalse(result.willEnter)
        assertEquals(600.0, result.seconds)
        assertEquals(6100.0, result.state.position.x, 1e-6)
    }
    @Test fun `reset can cancel bounded worker without publishing partial entry`() {
        Thread.currentThread().interrupt()
        try {
            assertFailsWith<InterruptedException> {
                EarthEntryForecast.integrate(CartesianState(Vector3(100.0, 0.0, 0.0), Vector3.ZERO),
                    { _, _ -> Vector3.ZERO }, { state, _ -> state }, { false })
            }
        } finally { Thread.interrupted() }
    }

}
