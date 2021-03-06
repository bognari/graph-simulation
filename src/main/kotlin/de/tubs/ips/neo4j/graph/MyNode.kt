package de.tubs.ips.neo4j.graph

import de.tubs.ips.neo4j.grammar.CypherParser
import de.tubs.ips.neo4j.parser.Visitor
import org.neo4j.graphdb.*

class MyNode(private val group: Group, val variable: String = "") : MyEntity(), Node {

    private val labels = HashSet<Label>() // FIXME shared between groups but no sharing is even fine because it is a lesser filter
    private val relationships = HashSet<Relationship>() // FIXME jede relation darf nur einmal im Patternpfad vorkommen
    private val relationshipsIncoming = HashSet<Relationship>()
    private val relationshipsOutgoing = HashSet<Relationship>()
    val labelCtxs = HashSet<CypherParser.NodeLabelsContext>()
    lateinit var whereCtx: CypherParser.WhereContext

    constructor(group: Group, other: MyNode) : this(group, other.variable) {
        properties.putAll(other.properties)
        labels.addAll(other.labels)
    }

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

    fun addRelationship(relationship: MyRelationship) {
        if (relationships.contains(relationship)) {
            //throw IllegalArgumentException()
            println("warning: loop at $this with $relationship")
            return
        }

        relationships.add(relationship)

        when (relationship.direction) {
            Direction.OUTGOING -> if (relationship.startNode === this) {
                relationshipsOutgoing.add(relationship)
            } else {
                relationshipsIncoming.add(relationship)
            }
            Direction.INCOMING -> if (relationship.startNode === this) {
                relationshipsIncoming.add(relationship)
            } else {
                relationshipsOutgoing.add(relationship)
            }
            Direction.BOTH -> {
                relationshipsIncoming.add(relationship)
                relationshipsOutgoing.add(relationship)
            }
        }
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

    fun hasLabels(): Boolean {
        return labels.any()
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
        return labels.joinToString(", ", prefix = "[", postfix = "]")
    }

    private fun variableString(): String {
        if (variable.isEmpty()) {
            return ""
        }
        return "$variable "
    }

    override fun toString(): String {
        return "(${variableString()}${labelString()} <$degree|${relationshipsIncoming.size}|${relationshipsOutgoing.size}>)"
    }

    private fun matchLabels(other: Node): Boolean {
        return labels.all { other.hasLabel(it) }
    }

    /**
     * labels is an AND filter
     */
    fun match(other: Node): Boolean {
        return matchLabels(other) && matchProperties(other)
    }

    fun writeCTXLabel(label: String, visitor: Visitor) {
        for (ctx in labelCtxs) {
            ctx.addChild(Visitor.PseudoToken(label, visitor.lexer))
        }
    }

    fun writeCTXWhere(possible: Collection<Node>, visitor: Visitor) {
        val string = "id($variable) IN ${possible.joinToString(prefix = "[", postfix = "]", transform = { it.id.toString() }, separator = ",")}"
        if (group.inGroup.contains(this)) {
            if (whereCtx.children.size == 1) {
                whereCtx.addChild(Visitor.PseudoToken(string, visitor.lexer))
            } else {
                whereCtx.addChild(Visitor.PseudoToken(" AND $string", visitor.lexer))
            }
        }
    }
}
