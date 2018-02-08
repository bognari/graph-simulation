package de.tubs.ips.neo4j.simulation

import de.tubs.ips.neo4j.graph.Group
import de.tubs.ips.neo4j.graph.MyNode
import de.tubs.ips.neo4j.graph.MyRelationship
import de.tubs.ips.neo4j.parser.Visitor
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import java.util.stream.Collectors


class Simulation(private val visitor: Visitor, private val db: GraphDatabaseService) {

    enum class Mode {
        NORMAL, SHARED, PARALLEL
    }

    private val lazySim: Map<Group, MutableMap<MyNode, MutableSet<Node>>> by lazy {
        val ret = HashMap<Group, HashMap<MyNode, MutableSet<Node>>>()

        for (n in db.allNodes) {
            for (g in visitor.groups) {
                g.nodes
                        .filter { it.match(n) }
                        .forEach { ret.getOrPut(g, { HashMap() }).getOrPut(it, { HashSet() }).add(n) }
            }
        }
        ret
    }

    fun dualSimulation(): Map<Group, Map<MyNode, Set<Node>>> {
        val ball = Neo4JWrapper(db)
        val result = HashMap<Group, Map<MyNode, Set<Node>>>()

        val sims = visitor.groups.parallelStream().map { Pair(it, dualSim(ball, it, Mode.PARALLEL)) }.collect(Collectors.toList())

        for (sim in sims) {
            result[sim.first] = sim.second
        }

        return result
    }

    fun strongSimulation(): Map<Group, Map<MyNode, Set<Node>>> {
        val result = HashMap<Group, Map<MyNode, Set<Node>>>()

        // valid optimization because ball creation is extremely expensive
        val max = visitor.groups.maxBy { it.diameter }!!.diameter
        val balls = Ball.createBalls(db.allNodes, max, db)

        for (group in visitor.groups) {
            val s_ws = balls.mapNotNull { extractMaxPG(it, dualSim(it, group)) }
                    .map { it.entries }
                    .toList()

            val r = HashMap<MyNode, MutableSet<Node>>()

            for (v in group.nodes) {
                r[v] = HashSet()
            }

            s_ws.flatMap { it }.forEach { r[it.key]!!.addAll(it.value) }

            result[group] = r
        }

        return result
    }

    private fun extractMaxPG(ball: IDB, s_w: Map<MyNode, Set<Node>>): Map<MyNode, Set<Node>>? {
        val w = (ball as Ball).center

        return if (s_w.entries.any { it.value.contains(w) }) s_w else null
    }

    private fun dualSim(ball: IDB, group: Group, mode: Mode = Mode.NORMAL): Map<MyNode, Set<Node>> {
        val sim = when (mode) {
            Mode.SHARED -> lazySim[group]!!
            Mode.PARALLEL -> {

                val ret = HashMap<MyNode, MutableSet<Node>>()

                val sims = group.nodes.parallelStream().map { Pair(it, sim(it, ball)) }.collect(Collectors.toList())

                for (sim in sims) {
                    ret[sim.first] = sim.second
                }

                ret
            }
            else -> {
                val ret = HashMap<MyNode, MutableSet<Node>>()

                // for each u \in V_q in Q do
                for (u in group.nodes) {
                    // sim(u) := {v | v is in G[w, d_Q] and l_Q(u) = l_G(v)};
                    ret[u] = sim(u, ball)
                }
                ret
            }
        }

        db.beginTx().use {

            var hasChanges: Boolean

            // while there are changes do
            do {
                hasChanges = false

                // for each edge (u, u') in E_Q
                for (relationship in group.relationships) {
                    // and each node v \in sim(u) do
                    // if there is no edge (v, v') in G[w, d_Q] with v' \in sim(u') then

                    val u = relationship.startNode
                    val u_ = relationship.endNode

                    hasChanges = hasChanges or innerLoop(u, u_, sim, Direction.OUTGOING, relationship)
                }

                // for each edge (u', u) in E_Q
                for (relationship in group.relationships) {
                    // and each node v \in sim(u) do
                    // if there is no edge (v', v) in G[w, d_Q] with v' \in sim(u') then

                    val u_ = relationship.startNode
                    val u = relationship.endNode

                    hasChanges = hasChanges or innerLoop(u, u_, sim, Direction.INCOMING, relationship)
                }

                // if sim(u) = {} then return {} ?!

            } while (hasChanges)
            it.success()
        }

        return sim
    }

    private fun innerLoop(u: MyNode, u_: MyNode, sim: MutableMap<MyNode, MutableSet<Node>>, direction: Direction, relationship: MyRelationship): Boolean {
        var hasChanges = false

        val sim_u = sim[u] ?: return false

// and each node v \in sim(u) do
        val iterator = sim_u.iterator()

        while (iterator.hasNext()) {
            val v = iterator.next()

            // if there is no edge (v, v') in G[w, d_Q] with v' \in sim(u') then
            if (!edgeIn(v, sim[u_], direction, relationship)) {
                // sim(u) := sim(u) \ {v};
                iterator.remove()
                hasChanges = true
            }
        }

        return hasChanges
    }

    // if there is a edge (v, v') in G[w, d_Q] with v' \in sim(u')
    private fun edgeIn(v: Node, sim_u_: MutableSet<Node>?, direction: Direction, relationship: MyRelationship): Boolean {
        if (sim_u_ == null || sim_u_.isEmpty()) {
            return false
        }

        val relationships =
                if (relationship.direction == Direction.BOTH) { // if both than use all relationships
                    v.relationships
                } else {
                    v.getRelationships(direction)
                }

        return relationships
                .filter { relationship.match(it) }
                .map { it.getOtherNode(v) }
                .any { sim_u_.contains(it) }
    }

    private fun sim(u: MyNode, ball: IDB): MutableSet<Node> {
        db.beginTx().use {
            val ret = if (ball is Neo4JWrapper) {
                ball.getNodes().stream().parallel().filter({
                    val node = it // ugly as hell... </3 neo4j
                    var b = false
                    db.beginTx().use {
                        b = u.match(node)
                        it.success()
                    }
                    b
                }).collect(Collectors.toSet()) as MutableSet<Node>
            } else {
                val tmp = HashSet<Node>()
                ball.getNodes().filterTo(tmp) { u.match(it) }
            }
            it.success()
            return ret
        }
    }
}