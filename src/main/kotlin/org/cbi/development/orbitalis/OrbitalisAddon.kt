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

import dev.ryanhcode.sable.Sable
import dev.ryanhcode.sable.sublevel.ServerSubLevel
import net.neoforged.fml.common.Mod
import org.cbi.orbitalis.minecraft.integration.sable.SableFlightGravity
import org.cbi.orbitalis.minecraft.integration.sable.ShipCoastForecast
import org.cbi.orbitalis.minecraft.flight.CelestialFlight
import org.cbi.orbitalis.core.planet.CartesianState
import org.cbi.orbitalis.core.planet.Vector3
import orbitalis.EarthSample
import orbitalis.EntrySummary
import orbitalis.NavigationSample
import orbitalis.CoastSummary
import ru.lazyhat.compukters.api.addon.addonPending
import ru.lazyhat.compukters.api.addon.addonPollCompleted
import ru.lazyhat.compukters.api.addon.addonPollFailed
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.cbi.orbitalis.minecraft.integration.sable.ShipTransferParticipants
import orbitalis.GravitySample
import orbitalis.OrbitalisAddonContract
import orbitalis.OrbitalisCapabilityHandler
import orbitalis.Vector3d
import ru.lazyhat.compukters.api.addon.AddonCallResult
import ru.lazyhat.compukters.api.addon.addonCompleted
import ru.lazyhat.compukters.api.addon.addonFailed
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersAddonHostFactory
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersAddonRegistry
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersComputerContext
import ru.lazyhat.compukters.lang.runtime.vm.HostFailureKind

@Mod("compukters_orbitalis")
class OrbitalisAddon {
    init {
        ShipTransferParticipants.register("compukters", CompuktersShipTransferParticipant)
        CompuktersAddonRegistry.register(
            OrbitalisAddonContract.guestApi(OrbitalisAddon::class.java),
            CompuktersAddonHostFactory { computer -> OrbitalisAddonContract.host(GravityHost(computer)) },
        )
    }
}

private class GravityHost(private val computer: CompuktersComputerContext) : OrbitalisCapabilityHandler {
    private var pending: Future<*>? = null
    override fun checkpoint(): ByteArray { reset(); return byteArrayOf() }
    override fun reset() { pending?.cancel(true); pending = null }

    private fun body(): ServerSubLevel? {
        check(computer.level.server.isSameThread) { "Navigation requires the server thread" }
        if (!computer.level.hasChunkAt(computer.position) || !SableFlightGravity.isManaged(computer.level)) return null
        return (Sable.HELPER.getContaining(computer.level, computer.position) as? ServerSubLevel)
            ?.takeUnless { it.isRemoved || it.massTracker.centerOfMass == null || it.massTracker.mass <= 0.0 }
    }

    override fun observe(): AddonCallResult<NavigationSample> {
        val body = body() ?: return unavailable()
        val sample = SableFlightGravity.reading(body)
        val celestial = CelestialFlight.sample(computer.level.server)
        val chart = computer.level.dimension()
        val root = celestial.rootState(CartesianState(sample.position, Vector3(sample.velocity.x, sample.velocity.y, sample.velocity.z)), chart)
        val geometry = celestial.frames.profile.geometry
        val extent = geometry.earthEnvelope.halfExtents
        val exclusion = extent.length() + geometry.earthEnvelope.descentDistance + 64.0
        fun guest(v: Vector3) = Vector3d(v.x, v.y, v.z)
        return addonCompleted(NavigationSample(
            computer.level.gameTime, body.uniqueId.toString(), chart.location().toString(),
            guest(root.position), guest(root.velocity), guest(celestial.dynamics.acceleration(root.position)),
            guest(celestial.nativeDirection(Vector3(1.0, 0.0, 0.0), chart)),
            guest(celestial.nativeDirection(Vector3(0.0, 1.0, 0.0), chart)),
            guest(celestial.nativeDirection(Vector3(0.0, 0.0, 1.0), chart)),
            celestial.dynamics.earth.body.mu, exclusion,
        ))
    }

    override fun coast(): AddonCallResult<CoastSummary> {
        val body = body() ?: return unavailable()
        val capture = ShipCoastForecast.capture(body) ?: return unavailable()
        return forecast {
            val trajectory = capture.calculate()
            val prediction = trajectory.prediction
            CoastSummary(trajectory.sampledTick, trajectory.ship.toString(), trajectory.dimension.toString(),
                prediction.end.name, prediction.terminal.seconds,
                prediction.points.minOf { it.state.position.length() },
                prediction.points.maxOf { it.state.position.length() })
        }
    }

