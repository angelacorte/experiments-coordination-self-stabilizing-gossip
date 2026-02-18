package it.unibo.collektive.experiments

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import it.unibo.collektive.utils.randomFromTimeElapsed

/**
 * Entrypoint for the *non-stabilizing* min/max consensus gossip.
 *
 * This is a baseline algorithm (it does not self-stabilize): after a topology change, the network may keep
 * propagating stale information indefinitely.
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
fun Aggregate<Int>.nonStabGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
): Double {
    val selector: (Double, Double) -> Double = if (env["findMax"]) ::maxOf else ::minOf
    return nonStabilizingGossip(
        value = randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it },
        reducer = selector,
    ).also { env["gossip-value"] = it }
}
