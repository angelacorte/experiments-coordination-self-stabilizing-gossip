package it.unibo.alchemist.model.global

import it.unibo.alchemist.model.Action
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Condition
import it.unibo.alchemist.model.Dependency
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.GlobalReaction
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Obstacle2D
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.TimeDistribution
import it.unibo.alchemist.model.environments.Environment2DWithObstacles
import it.unibo.alchemist.model.obstacles.RectObstacle2D
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.danilopianini.util.ListSet
import org.danilopianini.util.ListSets

/**
 * A global reaction that manages the addition or removal of rectangular obstacles
 * in a 2D environment with obstacles.
 *
 * @param W The type of the obstacle, which must be a subtype of [Obstacle2D] with [Euclidean2DPosition].
 * @param T The type of time used in the simulation.
 * @property environment The 2D environment with obstacles where the reaction takes place.
 * @property distribution The time distribution governing the reaction's timing.
 * @property shouldAdd A flag indicating whether to add (true) or remove (false) an obstacle.
 * @property xPos The x-coordinate of the bottom-left corner of the rectangle.
 * @property yPos The y-coordinate of the bottom-left corner of the rectangle.
 * @property width The width of the rectangle.
 * @property height The height of the rectangle.
 */
class ManageObstacle<W : Obstacle2D<Euclidean2DPosition>, T>(
    val environment: Environment2DWithObstacles<W, T>,
    val distribution: TimeDistribution<T>,
    val shouldAdd: Boolean = true,
    val xPos: Double,
    val yPos: Double,
    val width: Double,
    val height: Double,
): GlobalReaction<T> {
    override var actions: List<Action<T>> = mutableListOf()
        set(value) {
            field = listOf(*value.toTypedArray())
        }

    override var conditions: List<Condition<T>> = mutableListOf()
        set(value) {
            field = listOf(*value.toTypedArray())
        }

    override val rate: Double
        get() = distribution.rate

    override val tau: Time
        get() = distribution.nextOccurence

    override val inboundDependencies: ListSet<out Dependency> = ListSets.emptyListSet()

    override val outboundDependencies: ListSet<out Dependency> = ListSets.emptyListSet()

    override val timeDistribution: TimeDistribution<T> = distribution

    override fun execute() {
        if (shouldAdd) {
            environment.addObstacle(RectObstacle2D(xPos, yPos, width, height) as W)
        } else {
            val obstacle: List<W> = environment.getObstaclesInRange(xPos, yPos, width)
            obstacle.forEach { environment.removeObstacle(it) }
        }
        distribution.update(timeDistribution.nextOccurence, true, rate, environment)
    }

    override fun canExecute(): Boolean = true

    override fun initializationComplete(
        atTime: Time,
        environment: Environment<T, *>,
    ) = Unit

    override fun update(
        currentTime: Time,
        hasBeenExecuted: Boolean,
        environment: Environment<T, *>,
    ) = Unit

    override fun compareTo(other: Actionable<T>): Int = tau.compareTo(other.tau)
}