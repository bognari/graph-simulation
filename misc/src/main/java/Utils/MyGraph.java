package Utils;

import Adapters.MyNode;
import Adapters.MyRelationship;
import ParserStuff.Walker;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.GraphMeasurer;
import org.jgrapht.graph.SimpleGraph;
import org.neo4j.graphdb.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyGraph {
    private final Walker walker;
    private final Graph<MyNode, MyRelationship> graph;
    private final GraphMeasurer<MyNode, MyRelationship> graphMeasurer;

    private int diameter = -1;

    public MyGraph(final Walker walker) {
        this.walker = walker;
        graph = new SimpleGraph<>(MyRelationship.class);

        for (MyNode myNode : walker.getNodes()) {
            graph.addVertex(myNode);
        }

        for (MyRelationship myRelationship : walker.getRelationships()) {
            if (graph.addEdge(myRelationship.getStartNode(), myRelationship.getEndNode(), myRelationship)) {
                System.out.println(myRelationship);
            } else {
                System.out.println("not inserted: " + myRelationship);
            }
        }

        graphMeasurer = new GraphMeasurer<>(graph);
    }

    public double getDiameter() {
        if (diameter == -1) {
            diameter = (int) graphMeasurer.getDiameter();
        }
        return diameter;
    }

    public List<Map<MyNode, Set<Node>>> strongSimulation(GraphDatabaseService db) {
        Stream<Ball> balls = Ball.createBalls(db.getAllNodes(), (int) getDiameter());

        java.util.List<Map<MyNode, Set<Node>>> perfect_subgraphs = balls
                //.peek(System.out::println)
                .map(this::dualSim)
                /*.map(sim() -> extractMaxPG(, ) )
                    return extractMaxPG(ball, sW);
                })*/
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        //return perfect_subgraphs;

        Ball ball = new Ball(db);

        Map<MyNode, Set<Node>> sim = dualSim(ball);

        return null;
    }

    public Map<MyNode, Set<Node>> dualSim(Ball ball) {
        Map<MyNode, Set<Node>> sim = new HashMap<>();

        // for each u \in V_q in Q do
        for (MyNode u : walker.getNodes()) {
            // sim(u) := {v | v is in G[w, d_Q] and l_Q(u) = l_G(v)};
            sim.put(u, sim(u, ball));
        }

        boolean hasChanges;

        // while there are changes do
        do {
            hasChanges = false;

            // for each edge (u, u') in E_Q
            for (MyRelationship relationship : walker.getRelationships()) {
                // and each node v \in sim(u) do
                // if there is no edge (v, v') in G[w, d_Q] with v' \in sim(u') then

                MyNode u = relationship.getStartNode();
                MyNode u_ = relationship.getEndNode();

                hasChanges |= innerLoop(u, u_ , ball, sim, Direction.OUTGOING);
            }

            // for each edge (u', u) in E_Q
            for (MyRelationship relationship : walker.getRelationships()) {
                // and each node v \in sim(u) do
                // if there is no edge (v', v) in G[w, d_Q] with v' \in sim(u') then

                MyNode u_ = relationship.getStartNode();
                MyNode u = relationship.getEndNode();

                hasChanges |= innerLoop(u, u_, ball, sim, Direction.INCOMING);
            }

            // if sim(u) = {} then return {} ?!

        } while (hasChanges);

        return sim;
    }

    private static boolean innerLoop(MyNode u, MyNode u_, Ball ball, Map<MyNode, Set<Node>> sim, Direction direction) {
        boolean hasChanges = false;

        Set<Node> sim_u = sim.get(u);

        if (sim_u == null) {
            return false;
        }

        // and each node v \in sim(u) do
        Iterator<Node> iterator = sim_u.iterator();

        while (iterator.hasNext()) {
            Node v = iterator.next();

            // if there is no edge (v, v') in G[w, d_Q] with v' \in sim(u') then
            if (!edgeIn(v, ball, u_, sim, direction)) {
                // sim(u) := sim(u) \ {v};
                iterator.remove();
                hasChanges = true;
            }
        }

        return hasChanges;
    }

    // if there is a edge (v, v') in G[w, d_Q] with v' \in sim(u')
    private static boolean edgeIn(Node v, Ball ball, MyNode u_, Map<MyNode, Set<Node>> sim, Direction direction) {

        Set<Node> sim_u_ = sim.get(u_);

        if (sim_u_.isEmpty()) {
            return false;
        }

        for (Relationship relationship : v.getRelationships(direction)) {

            if (!ball.relationships.contains(relationship)) {
                continue;
            }

            Node v_ = relationship.getOtherNode(v);

            if (sim_u_.contains(v_)) {  // TODO not working
                return true;
            } else {
                System.out.printf("%s -> %s%n", v_, getNodeString(sim_u_));
            }
        }
        return false;
    }

    private static String getNodeString(Iterable<Node> nodes) {
        List<String> list = new LinkedList<>();
        for (Node node : nodes) {
            list.add(node.toString());
        }
        return String.join(", ", list);
    }

    private static Set<Node> sim(MyNode u, Ball ball) {
        Set<Node> ret = new HashSet<>();

        for (Node v : ball.nodes) {
            if (hasLabels(u, v)) {
                ret.add(v);
            }
        }

        return ret;
    }

    private static boolean hasLabels(MyNode u, Node v) {
        for (Label label : u.getLabels()) {
            if (!v.hasLabel(label)) {
                return false;
            }
        }

        return true;
    }

    private static Ball extractMaxPG(Ball ball, Map<MyNode, Set<Node>> sim) {
        Node center = ball.center;

        /*// if w does not appear in S_w then
        if (!wAppearInSim(center, sim)) {
            // return nil;
            return null;
        } */

        // Construct the matching graph G_m w.r.t. S_w;
        // return the connected component G_s containing w in G_m
        return ball; // ball is still a connected graph
    }

    private static boolean wAppearInSim(Node w, Map<MyNode, Set<Node>> sim) {
        for (Set<Node> nodes : sim.values()) {
            if (nodes.contains(w)) {
                return true;
            }
        }
        return false;
    }

    public static Node getOther(Relationship relationship, Node node) {
        if (relationship.getStartNode().equals(node)) {
            return relationship.getEndNode();
        }
        if (relationship.getEndNode().equals(node)) {
            return relationship.getStartNode();
        }
        return null;
    }


    public static class Ball {
        private final Set<Node> nodes;
        private final List<Relationship> relationships;
        private final int diameter;
        private final Node center;

        public Ball(Node center, int diameter) {
            this.center = center;
            this.diameter = diameter;
            this.nodes = new HashSet<>();
            this.relationships = new LinkedList<>();

            init(center, diameter);
        }

        private void init(Node node, int diameter) {
            if (diameter == 0) {
                return;
            }

            for (Relationship relationship : node.getRelationships()) {
                Node next = relationship.getOtherNode(node);
                nodes.add(next);
                relationships.add(relationship);
                init(next, diameter - 1);
            }
        }

        public static Stream<Ball> createBalls(ResourceIterable<Node> nodes, int diameter) {
            return nodes.stream()
                    //.parallel()
                    .map(node -> new Ball(node, diameter));
        }


        @Override
        public String toString() {
            return String.format("%d | %d", nodes.size(), relationships.size());
        }

        public Ball(GraphDatabaseService db) {
            nodes = new HashSet<>();
            relationships = new LinkedList<>();

            diameter = 1;
            center = null;

            for(Node node: db.getAllNodes()) {
                         nodes.add(node);
            }

            for (Relationship relationship : db.getAllRelationships()) {
                relationships.add(relationship);
            }

        }
    }
}
