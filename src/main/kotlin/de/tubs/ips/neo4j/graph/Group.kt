package de.tubs.ips.neo4j.graph

import de.tubs.ips.neo4j.grammar.CypherParser
import org.jgrapht.alg.shortestpath.GraphMeasurer
import org.jgrapht.graph.SimpleGraph

class Group {

    private val mode : String
    val nodesDirectory: MutableMap<String, MyNode> = LinkedHashMap()
    val nodes: MutableSet<MyNode> = LinkedHashSet()
    val relationships: MutableSet<MyRelationship> = LinkedHashSet()
    val number: Int
    val inGroup = HashSet<MyNode>()

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
    constructor(other: Group, whereContext: CypherParser.WhereContext) {
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

        for (node in nodes) {
            node.whereCtx = whereContext
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
