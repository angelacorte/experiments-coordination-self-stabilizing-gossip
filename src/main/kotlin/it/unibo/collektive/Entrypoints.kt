package it.unibo.collektive

import it.unibo.alchemist.collektive.device.CollektiveDevice
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.alchemist.device.sensors.RandomGenerator
import it.unibo.collektive.alchemist.device.sensors.TimeSensor
import it.unibo.collektive.experiments.gossipMax
import it.unibo.collektive.experiments.gossipMin
import it.unibo.collektive.gossip.CheckOnSelfInPathGossip.checkOnSelfInPathGossip
import it.unibo.collektive.gossip.SlowSelfStabGossip.slowSelfStabGossip
import it.unibo.collektive.gossip.genericGossip
import it.unibo.collektive.stdlib.ints.FieldedInts.toDouble
import it.unibo.collektive.stdlib.processes.timeReplicated
import it.unibo.collektive.stdlib.spreading.isHappeningAnywhere
import it.unibo.collektive.stdlib.spreading.nonStabilizingGossip
import it.unibo.collektive.stdlib.util.hops
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Uses Gossip in the standard library.
 */
fun Aggregate<Int>.gossipStdlibEntrypoint(env: EnvironmentVariables) =
    gossipMax(localId).also {
        env["gossip-value"] = it
    }

/**
 * Gossip that uses common path evaluation logic with gradient.
 */
fun Aggregate<Int>.genericGossipEntrypoint(env: EnvironmentVariables): Int =
    genericGossip(
        local = localId,
        bottom = 0.0,
        top = Double.POSITIVE_INFINITY,
        metric = hops().toDouble(),
        selector = ::maxOf,
        accumulateDistance = Double::plus,
    ).also {
        env["gossip-value"] = it
    }

/**
 * Uses last version of self-stabilizing gossip before path-gradient optimization.
 */
fun Aggregate<Int>.thirdGossipEntrypoint(
    env: EnvironmentVariables,
    timeSensor: TimeSensor,
    randomGenerator: RandomGenerator,
) = gossipMin(localId).also { env["gossip-value"] = it }

/**
 * Entrypoint for the simulation of the second implementation of the gossip algorithm with Collektive.
 */
fun Aggregate<Int>.secondGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = checkOnSelfInPathGossip(
    env,
    initial = localId,
//    randomFromTimeElapsed(timeSensor, randomGenerator)
//        .also { env["local-value"] = it },
) { first, second ->
    first.compareTo(second)
}.also { env["gossip-value"] = it }

/**
 * Entrypoint for the simulation of the first implementation of the gossip algorithm with Collektive.
 */
fun Aggregate<Int>.firstGossipEntrypoint(
    env: EnvironmentVariables,
    randomGenerator: RandomGenerator,
    timeSensor: TimeSensor,
) = slowSelfStabGossip(
    env,
    initial = localId,
//    randomFromTimeElapsed(timeSensor, randomGenerator).also { env["local-value"] = it },
) { first, second ->
    first >= second
}.also { env["gossip-value"] = it }

/**
 * Entrypoint for the simulation of the `isHappening` gossip function defined into Collektive's DSl.
 */
fun Aggregate<Int>.isHappeningGossipEntrypoint(timeSensor: TimeSensor) =
    isHappeningAnywhere {
        (timeSensor.getTimeAsDouble() % 100 < 50) && (localId % 2 == 0)
    }

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
