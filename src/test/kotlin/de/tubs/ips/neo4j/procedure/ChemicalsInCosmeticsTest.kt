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
class ChemicalsInCosmeticsTest {
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
            return listOf("MATCH (p:Product)<-[r:USED_IN]-(ch:Chemical) RETURN p, ch;","""MATCH (co:Company)-[:OWNS]->(b:Brand {name: "Revlon"}), (b)-[:PRODUCES]->(p:Product)-[:BELONGS_TO]->(s:Category)<-[:CONTAINS]-(c:Category), (ch:Chemical)-[:USED_IN]-(p)
RETURN co, b, p, s, c, ch LIMIT 1;""",
                    """MATCH (b:Brand)-[:PRODUCES]->(p:Product)
RETURN b.name AS Brand, count(p) AS numOfProducts
ORDER BY numOfProducts DESC;""",
                    """MATCH (b:Brand)-[:PRODUCES]->(p:Product)<-[r:USED_IN]-(ch:Chemical)
WHERE r.discontinuedDate IS NULL
RETURN b AS brand, count(p) AS productCount
ORDER BY productCount DESC;""", """MATCH (b:Brand)-[:PRODUCES]->(p:Product)
WHERE b.name = "Revlon"
RETURN p.name, p.initialDateReported, p.mostRecentDateReported;""", """MATCH (b:Brand {name: "Revlon"})-[:PRODUCES]->(p:Product)<-[r:USED_IN]-(ch:Chemical)
WHERE r.dateChemicalRemoved IS NULL
RETURN b AS brand, count(p) AS productCount, ch.name AS chemical;""").map { it.replace("\n", " ") }.map { it.replace("\"", "'") }
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withoutEncryption().toConfig())


            val import = listOf("CREATE INDEX ON :Product(cdphId);",
            "CREATE INDEX ON :Product(csfId);",
            "CREATE INDEX ON :Product(csf);",
            "CREATE INDEX ON :Chemical(casId);",
            "CREATE INDEX ON :Chemical(name);",
            "CREATE INDEX ON :Category(name);",

            "CREATE CONSTRAINT ON (c:Company) ASSERT c.name IS UNIQUE;",
            "CREATE CONSTRAINT ON (c:Company) ASSERT c.id IS UNIQUE;",
            "CREATE CONSTRAINT ON (b:Brand) ASSERT b.name IS UNIQUE;",
            "CREATE CONSTRAINT ON (p:Product) ASSERT p.name IS UNIQUE;",
            "CREATE CONSTRAINT ON (ch:Chemical) ASSERT ch.casNumber IS UNIQUE;",
            "CREATE CONSTRAINT ON (c:Category) ASSERT c.id IS UNIQUE;",
                    """//Import ChemicalsInCosmetics Data Indexes
USING PERIODIC COMMIT 1000
LOAD CSV WITH HEADERS FROM "https://gist.graphgrid.com/data/Chemicals_in_Cosmetics.csv" AS row
WITH row LIMIT 1000
WHERE row.CompanyId IS NOT NULL and row.CompanyName IS NOT NULL and row.BrandName IS NOT NULL and row.ProductName IS NOT NULL and row.PrimaryCategory IS NOT NULL and row.PrimaryCategoryId IS NOT NULL and row.ChemicalName IS NOT NULL and row.CasId IS NOT NULL and row.CasNumber IS NOT NULL and row.SubCategoryId IS NOT NULL and row.SubCategory IS NOT NULL
//Create Nodes
MERGE (co:Company {id: row.CompanyId}) ON CREATE SET co.name = row.CompanyName
MERGE (b:Brand {name: row.BrandName})
MERGE (p:Product {name: row.ProductName}) ON CREATE SET p.Id = row.ProductId, p.csfId = row.CSFId, p.csf = row.CSF, p.initialDateReported = row.InitialDateReported, p.mostRecentDateReported = row.MostRecentDateReported, p.discontinuedDate = row.DiscontinuedDate
MERGE (ch:Chemical {casNumber: row.CasNumber}) ON CREATE SET ch.Id = row.CasId, ch.name = row.ChemicalName
MERGE (c:Category {id: row.PrimaryCategoryId}) ON CREATE SET c.name = row.PrimaryCategory
MERGE (s:Category {id: row.SubCategoryId}) ON CREATE SET s.name = row.SubCategory
//Create Relationships
MERGE (co)-[:OWNS]->(b)
MERGE (b)-[:PRODUCES]->(p)
MERGE (p)-[:BELONGS_TO]->(s)
MERGE (c)-[:CONTAINS]->(s)
MERGE (ch)-[r:USED_IN]->(p)
//Set Properties on USED_IN Relationship
ON CREATE SET
r.chemicalCreatedOn = row.ChemicalCreatedAt,
r.chemicalUpdatedOn = row.ChemicalUpdatedAt,
r.dateChemicalRemoved = row.ChemicalDateRemoved,
r.chemicalCount = toInt(row.ChemicalCount),
r.chemicalId = row.ChemicalId;""")

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
            val result = session.run("CALL myprocedure.dualSimulationID(\"$query\", \"NORMAL\")").summary()
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
            val result = session.run("CALL myprocedure.strongSimulationID(\"$query\", \"NORMAL\")").summary()
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
            val result = session.run("CALL myprocedure.dualSimulationLabel(\"$query\", \"NORMAL\")").summary()
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
            val result = session.run("CALL myprocedure.strongSimulationLabel(\"$query\", \"NORMAL\")").summary()
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
            val result = session.run("CALL myprocedure.dualSimulationID(\"$query\", \"PARALLEL\")").summary()
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
            val result = session.run("CALL myprocedure.strongSimulationID(\"$query\", \"PARALLEL\")").summary()
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
            val result = session.run("CALL myprocedure.dualSimulationLabel(\"$query\", \"PARALLEL\")").summary()
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
            val result = session.run("CALL myprocedure.strongSimulationLabel(\"$query\", \"PARALLEL\")").summary()
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
            val result = session.run("CALL myprocedure.dualSimulationID(\"$query\", \"SHARED\")").summary()
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
            val result = session.run("CALL myprocedure.strongSimulationID(\"$query\", \"SHARED\")").summary()
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
            val result = session.run("CALL myprocedure.dualSimulationLabel(\"$query\", \"SHARED\")").summary()
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
            val result = session.run("CALL myprocedure.strongSimulationLabel(\"$query\", \"SHARED\")").summary()
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