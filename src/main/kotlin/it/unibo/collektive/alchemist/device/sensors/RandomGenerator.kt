package it.unibo.collektive.alchemist.device.sensors

/**
 * Seeded random generator exposed to aggregate programs.
 *
 * In Alchemist simulations, the actual implementation is typically provided as a node property that wraps the
 * simulation RNG. Keeping it behind an interface makes it easier to inject/mocks the generator in tests.
 */
interface RandomGenerator {
    /**
     * @return a random double in `[0.0, 1.0)` using the environment seed.
     */
    fun nextRandomDouble(): Double

    /**
     * @param until exclusive upper bound.
     * @return a random double in `[0.0, until)` using the environment seed.
     */
    fun nextRandomDouble(until: Double): Double

    /**
     * @param range a closed range.
     * @return a random double in `[range.start, range.endInclusive]` using the environment seed.
     */
    fun nextRandomDouble(range: ClosedFloatingPointRange<Double>): Double

    /**
     * @return a random value sampled from a standard Gaussian distribution.
     */
    fun nextGaussian(): Double
}
