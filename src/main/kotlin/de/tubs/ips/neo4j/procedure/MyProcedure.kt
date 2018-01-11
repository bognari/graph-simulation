package de.tubs.ips.neo4j.procedure

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.logging.Log
import org.neo4j.procedure.*



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
    
    @Procedure(value = "myprocedure.simulation", mode = Mode.WRITE)
    @Description("")
    fun simulation(@Name("pattern") pattern: String,
                @Name("simulation") simulation: String
        ) {
        log!!.info("start with pattern:\"$pattern\" and simulation:\"$simulation\"")

    }
}