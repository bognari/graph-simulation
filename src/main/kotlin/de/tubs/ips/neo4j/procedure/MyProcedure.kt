package de.tubs.ips.neo4j.procedure

import de.tubs.ips.neo4j.graph.Group
import de.tubs.ips.neo4j.graph.MyNode
import de.tubs.ips.neo4j.parser.Visitor
import de.tubs.ips.neo4j.simulation.StrongSimulation
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.logging.Log
import org.neo4j.procedure.*
import java.util.stream.Stream


class MyProcedure {
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    @JvmField
    var db: GraphDatabaseService? = null
    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    @JvmField
    var log: Log? = null

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    @JvmField
    var terminationGuard: TerminationGuard? = null


    @Procedure(value = "myprocedure.dualSimulation", mode = Mode.WRITE)
    @Description("")
    fun simulation(@Name("pattern") pattern: String,
                   @Name("sim") sim: String
    ): Stream<Output>? {
        val trimmedPattern = pattern.trim()

        log!!.info("start with pattern:\"$trimmedPattern\" and sim:\"$sim\"")

        val visitor = Visitor.setupVisitor(trimmedPattern)
        val simulation = StrongSimulation(visitor, db!!)
        val result = simulation.dualSimulation()

        writeLabels(result)

        val query = rewriteQuery(result, visitor)
        val ret = db!!.execute(query)

        val retList = ret.asSequence().toList()

        removeLabels(result)

        /*val ret_1 = ret.asSequence().toList()

        val ret_0 = db!!.execute(pattern).asSequence().toList()
        
        val ret_a = db!!.execute("match (a:g0_a) return a").asSequence().toList()
        val ret_b = db!!.execute("match (b:g0_b) return b").asSequence().toList()
        val ret_c = db!!.execute("match (c:g0_c) return c").asSequence().toList()


        val labels = db!!.allLabels

        val listLabels = ArrayList<Label>()
        listLabels.addAll(labels)



        val statistics = ret.queryStatistics

        val colums = ret.columns()
        */


        return retList.stream().map { Output(it) }
    }

    private fun writeLabels(result: Map<Group, Map<MyNode, Set<Node>>>) {
        for ((group, map) in result) {
            for ((node, list) in map) {
                if (node.variable.isNotBlank()) {
                    val label = genLabelString(group, node)
                    for (possibleNode in list) {
                        possibleNode.addLabel(Label.label(label))
                    }
                }
            }
        }
    }


    private fun removeLabels(result: Map<Group, Map<MyNode, Set<Node>>>) {
        for ((group, map) in result) {
            for ((node, list) in map) {
                if (node.variable.isNotBlank()) {
                    val label = genLabelString(group, node)
                    for (possibleNode in list) {
                        possibleNode.removeLabel(Label.label(label))
                    }
                }
            }
        }
    }

    private fun rewriteQuery(result: Map<Group, Map<MyNode, Set<Node>>>, visitor: Visitor): String {
        for ((group, map) in result) {
            for ((node, _) in map) {
                if (node.variable.isNotBlank()) {
                    val label = genLabelString(group, node)
                    node.writeCTXLabel(":$label", visitor)
                }
            }
        }

        return visitor.prettyPrint()
    }

    private fun genLabelString(group: Group, node: MyNode): String {
        return "g${group.number}_${node.variable}"
    }

    class Output(@JvmField
                 public var content: Map<String, Any>) {}
}