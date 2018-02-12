package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.ResourceIterable

class Neo4JWrapper(private val db : GraphDatabaseService) : IDB {
    override fun getNodes(): ResourceIterable<Node> {
        return db.allNodes
    }
}