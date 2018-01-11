// phrases
USING PERIODIC COMMIT 1000
LOAD CSV WITH HEADERS FROM "file:///Users/anveena/IntelliJIDEAProjects/neo4j-procedure-template/src/test/resources/words_scikit.csv" AS row
MERGE (phrase:Phrase {value: row.Phrase});

// episode -> phrase
USING PERIODIC COMMIT 1000
LOAD CSV WITH HEADERS FROM "file:///Users/anveena/IntelliJIDEAProjects/neo4j-procedure-template/src/test/resources/words_scikit.csv" AS row
MATCH (phrase:Phrase {value: row.Phrase})
MATCH (episode:Episode {id: TOINT(row.EpisodeId)})
MERGE (episode)-[:CONTAINED_PHRASE {times:TOINT(row.Count)}]->(phrase);
