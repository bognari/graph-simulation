package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.Entity
import org.neo4j.graphdb.GraphDatabaseService

abstract class MyEntity() : Entity {
    protected val properties: MutableMap<String, Any> = HashMap()

    override fun getId(): Long {
        throw UnsupportedOperationException()
    }

    override fun getGraphDatabase(): GraphDatabaseService {
        throw UnsupportedOperationException()
    }

    override fun hasProperty(s: String): Boolean {
        return properties.containsKey(s)
    }

    fun setProperty(map: Map<String, Any>) {
        properties.putAll(map)
    }

    override fun getProperty(s: String): Any? {
        return properties[s]
    }

    override fun getProperty(s: String, o: Any?): Any? {
        return properties.getOrDefault(s, o)
    }

    override fun setProperty(s: String, o: Any) {
        properties[s] = o
    }

    override fun removeProperty(s: String): Any {
        throw UnsupportedOperationException()
    }

    override fun getPropertyKeys(): Iterable<String> {
        return properties.keys
    }

    override fun getProperties(vararg strings: String): Map<String, Any> {
        return properties.filterKeys { strings.contains(it) }
    }

    override fun getAllProperties(): Map<String, Any> {
        return properties
    }

    fun matchProperties(other : Entity) : Boolean {
        for ((key, value) in properties) {
            if (!other.hasProperty(key) || value != other.getProperty(key)) {
                return false
            }
        }

        return true
    }
}