    override fun earth(): AddonCallResult<EarthSample> {
        val body = body() ?: return unavailable()
        val sample = SableFlightGravity.reading(body)
        val celestial = CelestialFlight.sample(computer.level.server)
        val chart = computer.level.dimension()
        val root = celestial.rootState(CartesianState(sample.position,
            Vector3(sample.velocity.x, sample.velocity.y, sample.velocity.z)), chart)
        val profile = celestial.frames.profile
        val frame = profile.earthSurfaceFrame(celestial.frames.tick)
        val earth = frame.fromParent(root)
        val envelope = profile.geometry.earthEnvelope
        fun guest(v: Vector3) = Vector3d(v.x, v.y, v.z)
        fun axis(v: Vector3) = guest(celestial.nativeDirection(frame.orientation.apply(v), chart))
        return addonCompleted(EarthSample(computer.level.gameTime, body.uniqueId.toString(), chart.location().toString(),
            guest(earth.position), guest(earth.velocity),
            guest(frame.fromParentAcceleration(root, celestial.dynamics.acceleration(root.position))),
            axis(Vector3(1.0, 0.0, 0.0)), axis(Vector3(0.0, 1.0, 0.0)), axis(Vector3(0.0, 0.0, 1.0)),
            envelope.halfExtents.x, envelope.halfExtents.z, envelope.descentDistance, envelope.halfExtents.y))
    }

    override fun entry(): AddonCallResult<EntrySummary> {
        val body = body() ?: return unavailable()
        val sample = SableFlightGravity.reading(body)
        val celestial = CelestialFlight.sample(computer.level.server)
        val root = celestial.rootState(CartesianState(sample.position,
            Vector3(sample.velocity.x, sample.velocity.y, sample.velocity.z)), computer.level.dimension())
        val tick = computer.level.gameTime
        val ship = body.uniqueId.toString()
        val dimension = computer.level.dimension().location().toString()
        // Copy only immutable values. No body, level, context or host enters the worker.
        val profile = celestial.frames.profile
        val epoch = celestial.frames.tick
        if (root.position.length() > profile.flightRange || root.velocity.length() > 9990.0) return unavailable()
        return forecast {
            val result = EarthEntryForecast.calculate(profile, epoch, root)
            val p = result.state.position
            val v = result.state.velocity
            EntrySummary(tick, ship, dimension, result.willEnter, result.seconds,
                Vector3d(p.x, p.y, p.z), Vector3d(v.x, v.y, v.z))
        }
    }

    private fun <T> forecast(calculate: () -> T): AddonCallResult<T> {
        if (pending != null) return addonFailed(HostFailureKind.UNAVAILABLE, "A forecast is already pending")
        val future = try { forecastWorker.submit<T> { calculate() } }
        catch (failure: java.util.concurrent.RejectedExecutionException) {
            return addonFailed(HostFailureKind.UNAVAILABLE, "Forecast worker is busy")
        }
        pending = future
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        return addonPending {
            when {
                pending !== future || future.isCancelled -> addonPollFailed(HostFailureKind.UNAVAILABLE, "Forecast cancelled")
                future.isDone -> {
                    pending = null
                    try { addonPollCompleted(future.get()) }
                    catch (failure: Exception) { addonPollFailed(HostFailureKind.UNAVAILABLE, "Forecast calculation failed") }
                }
                System.nanoTime() > deadline -> {
                    reset()
                    addonPollFailed(HostFailureKind.UNAVAILABLE, "Forecast timed out")
                }
                else -> null
            }
        }
    }

    override fun sample(): AddonCallResult<GravitySample> {
        check(computer.level.server.isSameThread) { "Gravity observations require the server thread" }
        if (!computer.level.hasChunkAt(computer.position) || !SableFlightGravity.isManaged(computer.level)) {
            return unavailable()
        }
        val body = Sable.HELPER.getContaining(computer.level, computer.position) as? ServerSubLevel ?: return unavailable()
        if (body.isRemoved || body.massTracker.centerOfMass == null || body.massTracker.mass <= 0.0) return unavailable()
        val sample = SableFlightGravity.reading(body)
        val p = sample.position
        val v = sample.velocity
        val a = sample.acceleration
        return addonCompleted(GravitySample(
            computer.level.gameTime, body.uniqueId.toString(), computer.level.dimension().location().toString(),
            Vector3d(computer.position.x.toDouble(), computer.position.y.toDouble(), computer.position.z.toDouble()),
            Vector3d(p.x, p.y, p.z), Vector3d(v.x, v.y, v.z), Vector3d(a.x, a.y, a.z), sample.mass,
        ))
    }

    private fun unavailable(): AddonCallResult<Nothing> =
        addonFailed(HostFailureKind.UNAVAILABLE, "Computer must be on a loaded construction in an Orbitalis gravity chart")
}

// No game handles enter this shared, bounded worker. Idle daemon exits after five seconds.
private val forecastWorker = ThreadPoolExecutor(0, 1, 5L, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(4),
    { work -> Thread(work, "Guest orbital forecast").apply { isDaemon = true } }, ThreadPoolExecutor.AbortPolicy())
