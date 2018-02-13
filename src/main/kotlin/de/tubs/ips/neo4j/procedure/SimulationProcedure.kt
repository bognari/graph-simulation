package de.tubs.ips.neo4j.procedure

import de.tubs.ips.neo4j.graph.Group
import de.tubs.ips.neo4j.graph.MyNode
import de.tubs.ips.neo4j.parser.Visitor
import de.tubs.ips.neo4j.simulation.Simulation
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.logging.Log
import org.neo4j.procedure.*
import java.util.stream.Stream


class SimulationProcedure {
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

    private fun getVisitor(pattern: String): Visitor {
        return Visitor.setupVisitor(pattern.trim())
    }

    private fun getResult(query: String, copy : Boolean = true): Stream<Output> {
        val ret = db!!.execute(query)
        return if (copy) {
            val retList = ret.asSequence().mapTo(ArrayList(), { Output(it) })
            ret.close()
            retList.stream()
        } else {
            val retList = ret.map({ Output(it) })
            retList.stream()
        }
    }

    @Procedure(value = "simulation.dualID", mode = Mode.READ)
    @Description("")
    fun dualSimulationID(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        log!!.info("simulation.dualID $mode, ${pattern.trim()}")
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).dualSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        log!!.info(query)
        return getResult(query, false)
    }

    @Procedure(value = "simulation.strongID", mode = Mode.READ)
    @Description("")
    fun strongSimulationID(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        log!!.info("simulation.strongID $mode, ${pattern.trim()}")
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).strongSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        log!!.info(query)
        return getResult(query, false)
    }

    @Procedure(value = "simulation.dualIDString", mode = Mode.READ)
    @Description("")
    fun dualSimulationIDString(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        log!!.info("simulation.dualIDString $mode, ${pattern.trim()}")
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).dualSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        log!!.info(query)
        return Stream.of(Output(mapOf(Pair("query", query))))
    }

    @Procedure(value = "simulation.strongIDString", mode = Mode.READ)
    @Description("")
    fun strongSimulationIDString(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        log!!.info("simulation.strongIDString $mode, ${pattern.trim()}")
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).strongSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        log!!.info(query)
        return Stream.of(Output(mapOf(Pair("query", query))))
    }

    @Procedure(value = "simulation.dualLabel", mode = Mode.WRITE)
    @Description("")
    fun dualSimulationLabel(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        log!!.info("simulation.dualLabel $mode, ${pattern.trim()}")
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).dualSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryLabel(result, visitor)
        writeLabels(result)
        val ret = getResult(query)
        removeLabels(result)
        log!!.info(query)
        return ret
    }

    @Procedure(value = "simulation.strongLabel", mode = Mode.WRITE)
    @Description("")
    fun strongSimulationLabel(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        log!!.info("simulation.strongLabel $mode, ${pattern.trim()}")
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).strongSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryLabel(result, visitor)
        writeLabels(result)
        val ret = getResult(query)
        removeLabels(result)
        log!!.info(query)
        return ret
    }

    private fun writeLabels(result: Map<Group, Map<MyNode, Set<Node>>>) {
        for ((group, map) in result) {
            for ((node, list) in map) {
                val label = genLabelString(group, node)
                for (possibleNode in list) {
                    possibleNode.addLabel(Label.label(label))
                }
            }
        }
    }

    private fun removeLabels(result: Map<Group, Map<MyNode, Set<Node>>>) {
        for ((group, map) in result) {
            for ((node, list) in map) {
                val label = genLabelString(group, node)
                for (possibleNode in list) {
                    possibleNode.removeLabel(Label.label(label))
                }
            }
        }
    }

    private fun rewriteQueryID(result: Map<Group, Map<MyNode, Set<Node>>>, visitor: Visitor): String {
        for ((_, map) in result) {
            for ((node, possible) in map) {
                node.writeCTXWhere(possible, visitor)
            }
        }
        return visitor.prettyPrint()
    }


    private fun rewriteQueryLabel(result: Map<Group, Map<MyNode, Set<Node>>>, visitor: Visitor): String {
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

    class Output(@JvmField var content: Map<String, Any>)
}