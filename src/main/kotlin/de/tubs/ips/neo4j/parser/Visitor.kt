package de.tubs.ips.neo4j.parser

import de.tubs.ips.neo4j.grammar.CypherBaseVisitor
import de.tubs.ips.neo4j.grammar.CypherLexer
import de.tubs.ips.neo4j.grammar.CypherParser
import de.tubs.ips.neo4j.graph.Group
import de.tubs.ips.neo4j.graph.MyNode
import de.tubs.ips.neo4j.graph.MyRelationship
import de.tubs.ips.neo4j.graph.MyRelationshipPrototype
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.TerminalNode
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Entity
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node

class Visitor : CypherBaseVisitor<Any>() {

    companion object {
        fun setupVisitor(pattern: String): Visitor {
            val lexer = CypherLexer(CharStreams.fromString(pattern, "user input"))
            val commonTokenStream = CommonTokenStream(lexer)
            val parser = CypherParser(commonTokenStream)
            val context = parser.cypher()
            val visitor = Visitor()
            visitor.visit(context)

            return visitor
        }
    }

    enum class DetailConstants {
        TYPES, VARIABLE, PROPERTIES, RANGES
    }

    val variables: MutableMap<String, Variable> = HashMap()
    val groups: MutableList<Group> = ArrayList()
    private lateinit var group: Group
    private val normalMatch: Group = Group(Group.Mode.MATCH)

    @Suppress("UNCHECKED_CAST")
    override fun visitNodePattern(ctx: CypherParser.NodePatternContext): Node {
        var variable: String? = null
        var labels: List<String>? = null
        var properties: Map<String, Any>? = null

        for (child in ctx.children) {
            when (child) {
                is CypherParser.VariableContext -> variable = visitVariable(child)
                is CypherParser.NodeLabelsContext -> labels = visitNodeLabels(child)
                is CypherParser.PropertiesContext -> properties = visitProperties(child) as Map<String, Any>
            }
        }

        val node = if (variable != null) {
            group.nodesDirectory.getOrPut(variable, { MyNode(variable) })
        } else {
            MyNode()
        }

        group.nodes.add(node)

        if (labels != null) {
            for (label in labels) {
                node.addLabel(Label.label(label))
            }
        }

        if (variable != null) {
            group.nodesDirectory.put(variable, node)
        }

        if (properties != null) {
            node.setProperty(properties)
        }

        return node
    }

    override fun visitNodeLabels(ctx: CypherParser.NodeLabelsContext): List<String> {
        val labels = ArrayList<String>()

        for (child in ctx.children) {
            when (child) {
                is CypherParser.NodeLabelContext -> labels.add(visitNodeLabel(child) as String)
            }
        }
        return labels
    }

    override fun visitMapLiteral(ctx: CypherParser.MapLiteralContext): Map<String, Any> {
        val list = ArrayList<Any?>()

        for (child in ctx.children) {
            when (child) {
                is CypherParser.PropertyKeyNameContext -> list.add(visitPropertyKeyName(child))
                is CypherParser.ExpressionContext -> list.add(visitExpression(child))
            }
        }

        val ret = HashMap<String, Any>()

        var i = 0
        while (i < list.size) {
            if (list[i] != null && list[i + 1] != null) {
                ret.put(list[i] as String, list[i + 1] as Any)
                i += 2
            }
        }

        return ret
    }

    override fun visitLiteral(ctx: CypherParser.LiteralContext): Any? {
        val child = ctx.getChild(0)
        return when (child) {
            is TerminalNode -> child.text.substring(1, child.text.length - 1)
            is CypherParser.BooleanLiteralContext -> "true".equals(child.text, true)
            is CypherParser.NumberLiteralContext -> visitNumberLiteral(child)
            else -> {
                println("warning: unsupported properties type ${child::class.java.simpleName}")
            }
        }
    }

    override fun visitDoubleLiteral(ctx: CypherParser.DoubleLiteralContext): Double {
        return ctx.getChild(0).text.toDouble()
    }

    override fun visitIntegerLiteral(ctx: CypherParser.IntegerLiteralContext): Int {
        val value = ctx.text

        return when {
            value.startsWith("0x") -> value.substring(2).toInt(16)
            value.startsWith("0b") -> value.substring(2).toInt(2)
            value.startsWith("0") -> value.substring(1).toInt(8)
            else -> value.toInt()
        }
    }

    override fun visitMatch(ctx: CypherParser.MatchContext): Any? {
        var optional = false
        lateinit var pattern: CypherParser.PatternContext
        for (child in ctx.children) {
            when (child) {
                is TerminalNode ->
                    if ("Optional".equals(child.text, true)) optional = true
                is CypherParser.PatternContext -> pattern = child
            }
        }

        group = if (optional) {
            Group(Group.Mode.OPTIONAL)
        } else {
            normalMatch
        }

        if (!groups.contains(group)) {
            groups.add(group)
        }

        visitPattern(pattern)

        return null
    }

