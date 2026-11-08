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

package orbitalis.navigation

import ru.lazyhat.compukters.api.contracts.GuestRead
import ru.lazyhat.compukters.api.contracts.GuestRecord
import ru.lazyhat.compukters.api.contracts.GuestService
import ru.lazyhat.compukters.api.contracts.GuestWait
import kotlin.math.Vector3d

/** Earth-centred, nonrotating physical state. Axes map root directions into the native chart.
 * exclusionRadius encloses the rotating Earth and its return-to-surface band, including 64 m clearance.
 * mu is a far-field approximation only; the coast forecast uses the real noncentral, moving field.
 */
@GuestRecord
value class NavigationSample(
    val gameTick: Long,
    val constructionId: String,
    val dimension: String,
    val position: Vector3d,
    val velocity: Vector3d,
    val gravity: Vector3d,
    val rootX: Vector3d,
    val rootY: Vector3d,
    val rootZ: Vector3d,
    val earthMu: Double,
    val exclusionRadius: Double,
)

/** Engines-off prediction from a captured state, not a manoeuvre recommendation.
 * ORBIT is a nearly closed turn; REVOLUTION is a complete nonclosed turn.
 * Other endings do not demonstrate an orbit. Times are seconds; distances are blocks.
 */
@GuestRecord
value class CoastSummary(
    val sampledTick: Long,
    val constructionId: String,
    val dimension: String,
    val end: String,
    val seconds: Double,
    val minimumRadius: Double,
    val maximumRadius: Double,
)

/** State measured in the rotating Earth terrain frame, also when the ship is in Space.
 * acceleration is the engines-off derivative of velocity, including transport terms.
 * earthX/Y/Z rotate Earth force vectors into the current native Sable chart.
 */
@GuestRecord
value class EarthSample(
    val gameTick: Long,
    val constructionId: String,
    val dimension: String,
    val position: Vector3d,
    val velocity: Vector3d,
    val acceleration: Vector3d,
    val earthX: Vector3d,
    val earthY: Vector3d,
    val earthZ: Vector3d,
    val halfWidth: Double,
    val halfDepth: Double,
    val entryDistance: Double,
    val halfHeight: Double,
)

/** First engines-off crossing of the rotating Earth's real return envelope within 600 s.
 * If willEnter is false, position/velocity describe the terminal forecast state instead.
 * Ignores engines, collisions and atmospheric drag; not a landing recommendation.
 */
@GuestRecord
value class EntrySummary(
    val sampledTick: Long,
    val constructionId: String,
    val dimension: String,
    val willEnter: Boolean,
    val seconds: Double,
    val position: Vector3d,
    val velocity: Vector3d,
)

@GuestService(bindings = "Navigation")
interface Navigation {
    @GuestRead(binding = "observe")
    fun observe(): NavigationSample

    @GuestRead(binding = "earth")
    fun earth(): EarthSample

    @GuestWait(binding = "entry")
    fun entry(): EntrySummary

    /** Bounded asynchronous work. Capture on the server; pure forecast on a worker.
         * Call after releasing thrust; the model omits engines and other non-gravity forces.
         */
    @GuestWait(binding = "coast")
    fun coast(): CoastSummary
}
