/*
 * Copyright 2026 Vsevolod Petrov (lazyhat)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cbi.development.orbitalis

import org.cbi.orbitalis.core.orbit.CelestialDynamics
import org.cbi.orbitalis.core.orbit.SolarSystemProfile
import org.cbi.orbitalis.core.planet.CartesianState
import org.cbi.orbitalis.core.planet.Vector3

/** Immutable, game-independent engines-off prediction. Uses the same field, clock and return
 * envelope as the real transfer. A 0.05 s integration cadence bounds work to 12,000 steps;
 * the first outside-to-inside crossing is refined within its integration segment.
 */
internal object EarthEntryForecast {
    data class Result(val willEnter: Boolean, val seconds: Double, val state: CartesianState)

    fun calculate(profile: SolarSystemProfile, tick: Long, initial: CartesianState): Result {
        require(initial.position.length() <= profile.flightRange && initial.velocity.length() <= 9990.0)
        val dynamics = CelestialDynamics(profile, tick)
        return integrate(initial, { p, t -> dynamics.acceleration(p, t) },
            { state, t -> profile.earthSurfaceFrame(tick, t).fromParent(state) },
            { p -> profile.geometry.earthEnvelope.distance(p) <= profile.geometry.earthEnvelope.descentDistance })
    }

    internal fun integrate(
        initial: CartesianState,
        acceleration: (Vector3, Double) -> Vector3,
        earth: (CartesianState, Double) -> CartesianState,
        inside: (Vector3) -> Boolean,
    ): Result {
        var state = initial
        var time = 0.0
        var a = acceleration(state.position, time)
        if (inside(earth(state, time).position)) return Result(true, time, earth(state, time))
        repeat(12000) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Entry forecast cancelled")
            val nextTime = (it + 1) * 0.05
            val half = state.velocity + a * 0.025
            val p = state.position + half * 0.05
            val nextA = acceleration(p, nextTime)
            val next = CartesianState(p, half + nextA * 0.025)
            if (inside(earth(next, nextTime).position)) {
                var lo = 0.0
                var hi = 1.0
                fun between(f: Double) = CartesianState(state.position + (next.position-state.position)*f,
                    state.velocity + (next.velocity-state.velocity)*f)
                repeat(20) {
                    val mid = (lo+hi)*0.5
                    if (inside(earth(between(mid), time+0.05*mid).position)) hi=mid else lo=mid
                }
                return Result(true, time+0.05*hi, earth(between(hi), time+0.05*hi))
            }
            state = next
            time = nextTime
            a = nextA
        }
        return Result(false, time, earth(state, time))
    }
}
