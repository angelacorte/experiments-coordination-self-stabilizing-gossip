package it.unibo.collektive.alchemist.device.sensors.impl

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.util.RandomGenerators.nextDouble
import org.apache.commons.math3.random.RandomGenerator

/**
 * Alchemist [NodeProperty] that exposes a seeded [RandomGenerator] as a Collektive [it.unibo.collektive.alchemist.device.sensors.RandomGenerator].
 *
 * This is meant to be installed on each node via YAML, so aggregate programs can access deterministic
 * randomness (deterministic with respect to the simulation seed).
 */
class RandomNodeProperty<T>(
    override val node: Node<T>,
    private val randomGenerator: RandomGenerator,
) : it.unibo.collektive.alchemist.device.sensors.RandomGenerator,
    NodeProperty<T> {
    override fun cloneOnNewNode(node: Node<T>): NodeProperty<T> = RandomNodeProperty(node, randomGenerator)

    override fun nextRandomDouble(): Double = randomGenerator.nextDouble()

    override fun nextRandomDouble(until: Double): Double = randomGenerator.nextDouble(0.0, until)

    override fun nextRandomDouble(range: ClosedFloatingPointRange<Double>): Double =
        randomGenerator.nextDouble(range.start, range.endInclusive)

    override fun nextGaussian(): Double = randomGenerator.nextGaussian()
}
