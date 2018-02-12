package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.*

class Neo4JWrapper(private val db : GraphDatabaseService) : IDB {
    override fun getNodes(): ResourceIterable<Node> {
        return db.allNodes
    }

    fun getNodes(label: Label) : ResourceIterator<Node> {
        return db.findNodes(label)
    }
}