package it.unibo.collektive.utils

import it.unibo.alchemist.util.RandomGenerators.nextDouble
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor

fun Aggregate<Int>.keepRandom(
    randomGenerator: RandomGenerator,
    startInclusive: Double,
    endInclusive: Double,
) = evolve(randomGenerator.nextRandomDouble(startInclusive..endInclusive)) { it }

fun Aggregate<Int>.randomFromTimeElapsed(
    timeSensor: TimeSensor,
    randomGenerator: RandomGenerator,
) = when {
    timeSensor.getTimeAsDouble() <= 100.0 -> keepRandom(randomGenerator, -1.0, 1.0)
    else -> keepRandom(randomGenerator, -0.5, 0.5)
}
