package it.unibo.collektive.utils

import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor

/**
 * Generates a random value once and then keeps it forever by storing it in local state via `evolve`.
 *
 * @param randomGenerator seeded RNG exposed through Alchemist.
 * @param startInclusive start of the open interval.
 * @param endInclusive end of the open interval.
 * @return a stable random value in `[startInclusive, endInclusive]`.
 */
fun Aggregate<Int>.keepRandom(
    randomGenerator: RandomGenerator,
    startInclusive: Double,
    endInclusive: Double,
) = evolve(randomGenerator.nextRandomDouble(startInclusive..endInclusive)) { it }

/**
 * Returns a stable random value whose range depends on the elapsed simulation time.
 *
 * For `t <= 100`, the range is `[-1, 1]`; afterward it shrinks to `[-0.5, 0.5]`.
 * This is used to introduce a controlled change in the input distribution without changing the value at every
 * simulation step.
 */
fun Aggregate<Int>.randomFromTimeElapsed(
    timeSensor: TimeSensor,
    randomGenerator: RandomGenerator,
) = when {
    timeSensor.getTimeAsDouble() <= 100.0 -> keepRandom(randomGenerator, -1.0, 1.0)
    else -> keepRandom(randomGenerator, -0.5, 0.5)
}
