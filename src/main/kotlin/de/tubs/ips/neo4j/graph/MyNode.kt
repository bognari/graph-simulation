package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.*

class MyNode(private val variable: String = "") : MyEntity(), Node {

    private val labels = HashSet<Label>() // FIXME shared between groups but no sharing is even fine because it is a lesser filter
    private val relationships = HashSet<Relationship>()
    private val relationshipsIncoming = HashSet<Relationship>()
    private val relationshipsOutgoing = HashSet<Relationship>()

    override fun delete() {
        throw UnsupportedOperationException()
    }

    override fun getRelationships(): Iterable<Relationship> {
        return relationships
    }

    override fun hasRelationship(): Boolean {
        return relationships.any()
    }

    private fun hasType(relationship: Relationship, vararg relationshipTypes: RelationshipType): Boolean {
        return relationshipTypes.any { relationship.isType(it) }
    }

    override fun getRelationships(vararg relationshipTypes: RelationshipType): Iterable<Relationship> {
        return relationships.filter { hasType(it, *relationshipTypes) }
    }

    override fun getRelationships(direction: Direction, vararg relationshipTypes: RelationshipType): Iterable<Relationship> {
        return getByDirection(direction).filter { hasType(it, *relationshipTypes) }
    }

    override fun hasRelationship(vararg relationshipTypes: RelationshipType): Boolean {
        return relationships.any { hasType(it, *relationshipTypes) }
    }

    override fun hasRelationship(direction: Direction, vararg relationshipTypes: RelationshipType): Boolean {
        return getByDirection(direction).any { hasType(it, *relationshipTypes) }
    }

    override fun getRelationships(direction: Direction): Iterable<Relationship> {
        return getByDirection(direction)
    }

    override fun hasRelationship(direction: Direction): Boolean {
        return getByDirection(direction).any()
    }

    override fun getRelationships(relationshipType: RelationshipType, direction: Direction): Iterable<Relationship> {
        return getByDirection(direction).filter { it.isType(relationshipType) }
    }

    override fun hasRelationship(relationshipType: RelationshipType, direction: Direction): Boolean {
        return getByDirection(direction).any { it.isType(relationshipType) }
    }

    override fun getSingleRelationship(relationshipType: RelationshipType, direction: Direction): Relationship {
        throw UnsupportedOperationException()
    }

    override fun createRelationshipTo(node: Node, relationshipType: RelationshipType): Relationship {
        throw UnsupportedOperationException()
    }

    fun insertIncomingRelationship(relationship: Relationship) {
        if (relationships.contains(relationship) || relationshipsIncoming.contains(relationship)) {
            throw IllegalArgumentException()
        }
        relationships.add(relationship)
        relationshipsIncoming.add(relationship)
    }

    fun insertOutgoingRelationship(relationship: Relationship) {
        if (relationships.contains(relationship) || relationshipsOutgoing.contains(relationship)) {
            throw IllegalArgumentException()
        }
        relationships.add(relationship)
        relationshipsOutgoing.add(relationship)
    }

    fun insertNullRelationship(relationship: Relationship) {
        if (relationships.contains(relationship) || relationshipsOutgoing.contains(relationship) || relationshipsIncoming.contains(relationship)) {
            throw IllegalArgumentException()
        }
        relationships.add(relationship)
        relationshipsOutgoing.add(relationship)
        relationshipsIncoming.add(relationship)
    }

    override fun getRelationshipTypes(): Iterable<RelationshipType> {
        return relationships.map { it.type }
    }

    override fun getDegree(): Int {
        return relationships.size
    }

    override fun getDegree(relationshipType: RelationshipType): Int {
        return relationships.filter { it.isType(relationshipType) }.size
    }

    override fun getDegree(direction: Direction): Int {
        return getByDirection(direction).size
    }

    override fun getDegree(relationshipType: RelationshipType, direction: Direction): Int {
        return getByDirection(direction).filter { it.isType(relationshipType) }.size
    }

    override fun addLabel(label: Label) {
        labels.add(label)
    }

    override fun removeLabel(label: Label) {
        labels.remove(label)
    }

    override fun hasLabel(label: Label): Boolean {
        return labels.contains(label)
    }

    override fun getLabels(): Iterable<Label> {
        return labels
    }

    private fun getByDirection(direction: Direction): Collection<Relationship> {
        return when (direction) {
            Direction.INCOMING -> relationshipsIncoming
            Direction.OUTGOING -> relationshipsOutgoing
            Direction.BOTH -> relationships
        }
    }

    private fun labelString(): String {
        if (labels.isEmpty()) {
            return ""
        }
        return labels.joinToString(", ")
    }

    private fun variableString(): String {
        if (variable.isEmpty()) {
            return ""
        }
        return "<$variable> "
    }

    override fun toString(): String {
        return "(${variableString()}${labelString()} <$degree|${relationshipsIncoming.size}|${relationshipsOutgoing.size}>)"
    }
}
