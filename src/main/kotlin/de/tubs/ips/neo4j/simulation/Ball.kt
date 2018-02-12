package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.ResourceIterable

class Ball(val center: Node, diameter: Int) : IDB {
    private val nodes = HashSet<Node>()

    override fun getNodes(): Iterable<Node> {
        return nodes
    }

    init {
        nodes.add(center)
        init(center, diameter)
    }

    /**
     * very slow...
     */
    private fun init(node: Node, diameter: Int) {
        if (diameter == 0) {
            return
        }

        for (relationship in node.relationships) {
            val next = relationship.getOtherNode(node)
            nodes.add(next)
            init(next, diameter - 1)
        }
    }

    override fun toString(): String {
        return nodes.size.toString()
    }

    companion object {
        fun createBalls(nodes: ResourceIterable<Node>, diameter: Int): Sequence<Ball> {
            return nodes.asSequence().map { Ball(it, diameter) }
        }
    }
}