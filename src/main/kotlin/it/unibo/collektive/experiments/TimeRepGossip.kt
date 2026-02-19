package it.unibo.collektive.experiments

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.alchemist.util.Environments.networkDiameter
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.stdlib.processes.timeReplicated
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import it.unibo.collektive.utils.randomFromTimeElapsed
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Runs [process] under the time-replication wrapper.
 *
 * Current parameters are intentionally kept small and constant for experiments:
 * - `maxReplicas = 4`
 * - `timeToSpawn = 9s`
 *
 * @param device the Alchemist/Collektive bridge, used here to read the current simulation time.
 * @param process the computation to replicate.
 */
@OptIn(ExperimentalTime::class)
inline fun <reified Value> Aggregate<Int>.timeRepGossip(
    device: CollektiveDevice<*>,
    noinline process: () -> Value,
): Value {
    val currentTime = device.getTimeAsInstant()
    return timeReplicated(
        currentTime = currentTime,
        maxReplicas = 4,
        timeToSpawn = 9.seconds,
        process = process,
    )
}

/**
 * Entrypoint for time-replicated *non-stabilizing* gossip.
 *
 * Expected environment variables (typically configured in YAML):
 * - `findMax`: `Boolean`. If true, compute/max-gossip; otherwise compute/min-gossip.
 *
 * Produced environment variables:
 * - `local-value`: `Double`. The node's local value for the current run.
 * - `gossip-value`: `Double`. The current best estimate of the global min/max.
 */
fun Aggregate<Int>.timeRepGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
): Double {
    device.environment.networkDiameter()

    val localValue = randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it }
    val selector: (Double, Double) -> Double = if (env["findMax"]) ::maxOf else ::minOf
    return timeRepGossip(
        device = device,
        process = { nonStabilizingGossip(value = localValue, reducer = selector) },
    ).also { env["gossip-value"] = it }
}

