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
    override fun checkpoint(): ByteArray = byteArrayOf()

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

    private fun unavailable(): AddonCallResult<GravitySample> =
        addonFailed(HostFailureKind.UNAVAILABLE, "Computer must be on a loaded construction in an Orbitalis gravity chart")
}
