package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.Entity
import org.neo4j.graphdb.GraphDatabaseService

abstract class MyEntity : Entity {
    private var properties: MutableList<Map<String, Any>> = ArrayList()

    override fun getId(): Long {
        throw UnsupportedOperationException()
    }

    override fun getGraphDatabase(): GraphDatabaseService {
        throw UnsupportedOperationException()
    }

    override fun hasProperty(s: String): Boolean {
        return properties.any { it.containsKey(s) }
    }

    fun setProperty(map: Map<String, Any>) {
        properties.add(map)
    }

    override fun getProperty(s: String): Any? {
        return properties
                .firstOrNull { it.containsKey(s) }
                ?.let { it[s] }
    }

    override fun getProperty(s: String, o: Any?): Any? {
        return properties
                .firstOrNull { it.containsKey(s) }
                ?.let { it[s] }
                ?: o
    }

    override fun setProperty(s: String, o: Any) {
        throw UnsupportedOperationException()
    }

    override fun removeProperty(s: String): Any {
        throw UnsupportedOperationException()
    }

    override fun getPropertyKeys(): Iterable<String> {
        val keys = ArrayList<String>()
        for (map in properties) {
            keys.addAll(map.keys)
        }

        return keys
    }

    override fun getProperties(vararg strings: String): Map<String, Any> {
        throw UnsupportedOperationException()
    }

    override fun getAllProperties(): Map<String, Any> {
        throw UnsupportedOperationException()
    }
}
