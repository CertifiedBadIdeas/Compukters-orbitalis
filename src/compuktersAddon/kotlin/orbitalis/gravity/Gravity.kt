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

import kotlin.math.Vector3d

/** Atomic server observation at native centre of mass, in the current dimension's chart.
 * Acceleration includes Orbitalis frame terms. Units are blocks, seconds, kpg and m/s².
 */
public value class GravitySample(
    public val gameTick: Long,
    public val constructionId: String,
    public val dimension: String,
    public val computerPlotPosition: Vector3d,
    public val position: Vector3d,
    public val velocity: Vector3d,
    public val acceleration: Vector3d,
    public val mass: Double,
)

/** Read-only field sampling; no guidance, forces, engine input or tick handlers. */
public object Gravity {
    external fun sample(): GravitySample
}
