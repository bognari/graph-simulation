// words
LOAD CSV WITH HEADERS FROM "https://raw.githubusercontent.com/mneedham/neo4j-himym/master/data/import/words.csv" AS row
MERGE (word:Word {value: row.Word})

// words to episodes
LOAD CSV WITH HEADERS FROM "https://raw.githubusercontent.com/mneedham/neo4j-himym/master/data/import/words.csv" AS row
MATCH (word:Word {value: row.Word})
MATCH (episode:Episode {id: TOINT(row.EpisodeId)})
MERGE (word)-[:USED_IN_EPISODE {occurrences : TOINT(row.Occurrences)}]->(episode);


// tfidf words
LOAD CSV WITH HEADERS FROM "https://raw.githubusercontent.com/mneedham/neo4j-himym/master/data/import/words_tfidf.csv" AS row
MERGE (word:Word {value: row.Word})

LOAD CSV WITH HEADERS FROM "https://raw.githubusercontent.com/mneedham/neo4j-himym/master/data/import/words_tfidf.csv" AS row
MATCH (word:Word {value: row.Word})
MATCH (episode:Episode {id: TOINT(row.EpisodeId)})
MERGE (word)-[:USED_IN_EPISODE {score : TOFLOAT(row.Score)}]->(episode);
