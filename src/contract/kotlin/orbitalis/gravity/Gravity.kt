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

package orbitalis.gravity

import ru.lazyhat.compukters.api.contracts.GuestRead
import ru.lazyhat.compukters.api.contracts.GuestRecord
import ru.lazyhat.compukters.api.contracts.GuestService
import kotlin.math.Vector3d

/** Atomic server observation at native centre of mass, in the current dimension's chart.
 * Acceleration includes Orbitalis frame terms. Units are blocks, seconds, kpg and m/s².
 */
@GuestRecord
value class GravitySample(
    val gameTick: Long,
    val constructionId: String,
    val dimension: String,
    val computerPlotPosition: Vector3d,
    val position: Vector3d,
    val velocity: Vector3d,
    val acceleration: Vector3d,
    val mass: Double,
)

/** Read-only field sampling; no guidance, forces, engine input or tick handlers. */
@GuestService(bindings = "Gravity")
interface Gravity {
    @GuestRead(binding = "sample")
    fun sample(): GravitySample
}
