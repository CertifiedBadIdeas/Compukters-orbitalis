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

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.cbi.orbitalis.minecraft.integration.sable.ShipTransferParticipant
import ru.lazyhat.compukters.api.addon.minecraft.CompuktersPeripheralWorldIntegration as Integration

/** Adapts lifecycle only. Flight decisions and actuator commands belong exclusively to Guest code. */
internal object CompuktersShipTransferParticipant : ShipTransferParticipant {
    override fun prepare(source: ServerLevel, positions: Set<BlockPos>): ShipTransferParticipant.Preparation {
        val guard = Integration.guardTransfer(source, positions)
        return try {
            val completion = Integration.prepareTransferAsync(source, positions)
            object : ShipTransferParticipant.Preparation {
                override val ready = completion
                override fun close() = guard.close()
            }
        } catch (failure: Exception) {
            guard.close()
            throw failure
        }
    }

    override fun stage(
        source: ServerLevel,
        destination: ServerLevel,
        positions: Map<BlockPos, BlockPos>,
    ): ShipTransferParticipant.Scope {
        val move = Integration.beginTransfer(source, destination, positions)
        val sourceGuard = Integration.guardComputers(source, positions.keys)
        val destinationGuard = Integration.guardComputers(destination, positions.values)
        return object : ShipTransferParticipant.Scope {
            override fun resolve() = move.close()
            override fun close() {
                try { move.close() } finally {
                    destinationGuard.close()
                    sourceGuard.close()
                }
            }
        }
    }

    override fun recover(
        source: ServerLevel,
        destination: ServerLevel,
        positions: Map<BlockPos, BlockPos>,
        committed: Boolean,
    ): ShipTransferParticipant.Scope {
        val recovery = Integration.recoverTransfer(source, destination, positions, committed)
        val sourceGuard = Integration.guardComputers(source, positions.keys)
        val destinationGuard = Integration.guardComputers(destination, positions.values)
        return object : ShipTransferParticipant.Scope {
            override fun resolve() = recovery.resolve()
            override fun close() {
                try { recovery.close() } finally {
                    destinationGuard.close()
                    sourceGuard.close()
                }
            }
        }
    }
}
