package example;

import ParserStuff.Walker;
import Utils.MyGraph;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.Test;

public class WalkerTest {

    @Test
    public void walk() throws Throwable {
        String pattern = "(node1:Label1:Swedish {name: 'Alice', age: 38, bla: true, property: $value, address: {city: 'London', residential: true}})-[:KNOWS|:LOVES *01..99]->(node2:Label2)-[*01..05]->(node3:Label3)<-[:KNOWS]-(node4:Label4)-[*]-(node5:Label5), (node2)-->(node1)" ;

        PatternLexer patternLexer = new PatternLexer(CharStreams.fromString(pattern, "user input"));
        CommonTokenStream commonTokenStream = new CommonTokenStream(patternLexer);
        PatternParser patternParser = new PatternParser(commonTokenStream);
        PatternParser.PatternContext context = patternParser.pattern();
        Walker visitor = new Walker();
        visitor.visit(context);

        MyGraph graph = new MyGraph(visitor);

        System.out.println("graph.getDiameter() = " + graph.getDiameter());
        
        System.out.println();
    }
}
