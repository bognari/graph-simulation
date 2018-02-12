package de.tubs.ips.neo4j.procedure

import de.tubs.ips.neo4j.graph.Group
import de.tubs.ips.neo4j.graph.MyNode
import de.tubs.ips.neo4j.parser.Visitor
import de.tubs.ips.neo4j.simulation.Simulation
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.procedure.*
import java.util.stream.Stream


class SimulationProcedure {
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    @JvmField
    var db: GraphDatabaseService? = null

    private fun getVisitor(pattern: String): Visitor {
        return Visitor.setupVisitor(pattern.trim())
    }

    private fun getResult(query: String): Stream<Output> {
        val ret = db!!.execute(query)
        val retList = ret.asSequence().mapTo(ArrayList(), { Output(it) })
        //ret.close()
        return retList.stream()
    }

    @Procedure(value = "simulation.dualID", mode = Mode.READ)
    @Description("")
    fun dualSimulationID(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).dualSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        return getResult(query)
    }

    @Procedure(value = "simulation.strongID", mode = Mode.READ)
    @Description("")
    fun strongSimulationID(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).strongSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        return getResult(query)
    }

    @Procedure(value = "simulation.dualIDString", mode = Mode.READ)
    @Description("")
    fun dualSimulationIDString(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).dualSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        return Stream.of(Output(mapOf(Pair("query", query))))
    }

    @Procedure(value = "simulation.strongIDString", mode = Mode.READ)
    @Description("")
    fun strongSimulationIDString(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).strongSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryID(result, visitor)
        return Stream.of(Output(mapOf(Pair("query", query))))
    }

    @Procedure(value = "simulation.dualLabel", mode = Mode.WRITE)
    @Description("")
    fun dualSimulationLabel(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).dualSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryLabel(result, visitor)
        writeLabels(result)
        val ret = getResult(query)
        removeLabels(result)
        return ret
    }

    @Procedure(value = "simulation.strongLabel", mode = Mode.WRITE)
    @Description("")
    fun strongSimulationLabel(@Name("pattern") pattern: String, @Name("mode", defaultValue = "NORMAL") mode: String
    ): Stream<Output> {
        val visitor = getVisitor(pattern)
        val result = Simulation(visitor, db!!).strongSimulation(Simulation.Mode.valueOf(mode.toUpperCase()))
        val query = rewriteQueryLabel(result, visitor)
        writeLabels(result)
        val ret = getResult(query)
        removeLabels(result)
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