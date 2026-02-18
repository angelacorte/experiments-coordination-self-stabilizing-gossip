package it.unibo.collektive.alchemist.device.sensors

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface TimeSensor {
    fun getTimeAsDouble(): Double

    @OptIn(ExperimentalTime::class)
    fun getTimeAsInstant(): Instant
}
