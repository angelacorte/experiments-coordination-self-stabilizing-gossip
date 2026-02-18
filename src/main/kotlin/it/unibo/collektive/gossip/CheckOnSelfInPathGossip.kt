package it.unibo.collektive.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables

/**
 * Provides a mechanism for gossip-based value propagation,
 * incorporating a check to avoid processing self-referential paths.
 *
 * The `checkOnSelfInPathGossip` function implements a gossiping protocol that propagates values within a system.
 * The function ensures the "best" value is selected based on a comparator,
 * while preventing propagation cycles by avoiding self-referential paths.
 *
 * @param env The environment variables that provide external information, such as the size of neighbors or the path details.
 * @param initial The initial value to be used and propagated within the gossiping process.
 * @param selector A comparator that dictates the logic for determining the highest priority value.
 * @return The highest priority propagated value based on the provided comparator and the gossiping process.
 */
object CheckOnSelfInPathGossip {
    inline fun <reified ID : Comparable<ID>, Value> Aggregate<ID>.checkOnSelfInPathGossip(
        env: EnvironmentVariables,
        initial: Value,
        selector: Comparator<Value>,
    ): Value {
        val local = GossipValue<ID, Value>(initial, initial)
        return share(local) { gossip ->
            val result =
                gossip.neighbors.list.fold(local) { current, (id, next) ->
                    val actualNext = if (localId in next.path) next.base(id) else next
                    val candidateValue = selector.compare(current.best, actualNext.best)
                    when {
                        candidateValue > 0 -> current
                        candidateValue == 0 -> listOf(current, next).minBy { it.path.size }
                        else -> actualNext
                    }
                }
            env["neighbors-size"] = gossip.neighbors.size
            env["path"] = result.path.joinToString("->")
            env["path-length"] = result.path.size
            GossipValue(result.best, initial, result.path + localId)
        }.best
    }

    /**
     * Represents a value used in a gossip protocol, containing the [best]-known value,
     * the [local]ly stored value, and the [path] of identifiers associated with the value's propagation.
     *
     * @property best The best-known value based on the gossip algorithm.
     * @property local The local value stored by the current node.
     * @property path The list of identifiers tracing the propagation of the value.
     */
    data class GossipValue<ID : Comparable<ID>, Value>(
        @JvmField
        val best: Value,
        @JvmField
        val local: Value,
        @JvmField
        val path: List<ID> = emptyList(),
    ) {
        fun base(id: ID) = GossipValue(local, local, listOf(id))
    }
}
