package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

class Neo4JWrapper(private val db : GraphDatabaseService) : IDB {
    override fun getRelationships(): Iterable<Relationship> {
        return db.allRelationships
    }

    override fun getNodes(): Iterable<Node> {
        return db.allNodes
    }
}