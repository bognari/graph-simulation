// references
LOAD CSV WITH HEADERS FROM "file:///Users/anveena/IntelliJIDEAProjects/graph-dualSimulation/src/test/resources/references.csv" AS row
MERGE (:Reference {text: row.ReferenceText});

// referenced -> reference
LOAD CSV WITH HEADERS FROM "file:///Users/anveena/IntelliJIDEAProjects/graph-dualSimulation/src/test/resources/references.csv" AS row
MATCH (reference:Reference {text: row.ReferenceText})
MATCH (referenced:Episode {id: TOINT(row.ReferencedEpisodeId)})
MERGE (referenced)<-[:HAPPENED_IN]-(reference);

// referencing -> reference
LOAD CSV WITH HEADERS FROM "file:///Users/anveena/IntelliJIDEAProjects/graph-dualSimulation/src/test/resources/references.csv" AS row
MATCH (reference:Reference {text: row.ReferenceText})
MATCH (referencing:Episode {id: TOINT(row.ReferencingEpisodeId)})
MERGE (referencing)-[:REFERENCES]->(reference);
