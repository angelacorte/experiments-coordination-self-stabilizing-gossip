package it.unibo.collektive.experiments

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.stdlib.processes.timeReplicated
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import it.unibo.collektive.utils.randomFromTimeElapsed
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Split and Merge experiments by using time replicated non stabilizing gossip.
 */
@OptIn(ExperimentalTime::class)
inline fun <reified Value> Aggregate<Int>.timeRepGossip(
    device: CollektiveDevice<*>,
    noinline process: () -> Value,
): Value {
    val currentTime: Instant = device.getTimeAsInstant()
    return timeReplicated(
        currentTime = currentTime,
        maxReplicas = 4,
        timeToSpawn = 3.seconds,
        process = process,
    )
}

fun Aggregate<Int>.timeRepGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    device: CollektiveDevice<*>,
    timeSensor: TimeSensor,
): Double {
    val localValue = randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it }
    val selector: (Double, Double) -> Double = if (env["findMax"]) ::maxOf else ::minOf
    return timeRepGossip(
        device = device,
        process = { nonStabilizingGossip(value = localValue, reducer = selector) },
    ).also { env["gossip-value"] = it }
}
