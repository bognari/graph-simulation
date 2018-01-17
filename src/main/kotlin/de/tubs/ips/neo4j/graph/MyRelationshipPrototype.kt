package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.RelationshipType

data class MyRelationshipPrototype(val parsingDirection: Direction, private val typesPrototype: List<String>?, private val prototypeProperties: Map<String, Any>?) : MyEntity() {

    val types = ArrayList<RelationshipType>()

    init {
        if (prototypeProperties != null) {
            setProperty(prototypeProperties)
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