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
        return prototype.type
    }

    override fun isType(relationshipType: RelationshipType): Boolean {
        return relationshipType == prototype.type
    }

    override fun toString(): String {
        return when (direction) {
            Direction.INCOMING -> "$start <-[${prototype.type}]- $end <${prototype.parsingDirection}>"
            Direction.OUTGOING -> "$start -[${prototype.type}]-> $end <${prototype.parsingDirection}>"
            Direction.BOTH -> "$start <-[${prototype.type}]-> $end <${prototype.parsingDirection}>"
            else -> "$start -[${prototype.type}]- $end <${prototype.parsingDirection}>"
        }
    }
}
