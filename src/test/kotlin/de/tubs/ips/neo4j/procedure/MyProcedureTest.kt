package de.tubs.ips.neo4j.procedure

import org.junit.Rule
import org.junit.Test
import org.neo4j.driver.v1.Config
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.junit.Neo4jRule
import java.nio.file.Files
import java.nio.file.Paths


class MyProcedureTest {
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

                val lines = Files.readAllLines(Paths.get("src", "test", "resources", "himym2.cql"))
                val content = lines.joinToString("\n")
                val statements = content.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                for (statement in statements) {
                    session.run(statement)
                }

                val o = session.run("""MATCH (c:Character {name: 'Ranjit'})-[:APPEARED_IN_EPISODE]->(e)
                        RETURN c.name, e.season, e.number, e.title, id(e) ORDER BY e.season, e.number""")

                while (o.hasNext()) {
                    val record = o.next()

                    val map = record.asMap()
                    println(map)
                }

                println(session.run("Match (a) RETURN count(a)").next())


                println(session.run("Match (a:Character) RETURN count(a)").next())

                // When
                val result = session.run("""CALL myprocedure.dualSimulation('
                    MATCH (c:Character {name: \'Ranjit\'})-[:APPEARED_IN_EPISODE]->(e)
                        RETURN c.name, e.season, e.number, e.title, id(e) ORDER BY e.season, e.number
                        ', 'test')""")

                // Then
            })
        })
    }
}