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

import kotlin.math.Vector3d

/** Earth-centred, nonrotating physical state. Axes map root directions into the native chart.
 * exclusionRadius encloses the rotating Earth and its return-to-surface band, including 64 m clearance.
 * mu is a far-field approximation only; the coast forecast uses the real noncentral, moving field.
 */
public value class NavigationSample(
    public val gameTick: Long,
    public val constructionId: String,
    public val dimension: String,
    public val position: Vector3d,
    public val velocity: Vector3d,
    public val gravity: Vector3d,
    public val rootX: Vector3d,
    public val rootY: Vector3d,
    public val rootZ: Vector3d,
    public val earthMu: Double,
    public val exclusionRadius: Double,
)

/** Engines-off prediction from a captured state, not a manoeuvre recommendation.
 * ORBIT is a nearly closed turn; REVOLUTION is a complete nonclosed turn.
 * Other endings do not demonstrate an orbit. Times are seconds; distances are blocks.
 */
public value class CoastSummary(
    public val sampledTick: Long,
    public val constructionId: String,
    public val dimension: String,
    public val end: String,
    public val seconds: Double,
    public val minimumRadius: Double,
    public val maximumRadius: Double,
)

public object Navigation {
    external fun observe(): NavigationSample
    /** Bounded asynchronous work. Capture on the server; pure forecast on a worker.
     * Call after releasing thrust; the model omits engines and other non-gravity forces.
     */
    external fun coast(): CoastSummary
}
