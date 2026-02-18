package it.unibo.collektive.experiments

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import it.unibo.collektive.utils.randomFromTimeElapsed

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
