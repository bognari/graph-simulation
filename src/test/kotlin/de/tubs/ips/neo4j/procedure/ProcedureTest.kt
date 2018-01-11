package de.tubs.ips.neo4j.procedure

import org.junit.Rule
import org.junit.Test
import org.neo4j.driver.v1.Config
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.junit.Neo4jRule

class ProcedureTest {
    // This rule starts a Neo4j instance
    @Rule
    @JvmField
    var neo4j = Neo4jRule().withProcedure(MyProcedure::class.java)

    @Test
    @Throws(Throwable::class)
    fun procedure() {
        // In a try-block, to make sure we close the driver and session after the test
        GraphDatabase.driver(neo4j.boltURI(), Config.build().withoutEncryption().toConfig()).use({ driver ->
            driver.session().use({ session ->
                // Given

                // When
                val result = session.run("RETURN example.concat(['name','surname'], ';') as result")

                // Then
            })
        })
    }
}