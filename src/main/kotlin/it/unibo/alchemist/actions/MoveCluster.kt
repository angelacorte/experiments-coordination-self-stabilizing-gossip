package it.unibo.alchemist.actions

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.AbstractAction

/**
 * An action that moves a node within an environment by modifying its position within a specified range.
 *
 * This class performs a movement operation on a given node, altering its position
 * within the environment by adding the specified `movingRange` to the x-coordinate value,
 * while maintaining the y-coordinate unchanged. The motion is constrained to the environment's bounds.
 *
 * @param T The type of the concentration present in the node.
 * @param P The type of the position used in the environment.
 * @property environment The environment in which the node operates.
 * @property node The node being moved.
 * @property movingRange The range of movement for the node along the x-axis.
 */
class MoveCluster<T : Any, P : Position<P>>(
    private val environment: Environment<T, P>,
    private val node: Node<T>,
    private val movingRange: Double,
) : AbstractAction<T>(node) {
    override fun cloneAction(
        node: Node<T>,
        reaction: Reaction<T>,
    ): Action<T> = MoveCluster(environment, node, movingRange)

    override fun execute() {
        val nodePosition = environment.getPosition(node).coordinates
        val newPosition = (nodePosition[0] + movingRange) to nodePosition[1]
        environment.moveNodeToPosition(node, environment.makePosition(newPosition.first, newPosition.second))
    }

    override fun getContext(): Context = Context.NEIGHBORHOOD
}
