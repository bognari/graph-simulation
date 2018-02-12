package de.tubs.ips.neo4j.simulation

import de.tubs.ips.neo4j.graph.Group
import de.tubs.ips.neo4j.graph.MyNode
import de.tubs.ips.neo4j.graph.MyRelationship
import de.tubs.ips.neo4j.parser.Visitor
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future


class Simulation(private val visitor: Visitor, private val db: GraphDatabaseService) {

    enum class Mode {
        NORMAL, SHARED, PARALLEL
    }

    companion object {
        private val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    }

    private val lazySim: Map<Group, MutableMap<MyNode, MutableSet<Node>>> by lazy {
        val ret = HashMap<Group, HashMap<MyNode, MutableSet<Node>>>()

        for (g in visitor.groups) {
            ret[g] = HashMap()
            for (n in g.nodes) {
                ret[g]!![n] = HashSet()
            }
        }

        for (v in db.allNodes) {
            for (g in visitor.groups) {
                g.nodes.asSequence()
                        .filter { it.match(v) }
                        .forEach { ret[g]!![it]!!.add(v) }
            }
        }

        ret
    }

    fun dualSimulation(mode: Mode = Mode.NORMAL): Map<Group, Map<MyNode, Set<Node>>> {
        val ball = Neo4JWrapper(db)
        val result = HashMap<Group, Map<MyNode, Set<Node>>>()

        if (mode == Mode.PARALLEL && visitor.groups.size > 1) {
            val tmp = HashMap<Group, Future<Map<MyNode, Set<Node>>>>()
            for (group in visitor.groups) {
                tmp[group] = pool.submit(Callable {
                    db.beginTx().use {
                        val sim = dualSim(ball, group, Mode.NORMAL)
                        it.close()
                        return@Callable sim
                    }
                })
            }


            for (entry in tmp) {
                result[entry.key] = entry.value.get()
            }
        } else {
            for (group in visitor.groups) {
                result[group] = dualSim(ball, group, mode)
            }
        }


        return result
    }

    fun strongSimulation(mode: Mode = Mode.NORMAL): Map<Group, Map<MyNode, Set<Node>>> {
        val result = HashMap<Group, Map<MyNode, Set<Node>>>()

        when (mode) {
            Mode.SHARED -> {
                // valid optimization because ball creation is extremely expensive
                val max = visitor.groups.maxBy { it.diameter }!!.diameter
                val balls = Ball.createBalls(db.allNodes, max).toList()

                for (group in visitor.groups) {
                    val s_ws = balls
                            .mapNotNull { extractMaxPG(it, dualSim(it, group)) }
                            .map { it.entries }

                    val r = HashMap<MyNode, MutableSet<Node>>()

                    for (v in group.nodes) {
                        r[v] = HashSet()
                    }

                    s_ws.flatMap { it }.forEach { r[it.key]!!.addAll(it.value) }

                    result[group] = r
                }
            }
            Mode.PARALLEL -> {
                /*val tmp = HashMap<Group, Future<Map<MyNode, Set<Node>>>>()
                for (group in visitor.groups) {
                    tmp[group] = pool.submit(Callable {
                        db.beginTx().use {
                            val r = HashMap<MyNode, MutableSet<Node>>()

                            for (v in group.nodes) {
                                r[v] = HashSet()
                            }

                            Ball.createBalls(db.allNodes, group.diameter)
                                    .mapNotNull { extractMaxPG(it, dualSim(it, group)) }
                                    .forEach {
                                        it.forEach {
                                            r[it.key]!!.addAll(it.value)
                                        }
                                    }

                            it.success()

                            return@Callable r as Map<MyNode, Set<Node>>
                        }
                    })
                }

                for (entry in tmp) {
                    result[entry.key] = entry.value.get()
                } */

                for (group in visitor.groups) {
                    val r = HashMap<MyNode, MutableSet<Node>>()

                    for (v in group.nodes) {
                        r[v] = HashSet()
                    }

                    val tmp = ArrayList<Future<Map<MyNode, Set<Node>>?>>()


                    db.allNodes.mapTo(tmp) { n ->
                        pool.submit(Callable {
                            db.beginTx().use {
                                val ball = Ball(n, group.diameter)
                                val ret = extractMaxPG(ball, dualSim(ball, group))
                                it.success()
                                return@Callable ret
                            }
                        })
                    }

                    tmp
                            .mapNotNull { it.get() }
                            .forEach {
                                for (entry in it) {
                                    r[entry.key]!!.addAll(entry.value)
                                }
                            }

                    result[group] = r
                }
            }
            else -> {
                for (group in visitor.groups) {
                    val r = HashMap<MyNode, MutableSet<Node>>()

                    for (v in group.nodes) {
                        r[v] = HashSet()
                    }

                    Ball.createBalls(db.allNodes, group.diameter)
                            .mapNotNull { extractMaxPG(it, dualSim(it, group)) }
                            .map { it.entries }
                            .forEach {
                                it.forEach {
                                    r[it.key]!!.addAll(it.value)
                                }
                            }


                    result[group] = r
                }
            }
        }

        return result
    }

    private fun extractMaxPG(ball: IDB, s_w: Map<MyNode, Set<Node>>): Map<MyNode, Set<Node>>? {
        val w = (ball as Ball).center

        return if (s_w.entries.any { it.value.contains(w) }) s_w else null
    }

    private fun dualSim(ball: IDB, group: Group, mode: Mode = Mode.NORMAL): Map<MyNode, Set<Node>> {
        val sim =
                if (mode == Mode.SHARED) {
                    lazySim[group]!!
                } else {
                    val ret = HashMap<MyNode, MutableSet<Node>>()

                    // for each u \in V_q in Q do
                    for (u in group.nodes) {
                        // sim(u) := {v | v is in G[w, d_Q] and l_Q(u) = l_G(v)};
                        ret[u] = sim(u, ball)
                    }
                    ret
                }

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

        return sim
    }

    private fun innerLoop(u: MyNode, u_: MyNode, sim: MutableMap<MyNode, MutableSet<Node>>, direction: Direction, relationship: MyRelationship): Boolean {
        var hasChanges = false

        val sim_u = sim[u]!!

// and each node v \in sim(u) do
        val iterator = sim_u.iterator()

        while (iterator.hasNext()) {
            val v = iterator.next()

            // if there is no edge (v, v') in G[w, d_Q] with v' \in sim(u') then
            if (!edgeIn(v, sim[u_]!!, direction, relationship)) {
                // sim(u) := sim(u) \ {v};
                iterator.remove()
                hasChanges = true
            }
        }

        return hasChanges
    }

    // if there is a edge (v, v') in G[w, d_Q] with v' \in sim(u')
    private fun edgeIn(v: Node, sim_u_: MutableSet<Node>, direction: Direction, relationship: MyRelationship): Boolean {
        if (sim_u_.isEmpty()) {
            return false
        }

        val relationships =
                if (relationship.direction == Direction.BOTH) { // if both than use all relationships
                    v.relationships
                } else {
                    v.getRelationships(direction)
                }

        return relationships.asSequence()
                .filter { sim_u_.contains(it.getOtherNode(v)) }
                .any { relationship.match(it) }
    }

    private fun sim(u: MyNode, ball: IDB): MutableSet<Node> {
        return if (ball is Neo4JWrapper && u.hasLabels()) {
            val iterator = ball.getNodes(u.labels.first())
            val ret = iterator.asSequence().filter { u.match(it) }.toHashSet()
            iterator.close()
            ret
        } else {
            ball.getNodes().filterTo(HashSet()) { u.match(it) } // only iterator and inline function
        }
    }
}