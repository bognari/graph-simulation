package de.tubs.ips.neo4j.graph

import org.neo4j.graphdb.*

class MyRelationship(private val prototype: MyRelationshipPrototype, private val start: MyNode, private val end: MyNode, private val direction: Direction?) : Entity by prototype, Relationship {

    init {
        prototype.relationships.add(this)

        when (direction) {
            Direction.OUTGOING -> {
                start.insertOutgoingRelationship(this)
                end.insertIncomingRelationship(this)
            }
            Direction.INCOMING -> {
                start.insertIncomingRelationship(this)
                end.insertOutgoingRelationship(this)
            }
            else -> {
                start.insertNullRelationship(this)
                end.insertNullRelationship(this)
            }
        }
    }

    fun isOrRelationship(): Boolean {
        return direction == null
    }

    fun getOtherRelationships(): List<MyRelationship> {
        return prototype.relationships
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

    override fun getType(): RelationshipType {
        throw UnsupportedOperationException()
    }

    override fun isType(relationshipType: RelationshipType?): Boolean {
        return prototype.hasType(relationshipType)
    }

    override fun toString(): String {
        return when (direction) {
            Direction.INCOMING -> "$start <-${typesString()}- $end <${prototype.parsingDirection}>"
            Direction.OUTGOING -> "$start -${typesString()}-> $end <${prototype.parsingDirection}>"
            Direction.BOTH -> "$start <-${typesString()}-> $end <${prototype.parsingDirection}>"
            else -> "$start -${typesString()}- $end <${prototype.parsingDirection}>"
        }
    }

    private fun variableString(): String {
        if (prototype.variable.isEmpty()) {
            return ""
        }
        return "<${prototype.variable}> "
    }

    private fun typesString(): String {
        return "[${variableString()}${prototype.types.joinToString(", ")}]"
    }
}
