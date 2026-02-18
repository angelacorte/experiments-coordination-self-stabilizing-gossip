package it.unibo.collektive.experiments

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.gossip.FindMaxOf.findMaxOf
import it.unibo.collektive.utils.randomFromTimeElapsed

fun Aggregate<Int>.selfStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double {
    val local = randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it }
    return if(env["findMax"]) {
        gossipMax(local)
    } else {
        gossipMin(local)
    }.also { env["gossip-value"] = it }
}

fun <T: Comparable<T>> compareValuesReversed(a: T, b: T) = compareValues(b, a)

inline fun <reified ID: Comparable<ID>, T : Comparable<T>> Aggregate<ID>.gossipMin(local: T): T = findMaxOf(
    local,
    comparator = ::compareValues,
)

inline fun <reified ID: Comparable<ID>, T : Comparable<T>> Aggregate<ID>.gossipMax(local: T): T = findMaxOf(
    local,
    comparator = ::compareValuesReversed,
)
