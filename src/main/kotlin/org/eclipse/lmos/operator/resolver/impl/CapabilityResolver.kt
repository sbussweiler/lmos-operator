/*
 * SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.lmos.operator.resolver.impl

import org.eclipse.lmos.operator.resolver.ResolveStrategy
import org.eclipse.lmos.operator.resolver.Wire
import org.eclipse.lmos.operator.resources.AgentResource
import org.semver4j.Semver

object CapabilityResolver {
    /**
     * Finds the best matching Wire based on the resolve strategy.
     *
     * @param wires           the set of wires to filter
     * @param resolveStrategy the resolve strategy to use
     * @return the best matching Wire
     */
    fun findBestMatchingWire(
        wires: Set<Wire<AgentResource>>,
        resolveStrategy: ResolveStrategy,
    ): Wire<AgentResource>? =
        if (resolveStrategy == ResolveStrategy.MOST_SIMILAR) {
            // Implement the logic for MOST_SIMILAR strategy here
            wires.minByOrNull { w -> Semver(w.providedCapability.version) }
        } else {
            wires.maxByOrNull { w -> Semver(w.providedCapability.version) }
        }
}
