package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.util.Environments.allSubNetworksWithHopDistance
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

class ErrorExtractor(
    val shouldFindMax: Boolean = true,
) : Extractor<Double> {
    override val columnNames: List<String>
        get() = listOf("RMSE", "MAE", "MEAN")

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        val localMolecule = SimpleMolecule("local-value")
        val gossipMolecule = SimpleMolecule("gossip-value")
        val subnetworks = environment.allSubNetworksWithHopDistance()
        var rmse = 0.0
        var mae = 0.0
        var mean = 0.0
        subnetworks.forEach { subnetwork ->
            val available = subnetwork.nodes.filter { node -> node.contains(localMolecule) && node.contains(gossipMolecule) }
            val localValues = available.map { it.getConcentration(localMolecule) as Double }
            val shouldGossip =
                when {
                    shouldFindMax -> localValues.maxOrNull()
                    else -> localValues.minOrNull()
                }
            if (shouldGossip != null) {
                val values = available.map { it.getConcentration(gossipMolecule) as Double }
                val differences = values.map { gossiping -> (shouldGossip - gossiping) }
                rmse += sqrt(values.sumOf { gossiping -> (shouldGossip - gossiping).pow(2) } / values.size)
                val absolutes = values.map { gossiping -> (shouldGossip - gossiping).absoluteValue }
                mae += absolutes.sum() / absolutes.size
                mean += differences.sum() / values.size
//                mape += values.sumOf { gossiping -> (shouldGossip - gossiping) / shouldGossip } * (100 / absolutes.size)
            }
        }
        val errors = listOf(rmse, mae, mean).map { it / subnetworks.size }
        return columnNames.zip(errors).toMap()
    }
}
