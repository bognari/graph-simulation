package de.tubs.ips.neo4j.procedure

import de.tubs.ips.neo4j.parser.Visitor
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.neo4j.driver.internal.value.MapValue
import org.neo4j.driver.v1.Config
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.junit.Neo4jRule

@RunWith(Parameterized::class)
class MAMETest {
    @Parameterized.Parameter
    @JvmField
    var query: String? = null

    companion object {
        @ClassRule
        @JvmField
        var neo4j: Neo4jRule = Neo4jRule().withProcedure(MyProcedure::class.java)

        @Parameterized.Parameters(name = "<{index}> {0}")
        @JvmStatic
        fun params(): Iterable<String> {
            return listOf("MATCH (G:Game)\n" +
                    "RETURN count(G)", "MATCH (C:Clone)\n" +
                    "RETURN count(C)", "MATCH (C:Clone {name:'pacman'})-[:CLONEOF]-(G:Game)\n" +
                    "MATCH (M:Manufacturer)-[:MANUFACTURED]-(G)\n" +
                    "MATCH (Y:Year)-[:PRODUCED]-(G)\n" +
                    "MATCH (S:Source)-[:DRIVER]-(G)\n" +
                    "RETURN C,G,M,Y,S", "MATCH (C:Clone )-[:CLONEOF]-(G:Game {name:'puckman'} )\n" +
                    "RETURN C,G", "MATCH (C:Clone)-[:CLONEOF]-(G:Game)\n" +
                    "RETURN G.name , count(C) AS Clones ORDER BY Clones DESC", "MATCH (S:Source {name:'galaxian.cpp'})-[:DRIVER]-(G:Game)\n" +
                    "RETURN G.description AS Name ORDER BY Name").map { it.replace("\n", " ") }.map { it.replace("\"", "'") }
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withoutEncryption().toConfig())


            val import = listOf("CREATE CONSTRAINT ON (C:Clone) ASSERT C.name IS UNIQUE;",
                    "CREATE CONSTRAINT ON (G:Game) ASSERT G.name IS UNIQUE;",
                    "CREATE CONSTRAINT ON (C:Clone) ASSERT C.name IS UNIQUE;",
                    "CREATE CONSTRAINT ON (Y:Year) ASSERT Y.name IS UNIQUE;",
                    "CREATE CONSTRAINT ON (S:Status) ASSERT S.name IS UNIQUE;",
                    "CREATE CONSTRAINT ON (M:Manufacturer) ASSERT M.name IS UNIQUE;",

                    """LOAD CSV WITH HEADERS FROM "https://raw.githubusercontent.com/csantill/MAMEGraphGist/master/submamelist.csv" AS line

FOREACH(ignoreMe IN CASE WHEN trim(line.cloneof) <> "" THEN [1] ELSE [] END |
 MERGE (C:Clone{name:line.name})
 ON MATCH SET C.description = line.description
 MERGE (So:Source{name:line.sourcefile})
 MERGE (G:Game{name:line.cloneof})
 MERGE (C)-[:CLONEOF]->(G)
 MERGE (So)-[:DRIVER]->(C)
)
FOREACH(ignoreMe IN CASE WHEN trim(line.cloneof) <> "" THEN [] ELSE [1] END |
 MERGE (G:Game{name:line.name})
 ON MATCH SET G.description = line.description
 MERGE (M:Manufacturer{name:line.manufacturer})
 MERGE (So:Source{name:line.sourcefile})
 MERGE (M)-[:MANUFACTURED]->(G)
 MERGE (M)-[:MANUFACTURED]->(G)
 MERGE (Y:Year{name:line.year})
 MERGE (S:Status{name:line.status})
 MERGE (G)-[:PRODUCED]->(Y)
 MERGE (G)-[:STATUS]->(S)
 MERGE (So)-[:DRIVER]->(G)
);""")

            driver.session().use({
                for (i in import) {
                    it.run(i)
                }
            })

        }

        @AfterClass
        @JvmStatic
        fun end() {
            driver.close()
        }

        private lateinit var driver: Driver
    }

    @Test
    fun test() {

        val visitor = Visitor.setupVisitor(query!!)

        GraphDatabase.driver(neo4j.boltURI(), Config.build().withoutEncryption().toConfig()).use({ driver ->
            driver.session().use({ session ->
                val q1 = "CALL myprocedure.dualSimulationID(\"$query\")"
                val q2 = "CALL myprocedure.strongSimulationID(\"$query\")"
                val q3 = "CALL myprocedure.dualSimulationLabel(\"$query\")"
                val q4 = "CALL myprocedure.strongSimulationLabel(\"$query\")"

                val result = session.run(query).asSequence().toList()
                val r1 = session.run(q1).asSequence().map { it["content"] as MapValue }.map { it.asMap() }.toList()
                val r2 = session.run(q2).asSequence().map { it["content"] as MapValue }.map { it.asMap() }.toList()
                val r3 = session.run(q3).asSequence().map { it["content"] as MapValue }.map { it.asMap() }.toList()
                val r4 = session.run(q4).asSequence().map { it["content"] as MapValue }.map { it.asMap() }.toList()

                //testResultsUnordered(result, r1)
                //testResultsUnordered(result, r2)
                //testResultsUnordered(result, r3)
                //testResultsUnordered(result, r4)

                testResults(result, r1)
                testResults(result, r2)
                //testResults(result, r3)
                //testResults(result, r4)

                println()
            })
        })
    }
}