package de.tubs.ips.neo4j.graph

data class Group(val mode: Mode) {

    enum class Mode {
        MATCH, OPTIONAL
    }

    val nodesDirectory: MutableMap<String, MyNode> = HashMap()
    val relationshipsDirectory: MutableMap<String, MyRelationshipPrototype> = HashMap()
    val nodes: MutableList<MyNode> = ArrayList()
    val relationships: MutableList<MyRelationship> = ArrayList()

    override fun toString(): String {
        return "Group(mode=$mode, nodesDirectory=$nodesDirectory, relationshipsDirectory=$relationshipsDirectory, nodes=$nodes, relationships=$relationships)"
    }

}
