package de.tubs.ips.neo4j.graph

import org.jgrapht.alg.shortestpath.GraphMeasurer
import org.jgrapht.graph.SimpleGraph

class Group {

    private val mode : String
    val nodesDirectory: MutableMap<String, MyNode> = HashMap()
    val nodes: MutableSet<MyNode> = HashSet()
    val relationships: MutableSet<MyRelationship> = HashSet()
    val number: Int

    companion object {
        internal var number = 0
    }

    init {
        number = Group.number++
    }

    constructor() {
        mode = "MATCH"
    }

    /**
     * Unconnected anonymous nodes are not copied
     */
    constructor(other: Group) {
        mode = "OPTIONAL"

        for ((key, othernode) in other.nodesDirectory) {
            val node = MyNode(this, othernode)
            nodesDirectory[key] = node
            nodes.add(node)
        }

        for (relationship in other.relationships) {
            val otherstart = relationship.startNode
            val otherend = relationship.endNode

            val start = nodesDirectory.getOrDefault(otherstart.variable, MyNode(this, otherstart))
            nodes.add(start)
            val end = nodesDirectory.getOrDefault(otherend.variable, MyNode(this, otherend))
            nodes.add(end)

            relationships.add(MyRelationship(relationship, start, end))
        }
    }

    val diameter: Int by lazy {
        val graph = SimpleGraph<MyNode, MyRelationship>(MyRelationship::class.java)

        for (node in nodes) {
            graph.addVertex(node)
        }

        for (relationship in relationships) {
            graph.addEdge(relationship.startNode, relationship.endNode, relationship)
        }

        GraphMeasurer(graph).diameter.toInt()
    }

    override fun toString(): String {
        return "Group<$number>(mode=$mode, diameter=$diameter, nodesDirectory=$nodesDirectory, nodes=$nodes, relationships=$relationships)"
    }
}