    override fun visitParameter(ctx: CypherParser.ParameterContext): Variable {
        return variables.getOrPut(ctx.text, { Variable() })
    }

    override fun visitVariable(ctx: CypherParser.VariableContext): String {
        return ctx.text
    }

    override fun visitLabelName(ctx: CypherParser.LabelNameContext): String {
        return ctx.text
    }

    override fun visitRelTypeName(ctx: CypherParser.RelTypeNameContext): String {
        return ctx.text
    }

    override fun visitPropertyKeyName(ctx: CypherParser.PropertyKeyNameContext): String {
        return ctx.text
    }

    override fun visitPatternElement(ctx: CypherParser.PatternElementContext): Any? {
        val list = ArrayList<Entity>()

        for (child in ctx.children) {
            when (child) {
                is CypherParser.NodePatternContext -> list.add(visitNodePattern(child))
                is CypherParser.PatternElementChainContext -> list.addAll(visitPatternElementChain(child))
            }
        }

        for (i in list.indices) {
            when {
                list[i] is MyRelationshipPrototype -> {
                    val prototype = list[i] as MyRelationshipPrototype

                    val start = list[i - 1] as MyNode
                    val end = list[i + 1] as MyNode

                    when (prototype.parsingDirection) {
                        Direction.OUTGOING -> {
                            group.relationships.add(MyRelationship(prototype, start, end, Direction.OUTGOING))
                        }
                        Direction.INCOMING -> {
                            group.relationships.add(MyRelationship(prototype, start, end, Direction.INCOMING))
                        }
                        Direction.BOTH -> {
                            group.relationships.add(MyRelationship(prototype, start, end, Direction.BOTH))
                            group.relationships.add(MyRelationship(prototype, end, start, Direction.BOTH))
                        }
                    }
                }
            }
        }

        return null
    }

    override fun visitPatternElementChain(ctx: CypherParser.PatternElementChainContext): List<Entity> {
        val list = ArrayList<Entity>()

        for (child in ctx.children) {
            when (child) {
                is CypherParser.RelationshipPatternContext -> list.add(visitRelationshipPattern(child))
                is CypherParser.NodePatternContext -> list.add(visitNodePattern(child))
            }
        }

        return list
    }

    @Suppress("UNCHECKED_CAST")
    override fun visitRelationshipPattern(ctx: CypherParser.RelationshipPatternContext): MyRelationshipPrototype {
        var rightArrow = false
        var leftArrow = false
        var details: Map<DetailConstants, Any>? = null
        var types: List<String>? = null
        var properties: Map<String, Any>? = null

        for (child in ctx.children) {
            when (child) {
                is CypherParser.LeftArrowHeadContext -> leftArrow = true
                is CypherParser.RightArrowHeadContext -> rightArrow = true
                is CypherParser.RelationshipDetailContext -> details = visitRelationshipDetail(child)
            }
        }

        if (details != null) {
            types = details[DetailConstants.TYPES] as List<String>?
            properties = details[DetailConstants.PROPERTIES] as Map<String, Any>?
        }

        val direction = when {
            rightArrow && !leftArrow -> Direction.OUTGOING
            leftArrow && !rightArrow -> Direction.INCOMING
            else -> Direction.BOTH
        }

        return MyRelationshipPrototype(direction, types, properties)
    }

    override fun visitRelationshipDetail(ctx: CypherParser.RelationshipDetailContext): HashMap<DetailConstants, Any> {
        val values = HashMap<DetailConstants, Any>()

        for (child in ctx.children) {
            when (child) {
                is CypherParser.VariableContext -> values.put(DetailConstants.VARIABLE, visitVariable(child))
                is CypherParser.RelationshipTypesContext -> values.put(DetailConstants.TYPES, visitRelationshipTypes(child))
                is CypherParser.RangeLiteralContext -> values.put(DetailConstants.RANGES, visitRangeLiteral(child))
                is CypherParser.PropertiesContext -> values.put(DetailConstants.PROPERTIES, visitProperties(child))
            }
        }

        return values
    }

    override fun visitRelationshipTypes(ctx: CypherParser.RelationshipTypesContext): List<String> {
        val types = ArrayList<String>()

        for (child in ctx.children) {
            when (child) {
                is CypherParser.RelTypeNameContext -> types.add(visitRelTypeName(child))
            }
        }

        return types
    }

    inner class Variable {
        var content: Any? = null
    }
}
