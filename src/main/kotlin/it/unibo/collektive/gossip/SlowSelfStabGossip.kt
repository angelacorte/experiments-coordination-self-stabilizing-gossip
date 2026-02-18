package it.unibo.collektive.gossip

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.share
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.stdlib.collapse.fold

object SlowSelfStabGossip {
    inline fun <reified ID : Comparable<ID>, Type> Aggregate<ID>.slowSelfStabGossip(
        env: EnvironmentVariables,
        initial: Type,
        crossinline selector: (Type, Type) -> Boolean,
    ): Type {
        val local = GossipValue(initial, listOf(localId))
        return share(local) { gossip ->
            val result =
                gossip.neighbors.fold(local) { current, next ->
                    when {
                        selector(current.value, next.value.value) || localId in next.value.path -> current
                        else -> next.value.copy(path = next.value.path + localId)
                    }
                }
            env["neighbors-size"] = gossip.neighbors.size
            env["path"] = result.path.joinToString("->")
            env["path-length"] = result.path.size
            result
        }.value
    }

    data class GossipValue<ID : Comparable<ID>, Type>(
        @JvmField
        val value: Type,
        @JvmField
        val path: List<ID>,
    )
}
