package it.unibo.collektive.experiments

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.stdlib.gossip.FindMaxOf.findMaxOf
import it.unibo.collektive.utils.randomFromTimeElapsed

/**
 * Minimal entrypoint that runs the self-stabilizing gossip algorithm using the node id as local value.
 *
 * It's mainly useful as a smoke test/demo because every node starts with a deterministic value.
 * The resulting consensus is written into the environment as the molecule/variable `gossip-value`.
 *
 * @param env variables/molecules exposed by the simulator (Alchemist), used here only for output.
 */
fun Aggregate<Int>.gossipEntrypoint(env: EnvironmentVariables) = gossipMin(localId).also { env["gossip-value"] = it }

/**
 * Entrypoint for the self-stabilizing min/max consensus gossip based on path-loop detection.
 *
 * The node generates a local random value (derived from the simulated time via [randomFromTimeElapsed])
 * and gossips either the global minimum or maximum depending on the `findMax` configuration variable.
 *
 * Expected environment variables (typically configured in YAML):
 * - `findMax`: `Boolean`. If true, compute/max-gossip; otherwise compute/min-gossip.
 *
 * Produced environment variables:
 * - `local-value`: `Double`. The node's local value for the current run.
 * - `gossip-value`: `Double`. The current best estimate of the global min/max.
 *
 * @return the current estimate of the global min/max.
 */
fun Aggregate<Int>.selfStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double {
    val local = randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it }
    return if (env["findMax"]) {
        gossipMax(local)
    } else {
        gossipMin(local)
    }.also { env["gossip-value"] = it }
}

/**
 * A convenience comparator that reverses the natural order.
 */
fun <T : Comparable<T>> compareValuesReversed(
    a: T,
    b: T,
) = compareValues(b, a)

/**
 * Self-stabilizing *minimum* gossip.
 *
 * Internally this is implemented by reusing [findMaxOf] with the natural order comparator,
 * so that the "best" value is the smallest one.
 */
inline fun <reified ID : Comparable<ID>, T : Comparable<T>> Aggregate<ID>.gossipMin(local: T): T =
    findMaxOf(
        local,
        comparator = ::compareValues,
    )

/**
 * Self-stabilizing *maximum* gossip.
 *
 * Internally this is implemented by reusing [findMaxOf] with a reversed comparator,
 * so that the "best" value is the largest one.
 */
inline fun <reified ID : Comparable<ID>, T : Comparable<T>> Aggregate<ID>.gossipMax(local: T): T =
    findMaxOf(
        local,
        comparator = ::compareValuesReversed,
    )
