package de.tubs.ips.neo4j.procedure

import de.tubs.ips.neo4j.parser.Visitor
import org.junit.Rule
import org.junit.Test
import org.neo4j.driver.v1.Config
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.junit.Neo4jRule


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

                /*val lines = Files.readAllLines(Paths.get("src", "test", "resources", "himym2.cql"))
                val content = lines.joinToString("\n")
                val statements = content.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

                for (statement in statements) {
                    session.run(statement)
                }

                val list = session.run("""match (a)-->(b:Character) Match (b)--(c) OPTIONAL MATCH (a:bla {name:'foobar'}) return a, b, c""").asSequence().toList()

                println(session.run("Match (a) RETURN count(a)").next())


                println(session.run("Match (a:Character) RETURN count(a)").next())
                */
                // When
                val result = session.run("""CALL myprocedure.dualSimulationID("
                    MATCH (x:FullProfessor)-[:worksFor]-({id: 'http://www.Department0.University12.edu'}) OPTIONAL MATCH (y)-[:advisor]-(x), (x)-[:teacherOf]-(x), (y)-[:takesCourse]-(z) RETURN x, y, z;

                        ")""")

                val rList = result.asSequence().toList()

                println()

                // Then
            })
        })
    }

    @Test
    @Throws(Throwable::class)
    fun ast() {
        val v = Visitor.setupVisitor("""
                    MATCH (x:FullProfessor)-[:worksFor]-({id: 'http://www.Department0.University12.edu'}) OPTIONAL MATCH (y)-[:advisor]-(x), (x)-[:teacherOf]-(x), (y)-[:takesCourse]-(z) RETURN x, y, z;
""")
        println()
    }
}