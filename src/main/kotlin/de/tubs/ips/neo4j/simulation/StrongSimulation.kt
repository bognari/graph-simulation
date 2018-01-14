package de.tubs.ips.neo4j.simulation

import de.tubs.ips.neo4j.graph.Group
import de.tubs.ips.neo4j.graph.MyNode
import de.tubs.ips.neo4j.parser.Visitor
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node


class StrongSimulation(val visitor: Visitor, private val db: GraphDatabaseService) {

    fun dualSimulation(): Map<Group, Map<MyNode, Set<Node>>> {
        val ball = Neo4JWrapper(db)
        val result = HashMap<Group, Map<MyNode, Set<Node>>>()
        for (group in visitor.groups) {
            result[group] = dualSim(ball, group)
        }
        return result
    }

    fun strongSimulation(): Map<Group, Map<MyNode, Set<Node>>> {
        return emptyMap()
    }

    private fun dualSim(ball: IDB, group: Group): Map<MyNode, Set<Node>> {
        val sim = HashMap<MyNode, MutableSet<Node>>()

        // for each u \in V_q in Q do
        for (u in group.nodes) {
            // sim(u) := {v | v is in G[w, d_Q] and l_Q(u) = l_G(v)};
            sim.put(u, sim(u, ball))
        }

        println()

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

                hasChanges = hasChanges or innerLoop(u, u_, ball, sim, Direction.OUTGOING)
            }

            // for each edge (u', u) in E_Q
            for (relationship in group.relationships) {
                // and each node v \in sim(u) do
                // if there is no edge (v', v) in G[w, d_Q] with v' \in sim(u') then

                val u_ = relationship.startNode
                val u = relationship.endNode

                hasChanges = hasChanges or innerLoop(u, u_, ball, sim, Direction.INCOMING)
            }

            // if sim(u) = {} then return {} ?!

        } while (hasChanges)

        return sim
    }

    private fun innerLoop(u: MyNode, u_: MyNode, ball: IDB, sim: MutableMap<MyNode, MutableSet<Node>>, direction: Direction): Boolean {
        var hasChanges = false

        val sim_u = sim[u] ?: return false

// and each node v \in sim(u) do
        val iterator = sim_u.iterator()

        while (iterator.hasNext()) {
            val v = iterator.next()

            // if there is no edge (v, v') in G[w, d_Q] with v' \in sim(u') then
            if (!edgeIn(v, ball, sim[u_], direction)) {
                // sim(u) := sim(u) \ {v};
                iterator.remove()
                hasChanges = true
            }
        }

        return hasChanges
    }

    // if there is a edge (v, v') in G[w, d_Q] with v' \in sim(u')
    private fun edgeIn(v: Node, ball: IDB, sim_u_: MutableSet<Node>?, direction: Direction): Boolean {
        if (sim_u_ == null || sim_u_.isEmpty()) {
            return false
        }

        return v.getRelationships(direction)
                .asSequence()
                .filter { ball.getRelationships().contains(it) }
                .map { it.getOtherNode(v) }
                .any { sim_u_.contains(it) }
    }

    private fun sim(u: MyNode, ball: IDB): MutableSet<Node> {
        val ret = HashSet<Node>()

        ball.getNodes().filterTo(ret) { hasLabels(u, it) }

        return ret
    }

    private fun hasLabels(u: MyNode, v: Node): Boolean {
        return u.labels.none() || u.labels.all { v.hasLabel(it) }
    }
}