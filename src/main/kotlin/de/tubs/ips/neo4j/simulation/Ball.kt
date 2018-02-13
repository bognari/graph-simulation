package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.ResourceIterable
import java.util.*

class Ball(val center: Node, diameter: Int) : IDB {
    private val nodes = HashSet<Node>()

    override fun getNodes(): Iterable<Node> {
        return nodes
    }

    init {
        nodes.add(center)
        init(center, diameter)
    }

    private fun init(node: Node, diameter: Int) {
        val queue: Queue<Pair<Node, Int>> = LinkedList()
        queue.add(Pair(node, 0))

        while (true) {
            val p: Pair<Node, Int> = queue.poll() ?: return
            val n = p.first
            val d = p.second

            p.first.relationships
                    .asSequence()
                    .map { it.getOtherNode(n) }
                    .filter { nodes.add(it) && d + 1 < diameter }
                    .mapTo(queue) { Pair(it, d + 1) }
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