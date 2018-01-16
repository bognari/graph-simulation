package de.tubs.ips.neo4j.graph

import org.jgrapht.alg.shortestpath.GraphMeasurer
import org.jgrapht.graph.SimpleGraph

data class Group(val mode: Mode) {

    enum class Mode {
        MATCH, OPTIONAL
    }

    val nodesDirectory: MutableMap<String, MyNode> = HashMap()
    val nodes: MutableList<MyNode> = ArrayList()
    val relationships: MutableList<MyRelationship> = ArrayList()
    val number: Int

    companion object {
        internal var number = 0
    }

    init {
        number = Group.number++
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
