package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.RelationshipType

data class MyRelationshipPrototype(val parsingDirection: Direction, private val typesPrototype: List<String>?, private val properties: Map<String, Any>?) : MyEntity() {

    internal val relationships = ArrayList<MyRelationship>()
    val types = ArrayList<RelationshipType>()

    init {
        if (properties != null) {
            setProperty(properties)
        }
        typesPrototype?.mapTo(types) { RelationshipType.withName(it) }
    }

    /**
     * types is an OR filter
     */
    fun matchTypes(other : Relationship) : Boolean {
        return types.isEmpty() || types.any { other.isType(it) }
    }
}