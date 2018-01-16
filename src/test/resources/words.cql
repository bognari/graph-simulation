// phrases
USING PERIODIC COMMIT 1000
LOAD CSV WITH HEADERS FROM "https://raw.githubusercontent.com/mneedham/neo4j-himym/master/data/import/words_scikit.csv" AS row
MERGE (phrase:Phrase {value: row.Phrase});

// episode -> phrase
USING PERIODIC COMMIT 1000
LOAD CSV WITH HEADERS FROM "https://raw.githubusercontent.com/mneedham/neo4j-himym/master/data/import/words_scikit.csv" AS row
MATCH (phrase:Phrase {value: row.Phrase})
MATCH (episode:Episode {id: TOINT(row.EpisodeId)})
MERGE (episode)-[:CONTAINED_PHRASE {times:TOINT(row.Count)}]->(phrase);
