package de.tubs.ips.neo4j.simulation

import org.neo4j.graphdb.Node

interface IDB {
    fun getNodes() : Iterable<Node>
}