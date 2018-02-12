package de.tubs.ips.neo4j.procedure

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.neo4j.driver.v1.Config
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.GraphDatabase
import org.neo4j.harness.junit.Neo4jRule
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class MAMETest {
    @Parameterized.Parameter
    @JvmField
    var query: String? = null

    companion object {
        @ClassRule
        @JvmField
        var neo4j: Neo4jRule = Neo4jRule().withProcedure(SimulationProcedure::class.java)

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

        @JvmStatic
        private lateinit var driver: Driver
    }

    @Test
    fun normal() {
        driver.session().use({ session ->
            val result = session.run(query).summary()
            print("normal, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(", ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun dualSimulationID() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.dualID(\"$query\", \"NORMAL\")").summary()
            print("dualSimulationID, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(", ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun strongSimulationID() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.strongID(\"$query\", \"NORMAL\")").summary()
            print("strongSimulationID, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(", ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun dualSimulationLabel() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.dualLabel(\"$query\", \"NORMAL\")").summary()
            print("dualSimulationLabel, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(", ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun strongSimulationLabel() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.strongLabel(\"$query\", \"NORMAL\")").summary()
            print("strongSimulationLabel, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(", ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun dualSimulationIDP() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.dualID(\"$query\", \"PARALLEL\")").summary()
            print("dualSimulationIDP, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun strongSimulationIDP() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.strongID(\"$query\", \"PARALLEL\")").summary()
            print("strongSimulationIDP, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun dualSimulationLabelP() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.dualLabel(\"$query\", \"PARALLEL\")").summary()
            print("dualSimulationLabelP, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun strongSimulationLabelP() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.strongLabel(\"$query\", \"PARALLEL\")").summary()
            print("strongSimulationLabelP, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun dualSimulationIDS() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.dualID(\"$query\", \"SHARED\")").summary()
            print("dualSimulationIDS, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun strongSimulationIDS() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.strongID(\"$query\", \"SHARED\")").summary()
            print("strongSimulationIDS, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun dualSimulationLabelS() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.dualLabel(\"$query\", \"SHARED\")").summary()
            print("dualSimulationLabelS, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }

    @Test
    fun strongSimulationLabelS() {
        driver.session().use({ session ->
            val result = session.run("CALL simulation.strongLabel(\"$query\", \"SHARED\")").summary()
            print("strongSimulationLabelS, ")
            print(result.profile()?.records())
            print(", ")
            print(result.profile()?.dbHits())
            print(", ")
            print(result.resultAvailableAfter(TimeUnit.MILLISECONDS))
            print(" ")
            println(result.resultConsumedAfter(TimeUnit.MILLISECONDS))
        })
    }
}