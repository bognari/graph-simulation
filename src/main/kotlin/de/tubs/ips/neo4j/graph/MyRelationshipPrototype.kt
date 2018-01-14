package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.RelationshipType

data class MyRelationshipPrototype(val parsingDirection: Direction?, private val typePrototype: String?, private val properties: Map<String, Any>?) : MyEntity() {

    internal val relationships = ArrayList<MyRelationship>()
    internal val type: RelationshipType?

    init {
        if (properties != null) {
            setProperty(properties)
        }
        type = if (typePrototype == null) {
            null
        } else {
            RelationshipType.withName(typePrototype)
        }
    }
}