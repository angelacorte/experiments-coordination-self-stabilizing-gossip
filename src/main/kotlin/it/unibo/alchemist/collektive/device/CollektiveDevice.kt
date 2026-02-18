package it.unibo.alchemist.collektive.device

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.NodeProperty
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.collektive.aggregate.Field
import it.unibo.collektive.aggregate.api.Aggregate
import it.unibo.collektive.aggregate.api.DataSharingMethod
import it.unibo.collektive.aggregate.api.neighboring
import it.unibo.collektive.alchemist.device.sensors.EnvironmentVariables
import it.unibo.collektive.networking.Mailbox
import it.unibo.collektive.networking.Message
import it.unibo.collektive.networking.NeighborsData
import it.unibo.collektive.networking.NoNeighborsData
import it.unibo.collektive.networking.OutboundEnvelope
import it.unibo.collektive.path.Path
import it.unibo.collektive.stdlib.gossip.FindMaxOf
import it.unibo.collektive.stdlib.processes.TimedReplica
import it.unibo.collektive.stdlib.util.PathValue
import org.apache.commons.math3.random.RandomGenerator
import java.io.ByteArrayOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Representation of a Collektive device (as a [node]) in an Alchemist [environment] with a [randomGenerator].
 * [P] is the position type,
 * [retainMessagesFor] is the time for which messages are retained.
 */
class CollektiveDevice<P>(
    val randomGenerator: RandomGenerator,
    val environment: Environment<Any?, P>,
    override val node: Node<Any?>,
    private val retainMessagesFor: Time? = null,
) : NodeProperty<Any?>,
    Mailbox<Int>,
    EnvironmentVariables where P : Position<P> {
    private data class TimedMessage(
        val receivedAt: Time,
        val payload: Message<Int, *>,
    )

    override val inMemory: Boolean = true

    /**
     * The current time.
     */
    val currentTime: Time
        get() = environment.simulationOrNull?.time ?: Time.ZERO

    @OptIn(ExperimentalTime::class)
    fun getTimeAsInstant(): Instant {
        val time: Double = environment.simulation.time.toDouble()
        return Instant.DISTANT_PAST + time.seconds
    }

    /**
     * The ID of the node (alias of [localId]).
     */
    val id = node.id

    /**
     * The ID of the node (alias of [id]).
     */
    val localId = node.id

    private val validMessages: MutableMap<Int, TimedMessage> = mutableMapOf()

    private fun receiveMessage(
        time: Time,
        message: Message<Int, *>,
    ) {
        validMessages += message.senderId to TimedMessage(time, message)
    }

    /**
     * Returns the distances to the neighboring nodes.
     */
    fun <ID : Any> Aggregate<ID>.distances(): Field<ID, Double> =
        environment.getPosition(node).let { nodePosition ->
            neighboring(nodePosition.coordinates).mapValues { position ->
                nodePosition.distanceTo(environment.makePosition(position))
            }
        }

    override fun cloneOnNewNode(node: Node<Any?>): NodeProperty<Any?> =
        CollektiveDevice(randomGenerator, environment, node, retainMessagesFor)

    @OptIn(ExperimentalTime::class)
    override fun deliverableFor(outboundMessage: OutboundEnvelope<Int>) {
        val defaultHashedPathSize = 32
        val kryo = Kryo()
        kryo.register(Int::class.java)
        kryo.register(Set::class.java)
        kryo.register(Double::class.java)
        kryo.register(Instant::class.java)
        kryo.register(Boolean::class.java)
        kryo.register(Duration::class.java)
        kryo.register(ArrayList::class.java)
        kryo.register(PathValue::class.java)
        kryo.register(TimedReplica::class.java)
        kryo.register(FindMaxOf.GossipValue::class.java)
        kryo.register(Class.forName($$"java.util.Collections$SingletonSet"))
        if (outboundMessage.isNotEmpty()) {
            val neighborsNodes = environment.getNeighborhood(node)
            if (!neighborsNodes.isEmpty) {
                val neighborhood =
                    neighborsNodes.mapNotNull { node ->
                        @Suppress("UNCHECKED_CAST")
                        node.properties.firstOrNull { it is CollektiveDevice<*> } as? CollektiveDevice<P>
                    }
                var messageSizeAccumulator = 0
                neighborhood.forEach { neighbor ->
                    val message: Message<Int, *> = outboundMessage.prepareMessageFor(neighbor.id)
                    val messageValues = message.sharedData
                    val buff = ByteArrayOutputStream()
                    Output(buff).use { buffer ->
                        messageValues.forEach { (_: Path, value) ->
                            kryo.writeClassAndObject(buffer, value)
                        }
                    }
                    messageSizeAccumulator += buff.size() + defaultHashedPathSize * messageValues.size
                    neighbor.deliverableReceived(message)
                    node.setConcentration(SimpleMolecule("MessageSize"), messageSizeAccumulator)
                }
            }
        }
    }

    override fun deliverableReceived(message: Message<Int, *>) {
        receiveMessage(currentTime, message)
    }

    override fun currentInbound(): NeighborsData<Int> {
        if (validMessages.isEmpty()) {
            return NoNeighborsData()
        }
        val messages: Map<Int, Message<Int, *>> =
            when {
                retainMessagesFor == null -> {
                    validMessages.mapValues { it.value.payload }.also { validMessages.clear() }
                }

                else -> {
                    validMessages.values.retainAll { it.receivedAt + retainMessagesFor >= currentTime }
                    validMessages.mapValues { it.value.payload }
                }
            }
        return object : NeighborsData<Int> {
            override val neighbors: Set<Int> get() = messages.keys

            @Suppress("UNCHECKED_CAST")
            override fun <Value> dataAt(
                path: Path,
                dataSharingMethod: DataSharingMethod<Value>,
            ): Map<Int, Value> =
                // Messages need to be filtered by the path to allow for null values
                messages
                    .filterValues { message -> message.sharedData.containsKey(path) }
                    .mapValues { (_, message) -> message.sharedData[path] as Value }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(name: String): T = node.getConcentration(SimpleMolecule(name)) as T

    override fun <T> getOrNull(name: String): T? =
        when {
            isDefined(name) -> get(name)
            else -> null
        }

    override fun <T> getOrDefault(
        name: String,
        default: T,
    ): T = getOrNull(name) ?: default

    override fun isDefined(name: String): Boolean = node.contains(SimpleMolecule(name))

    override fun <T> set(
        name: String,
        value: T,
    ): T = value.also { node.setConcentration(SimpleMolecule(name), it) }
}
