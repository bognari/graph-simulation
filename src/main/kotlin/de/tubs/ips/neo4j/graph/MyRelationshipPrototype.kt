package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.RelationshipType

data class MyRelationshipPrototype(val parsingDirection: Direction?, internal val variable: String = "") : MyEntity() {
    internal val types = ArrayList<RelationshipType>()
    internal val relationships = ArrayList<MyRelationship>()

    fun addType(type: RelationshipType) {
        types.add(type)
    }

    fun hasType(relationshipType: RelationshipType?): Boolean {
        return types.contains(relationshipType)
    }
}