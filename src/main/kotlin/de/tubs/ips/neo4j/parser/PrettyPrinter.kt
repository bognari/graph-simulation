package de.tubs.ips.neo4j.parser

import de.tubs.ips.neo4j.grammar.CypherBaseVisitor
import de.tubs.ips.neo4j.grammar.CypherParser
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

class PrettyPrinter : CypherBaseVisitor<Unit>() {
    val text = StringBuilder()

    override fun visitTerminal(node: TerminalNode) {
        if (node.symbol.type != Token.EOF) {
            text.append(node.symbol.text)
        }
    }

    override fun visitWhere(ctx: CypherParser.WhereContext) {
        if (ctx.children.size > 1) {
            super.visitWhere(ctx)
        }
    }
}