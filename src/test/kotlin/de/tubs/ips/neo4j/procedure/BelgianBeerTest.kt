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
class BelgianBeerTest {
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
            return listOf("""MATCH (b:BeerBrand)
WITH b
LIMIT 10
MATCH (b)--(n)
RETURN b,n;""", """MATCH (orval:BeerBrand {name:"Orval"})
RETURN orval;""").map { it.replace("\n", " ") }.map { it.replace("\"", "'") }
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withoutEncryption().toConfig())


            val import = listOf("CREATE INDEX ON :BeerBrand(name);", "CREATE INDEX ON :Brewery(name);", "CREATE INDEX ON :BeerType(name);", "CREATE INDEX ON :AlcoholPercentage(value);", """LOAD CSV WITH HEADERS FROM
"https://docs.google.com/spreadsheets/d/1FwWxlgnOhOtrUELIzLupDFW7euqXfeh8x3BeiEY_sbI/export?format=csv&id=1FwWxlgnOhOtrUELIzLupDFW7euqXfeh8x3BeiEY_sbI&gid=0" AS CSV
WITH CSV AS beercsv
WHERE beercsv.BeerType IS not NULL
MERGE (b:BeerType {name: beercsv.BeerType})
WITH beercsv
WHERE beercsv.BeerBrand IS not NULL
MERGE (b:BeerBrand {name: beercsv.BeerBrand})
WITH beercsv
WHERE beercsv.Brewery IS not NULL
MERGE (b:Brewery {name: beercsv.Brewery})
WITH beercsv
WHERE beercsv.AlcoholPercentage IS not NULL
MERGE (b:AlcoholPercentage {value: tofloat(replace(replace(beercsv.AlcoholPercentage,'%',''),',','.'))})
WITH beercsv
MATCH (ap:AlcoholPercentage {value: tofloat(replace(replace(beercsv.AlcoholPercentage,'%',''),',','.'))}),
(br:Brewery {name: beercsv.Brewery}),
(bb:BeerBrand {name: beercsv.BeerBrand}),
(bt:BeerType {name: beercsv.BeerType})
CREATE (bb)-[:HAS_ALCOHOLPERCENTAGE]->(ap),
(bb)-[:IS_A]->(bt),
(bb)<-[:BREWS]-(br);""")

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
    fun test() {
        val visitor = Visitor.setupVisitor(query!!)

        driver.session().use({ session ->
            val q1 = "CALL myprocedure.dualSimulationID(\"$query\")"
            val q2 = "CALL myprocedure.strongSimulationID(\"$query\")"
            val q3 = "CALL myprocedure.dualSimulationLabel(\"$query\")"
            val q4 = "CALL myprocedure.strongSimulationLabel(\"$query\")"

            val result = session.run(query).asSequence().toList()
            val r1 = session.run(q1).asSequence().map { it["content"] as MapValue }.map { it.asMap() }.toList()
            val r2 = session.run(q2).asSequence().map { it["content"] as MapValue }.map { it.asMap() }.toList()

            testResults(result, r1)
            testResults(result, r2)

            println()
        })
    }

    @Test
    fun normal() {
        driver.session().use({ session ->
            val result = session.run(query).asSequence().toList()
        })
    }

    @Test
    fun dualSimulationID() {
        driver.session().use({ session ->
            val result = session.run("CALL myprocedure.dualSimulationID(\"$query\")").asSequence().toList()
        })
    }

    @Test
    fun strongSimulationID() {
        driver.session().use({ session ->
            val result = session.run("CALL myprocedure.strongSimulationID(\"$query\")").asSequence().toList()
        })
    }

    @Test
    fun dualSimulationLabel() {
        driver.session().use({ session ->
            val result = session.run("CALL myprocedure.dualSimulationLabel(\"$query\")").asSequence().toList()
        })
    }

    @Test
    fun strongSimulationLabel() {
        driver.session().use({ session ->
            val result = session.run("CALL myprocedure.strongSimulationLabel(\"$query\")").asSequence().toList()
        })
    }
}