package it.unibo.collektive.stdlib.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.aggregate.values
import it.unibo.collektive.stdlib.collapse.fold

/**
 * Self-stabilizing min/max consensus gossip based on *path-loop detection*.
 *
 * This module provides a single reusable primitive, [findMaxOf], that can be used to compute a network-wide
 * extremum (min or max) depending on the comparator passed.
 *
 * The algorithm disseminates a [GossipValue] that contains:
 * - the current best candidate value;
 * - the path (sequence of node IDs) the candidate has traversed.
 *
 * Nodes discard candidates whose path already contains the local node ID. This prevents reinforcing cycles
 * and is the mechanism that makes the protocol self-stabilizing after topology changes.
 */
object FindMaxOf {
    /**
     * Payload shared among neighbors for the [findMaxOf] algorithm.
     *
     * @param ID the node identifier type.
     * @param Value the gossiped value type.
     * @property best the current best candidate according to the comparator supplied to [findMaxOf].
     * @property path the (possibly empty) sequence of node IDs traversed by this candidate so far.
     */
    data class GossipValue<ID : Comparable<ID>, Value>(
        @JvmField
        val best: Value,
        @JvmField
        val path: List<ID> = emptyList(),
    ) {
        /**
         * Returns a copy of this value with [id] appended to [path].
         */
        fun addHop(id: ID) = GossipValue(best, path + id)
    }

    /**
     * Gossip-based network extremum.
     *
     * At each round, the node compares its current candidate ([local]) with the best candidates received from
     * neighbors, discarding those that would introduce a loop (`localId in neighbor.path`).
     *
     * Tie-breaking rules when values compare as equal:
     * 1. Prefer the candidate with the *shorter* path (heuristic: it likely reached us through fewer hops).
     * 2. If path length is the same, prefer the candidate whose last hop ID is smaller.
     *
     * @param local the local value to inject into the gossip.
     * @param comparator comparator defining what "better" means (e.g., natural order for min, reversed for max).
     * @return the best candidate value currently known by this node.
     */
    inline fun <reified ID : Comparable<ID>, Value> Aggregate<ID>.findMaxOf(
        local: Value,
        comparator: Comparator<in Value>,
    ): Value {
        val localGossip = GossipValue<ID, Value>(best = local)
        return share(localGossip) { gossip ->
            gossip.neighbors.values
                .fold(localGossip) { current, neighbor ->
                    when {
                        localId in neighbor.path -> current
                        // Ignore paths that loop back
                        else -> {
                            when (comparator.compare(neighbor.best, current.best)) {
                                0 -> when { // If values tie, select based on path
                                        neighbor.path.size == current.path.size -> {
                                            // If paths length tie, compare their last element
                                            // (they necessarily come from different neighbors)
                                            listOf(neighbor, current).minBy { it.path.last() }
                                        }
                                        // Select the shortest path
                                        else -> listOf(neighbor, current).minBy { it.path.size }
                                    }
                                // Pick the best value according to the comparator
                                in 1..Int.MAX_VALUE -> neighbor
                                else -> current
                            }
                        }
                    }
                }.addHop(localId)
        }.best
    }
}
