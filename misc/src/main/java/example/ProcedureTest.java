package example;

import Adapters.MyNode;
import ParserStuff.Walker;
import Utils.MyGraph;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This is an example showing how you could expose Neo4j's full text indexes as
 * two procedures - one for updating indexes, and one for querying by label and
 * the lucene query language.
 */
public class ProcedureTest
{
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;

    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;

    /**
     * This declares the first of two procedures in this class - a
     * procedure that performs queries in a legacy index.
     *
     * It returns a Stream of Records, where records are
     * specified per procedure. This particular procedure returns
     * a stream of bla records.
     *
     * The arguments to this procedure are annotated with the
     * {@link Name} annotation and define the position, name
     * and type of arguments required to invoke this procedure.
     * There is a limited set of types you can use for arguments,
     * these are as follows:
     *
     * <ul>
     *     <li>{@link String}</li>
     *     <li>{@link Long} or {@code long}</li>
     *     <li>{@link Double} or {@code double}</li>
     *     <li>{@link Number}</li>
     *     <li>{@link Boolean} or {@code boolean}</li>
     *     <li>{@link Map} with key {@link String} and value {@link Object}</li>
     *     <li>{@link List} of elements of any valid argument type, including {@link List}</li>
     *     <li>{@link Object}, meaning any of the valid argument types</li>
     * </ul>
     *
     * @param pattern
     * @return the nodes found by the query
     */
    // TODO: This is here as a workaround, because index().forNodes() is not read-only
    @Procedure(value = "example.simulation", mode = Mode.WRITE)
    @Description("")
    public Stream<Output> simulation(@Name("pattern") String pattern)
    {
        ResourceIterable<Node> nodes = db.getAllNodes();



        PatternLexer patternLexer = new PatternLexer(CharStreams.fromString(pattern, "user input"));
        CommonTokenStream commonTokenStream = new CommonTokenStream(patternLexer);
        PatternParser patternParser = new PatternParser(commonTokenStream);
        PatternParser.PatternContext context = patternParser.pattern();
        Walker visitor = new Walker();
        visitor.visit(context);

        MyGraph graph = new MyGraph(visitor);
        List<Map<MyNode, Set<Node>>> ret = graph.strongSimulation(db);

        //System.out.println(context.toStringTree(patternParser));

        

        return null;
    }

    public static class Output {
        public String out;
    }
}
