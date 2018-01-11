// episodes
LOAD CSV WITH HEADERS FROM 'file:///Users/anveena/IntelliJIDEAProjects/neo4j-procedure-template/src/test/resources/episodes_full.csv'
AS row
MERGE (episode:Episode {id: toint(row.NumberOverall)})
SET episode.title = row.Title,
episode.viewers = toFloat(row.Viewers),
episode.dateAired = row.DateAired,
episode.timestamp = toint(row.Timestamp),
episode.number = toint(row.NumberInSeason),
episode.season = toint(row.Season),
episode.ranking = toint(row.Rating);

// characters
LOAD CSV WITH HEADERS FROM 'file:///Users/anveena/IntelliJIDEAProjects/neo4j-procedure-template/src/test/resources/characters.csv'
AS row
MERGE (character:Character {name: row.Character});

// character -> episode
LOAD CSV WITH HEADERS FROM 'file:///Users/anveena/IntelliJIDEAProjects/neo4j-procedure-template/src/test/resources/characters.csv'
AS row
MATCH (episode:Episode {id: toint(row.EpisodeId)})
MATCH (character:Character {name: row.Character})
MERGE (character)-[:APPEARED_IN_EPISODE]->(episode);