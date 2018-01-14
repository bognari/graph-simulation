package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

interface IDB {
    fun getNodes() : Iterable<Node>
    fun getRelationships() : Iterable<Relationship>
}