package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.util.Environments.allSubNetworksWithHopDistance
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Extracts error metrics for the gossip consensus.
 *
 * The extractor expects each node to expose two molecules/variables:
 * - `local-value`: the local input value of the node
 * - `gossip-value`: the current node estimate of the global min/max
 *
 * For each connected component (subnetwork) in the environment, it computes the target value as either the
 * maximum or minimum of the available local values, then compares each node estimate against it.
 * Finally, it averages the metrics across subnetworks.
 *
 * Exported columns:
 * - `RMSE`: root mean squared error
 *
 * If [shouldFindMax] is true, the consensus target is the maximum of `local-value` in each component;
 * otherwise the minimum.
 */
class ErrorExtractor(
    val shouldFindMax: Boolean = true,
) : Extractor<Double> {
    override val columnNames: List<String>
        get() = listOf("RMSE")

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        val localMolecule = SimpleMolecule("local-value")
        val gossipMolecule = SimpleMolecule("gossip-value")
        val subnetworks = environment.allSubNetworksWithHopDistance()
        val rmse: Double = subnetworks.sumOf { subnetwork ->
            val available = subnetwork.nodes
                .filter { node -> node.contains(localMolecule) && node.contains(gossipMolecule) }
                .map { it.getConcentration(localMolecule) as Double to it.getConcentration(gossipMolecule) as Double }
            val expectedValue: Double? = when {
                shouldFindMax -> available.maxByOrNull { it.first }?.first
                else -> available.minByOrNull { it.first }?.first
            }
            when (expectedValue) {
                null -> 0.0
                else -> sqrt(available.sumOf { (_, gossiping) -> (expectedValue - gossiping).pow(2) }) / available.size
            }
        }
        return columnNames.zip(listOf(rmse)).toMap()
    }
}
