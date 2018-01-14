package de.tubs.ips.neo4j.procedure

import de.tubs.ips.neo4j.parser.Visitor
import de.tubs.ips.neo4j.simulation.StrongSimulation
import org.neo4j.graphdb.GraphDatabaseService
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
        log!!.info("start with pattern:\"$pattern\" and sim:\"$sim\"")

        println()

        val visitor = Visitor.setupVisitor(pattern)
        val simulation = StrongSimulation(visitor, db!!)
        val result = simulation.dualSimulation()
        

        return null
    }

    class Output() {
        @JvmField
        public var content: Any? = null
    }
}