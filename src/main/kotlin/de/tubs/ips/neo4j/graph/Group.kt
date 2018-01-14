package de.tubs.ips.neo4j.graph

data class Group(val mode: Mode) {

    enum class Mode {
        MATCH, OPTIONAL
    }

    val nodesDirectory: MutableMap<String, MyNode> = HashMap()
    val nodes: MutableList<MyNode> = ArrayList()
    val relationships: MutableList<MyRelationship> = ArrayList()
    var diameter: Int = -1

    //private val graph: Graph<MyNode, MyRelationship>? = null
    //private val graphMeasurer: GraphMeasurer<MyNode, MyRelationship>? = null

    override fun toString(): String {
        return "Group(mode=$mode, nodesDirectory=$nodesDirectory, nodes=$nodes, relationships=$relationships)"
    }

    
}
