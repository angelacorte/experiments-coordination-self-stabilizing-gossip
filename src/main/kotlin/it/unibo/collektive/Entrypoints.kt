package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.experiments.gossipMin
import it.unibo.collektive.stdlib.processes.timeReplicated
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Uses last version of self-stabilizing gossip before path-gradient optimization.
 */
fun Aggregate<Int>.gossipEntrypoint(
    env: EnvironmentVariables,
) = gossipMin(localId).also { env["gossip-value"] = it }

/**
 * Entrypoint for the non stabilizing gossip replicated with timeReplicated.
 */
@OptIn(ExperimentalTime::class)
fun Aggregate<Int>.timeReplicatedGossipEntrypoint(
    env: EnvironmentVariables,
    device: CollektiveDevice<*>,
): Int {
    val currentTime: Instant = Instant.fromEpochSeconds(device.currentTime.toDouble().toLong())
    env["time"] = currentTime
    return timeReplicated(
        currentTime = currentTime,
        maxReplicas = 4,
        timeToSpawn = 3.seconds,
        process = { nonStabilizingGossip(localId, reducer = ::maxOf).also { env["process"] = it } },
    ).also { env["gossip-value"] = it }
}
