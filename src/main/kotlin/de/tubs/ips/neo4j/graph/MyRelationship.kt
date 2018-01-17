package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.*

/**
 * jede relation innerhalb der Db hat eine richtung, OUTGOING oder INCOMING
 * -- und <--> haben die gleiche bedeutung...
 */

class MyRelationship(private val prototype: MyRelationshipPrototype, private val start: MyNode, private val end: MyNode, val direction: Direction) : Entity by prototype, Relationship {

    constructor(other: MyRelationship, start: MyNode, end: MyNode) : this(other.prototype, start, end, other.direction)

    init {
        start.addRelationship(this)
        end.addRelationship(this)
    }

    override fun delete() {
        throw UnsupportedOperationException()
    }

    override fun getStartNode(): MyNode {
        return start
    }

    override fun getEndNode(): MyNode {
        return end
    }

    override fun getOtherNode(node: Node): MyNode? {
        return when {
            node === start -> end
            node === end -> start
            else -> null
        }
    }

    override fun getNodes(): Array<Node> {
        return arrayOf(start, end)
    }

    override fun getType(): RelationshipType? {
        throw UnsupportedOperationException()
    }

    override fun isType(relationshipType: RelationshipType): Boolean {
        return prototype.types.contains(relationshipType)
    }

    fun typesString() : String {
        return prototype.types.joinToString("|", prefix = "[", postfix = "]", transform = { it.name() })
    }

    override fun toString(): String {
        return when (direction) {
            Direction.INCOMING -> "$start <-${typesString()}- $end <${prototype.parsingDirection}>"
            Direction.OUTGOING -> "$start -${typesString()}-> $end <${prototype.parsingDirection}>"
            Direction.BOTH -> "$start <-${typesString()}-> $end <${prototype.parsingDirection}>"
            else -> "$start -${typesString()}- $end <${prototype.parsingDirection}>"
        }
    }

    fun match(other: Relationship): Boolean {
        return prototype.matchTypes(other) && prototype.matchProperties(other)
    }
}
