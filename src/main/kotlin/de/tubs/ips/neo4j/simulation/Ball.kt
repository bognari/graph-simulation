package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.ResourceIterable


class Ball(val center: Node, diameter: Int) : IDB {
    override fun getNodes(): Iterable<Node> {
        return nodes
    }

    override fun getRelationships(): Iterable<Relationship> {
        return relationships
    }

    private val nodes: MutableSet<Node>
    private val relationships: MutableList<Relationship>

    init {
        this.nodes = HashSet()
        this.relationships = ArrayList()
        nodes.add(center)

        init(center, diameter)
    }

    private fun init(node: Node, diameter: Int) {
        if (diameter == 0) {
            return
        }

        for (relationship in node.relationships) {
            val next = relationship.getOtherNode(node)
            nodes.add(next)
            relationships.add(relationship)
            init(next, diameter - 1)
        }
    }

    override fun toString(): String {
        return String.format("%d | %d", nodes.size, relationships.size)
    }

    companion object {
        fun createBalls(nodes: ResourceIterable<Node>, diameter: Int): Sequence<Ball> {
            return nodes.asSequence()
                    .map { Ball(it, diameter) }
        }
    }
}