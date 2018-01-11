package example;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.Neo4jRule;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ProcedureTestTest {
    // This rule starts a Neo4j instance for us
    @Rule
    public Neo4jRule neo4j = new Neo4jRule()

            // This is the Procedure we want to test
            .withProcedure(ProcedureTest.class);

    @Test
    public void simulationPlayGround() throws Throwable {

        // In a try-block, to make sure we close the driver and session after the test
        try (Driver driver = GraphDatabase.driver(neo4j.boltURI(), Config.build()
                .withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig());
             Session session = driver.session()) {


            java.util.List<String> lines = Files.readAllLines(Paths.get("src", "test", "resources", "himym2.cql"));
            String content = String.join("\n", lines);
            String[] statements = content.split(";");

            for (String statement : statements) {
                session.run(statement);
            }

            StatementResult o = session.run("MATCH (c:Character)-[:APPEARED_IN_EPISODE]->()\n" +
                    "RETURN c.name, COUNT(*) AS times\n" +
                    "ORDER BY times DESC\n" +
                    "LIMIT 10");

            for (StatementResult it = o; it.hasNext(); ) {
                final Record record = it.next();

                Map<String, Object> map = record.asMap();
                System.out.println();
            }

            // When I use the index procedure to index a node
            session.run("CALL example.simulation('(c1:Character)-[:APPEARED_IN_EPISODE]->()')");
        }
    }
}
