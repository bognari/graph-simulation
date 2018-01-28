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
class MultidimensionalGraphTest {
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
            return listOf("""MATCH (v)
OPTIONAL MATCH (v)-[r1]-(a1), (v)-[q1]-(b1)
WHERE a1 <> b1 AND r1 <> q1
WITH DISTINCT v, a1, b1
WITH DISTINCT v, toFloat(COUNT(a1)) AS possible

OPTIONAL MATCH (v)-[r2]-(a2)-[]-(b2)-[q2]-(v)
WHERE a2 <> b2 AND r2 <> q2
WITH DISTINCT v, a2, b2, possible
WHERE possible <> 0
WITH DISTINCT v, COUNT(a2) AS actual, possible
WITH v, actual/possible AS c
RETURN v.name, round(10^4 * toFloat(c))/10^4 AS c
ORDER BY v.name""", """MATCH (v)-[e]-()
RETURN DISTINCT type(e) AS dimension, v.name, COUNT(e) AS dd
ORDER BY v.name, dimension""", """MATCH (v)
OPTIONAL MATCH (v)-[e]-()
WITH
 toFloat(COUNT(DISTINCT v)) AS numberOfVertices,
 toFloat(COUNT(DISTINCT e)) AS numberOfEdges

MATCH (v)-[e]-()
WITH
 DISTINCT type(e) AS dimension,
 COUNT(DISTINCT v) AS nda,
 COUNT(DISTINCT v)/numberOfVertices AS ndc,
 COUNT(DISTINCT e) AS eda,
 COUNT(DISTINCT e)/numberOfEdges AS edc
RETURN
 dimension,
 nda,
 round(10^4 * toFloat(ndc))/10^4 AS ndc,
 eda,
 round(10^4 * toFloat(edc))/10^4 AS edc

ORDER BY dimension""", """MATCH (v)-[e]-()
RETURN v.name, COUNT(DISTINCT type(e)) AS na
ORDER BY v.name""",
                    /*"""MATCH (v)-[e]-()
WITH
 toFloat(COUNT(e)) AS degreeTotal,
 toFloat(COUNT(DISTINCT type(e))) AS numberOfDimensions

MATCH (v)-[e]-()
WITH v, type(e) AS dimension, COUNT(e) AS dimensionalDegree, degreeTotal, numberOfDimensions
WITH v, COLLECT(dimensionalDegree) AS dimensionalDegrees, toFloat(SUM(dimensionalDegree)) AS vertexDegreeTotal, degreeTotal, numberOfDimensions
WITH
 v,
 numberOfDimensions/(numberOfDimensions-1) *
 (1 - REDUCE(deg = 0.0, x in dimensionalDegrees|deg + (x/vertexDegreeTotal)^2)) AS mpc
RETURN v.name, round(10^4 * toFloat(mpc))/10^4 AS mpc

ORDER BY v.name""",   */
                    """MATCH (v)
OPTIONAL MATCH (v)-[r1]-(a1), (v)-[q1]-(b1)
WHERE a1 <> b1 AND type(r1) = type(q1)
WITH DISTINCT v, a1, b1
WITH DISTINCT v, toFloat(COUNT(a1)) AS possible
WHERE possible <> 0

OPTIONAL MATCH (v)-[r2]-(a2)-[s2]-(b2)-[q2]-(v)
WHERE a2 <> b2 AND type(r2) = type(q2) AND type(r2) <> type(s2)
WITH DISTINCT v, a2, b2, possible
WITH DISTINCT v, COUNT(a2) AS actual, possible
WITH v, actual/possible AS dc1
RETURN v.name, round(10^4 * toFloat(dc1))/10^4 AS dc1
ORDER BY v.name""",
                    """MATCH (v)
WITH toFloat(COUNT(DISTINCT v)) AS numberOfVertices
MATCH (v)-[e]-()
WITH DISTINCT numberOfVertices, type(e) AS d1
MATCH (v)-[e]-()
WITH DISTINCT numberOfVertices, d1, type(e) AS d2
OPTIONAL MATCH ()-[e1]-(v)-[e2]-()
WHERE type(e1) = d1 AND type(e2) = d2
WITH DISTINCT numberOfVertices, d1, d2, v
WITH DISTINCT d1, d2, COUNT(v)/numberOfVertices AS pairwise_multiplexity
RETURN d1, d2, round(10^4 * toFloat(pairwise_multiplexity))/10^4 AS pairwise_multiplexity
ORDER BY d1, d2""").map { it.replace("\n", " ") }.map { it.replace("\"", "'") }
        }

        @BeforeClass
        @JvmStatic
        fun setup() {
            driver = GraphDatabase.driver(neo4j.boltURI(), Config.build().withoutEncryption().toConfig())


            val import = listOf(
                    """CREATE
 // nodes
 (route1:Route {name:"Route1"}), (route2:Route {name:"Route2"}),
 (sensorA:Sensor {name:"SensorA"}), (sensorB:Sensor {name:"SensorB"}), (sensorC:Sensor {name:"SensorC"}),
 (segment1:Segment {name:"Segment1"}), (segment2: Segment {name:"Segment2"}), (segment3: Segment {name:"Segment3"}),
 (sw:Switch {name:"Switch"}),
 (swP1:SwitchPosition {name:"SwP1", position:"DIVERGING"}), (swP2:SwitchPosition {name:"SwP2", position:"STRAIGHT"}),
 // requires edges
 (route1)-[:requires]->(sensorA),
 (route1)-[:requires]->(sensorB),
 (route1)-[:requires]->(sensorC),
 (route2)-[:requires]->(sensorA),
 (route2)-[:requires]->(sensorC),
 // monitoredBy edges
 (segment1)-[:monitoredBy]->(sensorA),
 (sw)-[:monitoredBy]->(sensorA),
 (sw)-[:monitoredBy]->(sensorC),
 (segment2)-[:monitoredBy]->(sensorB),
 (segment3)-[:monitoredBy]->(sensorC),
 // connectsTo edges
 (segment1)-[:connectsTo]->(sw),
 (sw)-[:connectsTo]->(segment2),
 (sw)-[:connectsTo]->(segment3),
 // target edges
 (swP1)-[:target]->(sw),
 (swP2)-[:target]->(sw),
 // follows edges
 (route1)-[:follows]->(swP1),
 (route2)-[:follows]->(swP2)""")

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