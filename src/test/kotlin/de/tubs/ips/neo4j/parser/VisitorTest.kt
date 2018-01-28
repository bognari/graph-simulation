package de.tubs.ips.neo4j.parser

import org.junit.Assert.assertEquals
import org.junit.Test


class VisitorTest {
    @Test
    @Throws(Throwable::class)
    fun statement1() {
        val pattern = """Optional MATCH (you {name:"You"})-[:FRIEND]->(yourFriends)
                RETURN you, yourFriends"""

        val visitor = Visitor.setupVisitor(pattern)

        assertEquals(1, visitor.groups.size)

        assertEquals("Group<1>(mode=OPTIONAL, diameter=1, nodesDirectory={you=(you  <1|0|1>), yourFriends=(yourFriends  <1|1|0>)}, nodes=[(you  <1|0|1>), (yourFriends  <1|1|0>)], relationships=[(you  <1|0|1>) -[FRIEND]-> (yourFriends  <1|1|0>) <OUTGOING>])", visitor.groups[0].toString())

    }

    @Test
    @Throws(Throwable::class)
    fun statement2() {
        val pattern = """MATCH (neo:Database {name:"Neo4j"})
                MATCH (anna:Person {name:"Anna"})
                RETURN neo"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(2, visitor.groups.size)

        assertEquals("Group(mode=MATCH, nodesDirectory={neo=(<neo> Database <0|0|0>)}, relationshipsDirectory={}, nodes=[(<neo> Database <0|0|0>)], relationships=[])", visitor.groups[0].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={anna=(<anna> Person <0|0|0>)}, relationshipsDirectory={}, nodes=[(<anna> Person <0|0|0>)], relationships=[])", visitor.groups[1].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement3() {
        val pattern = """MATCH (you {name:"You"})
                MATCH (expert)-[:WORKED_WITH]->(db:Database {name:"Neo4j"})
                MATCH (you)-[:FRIEND*..5]-(expert)
                RETURN db, expert, path"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 3)

        assertEquals("Group(mode=MATCH, nodesDirectory={you=(<you> no labels <0|0|0>)}, relationshipsDirectory={}, nodes=[(<you> no labels <0|0|0>)], relationships=[])", visitor.groups[0].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={expert=(<expert> no labels <1|0|1>), db=(<db> Database <1|1|0>)}, relationshipsDirectory={}, nodes=[(<expert> no labels <1|0|1>), (<db> Database <1|1|0>)], relationships=[(<expert> no labels <1|0|1>) -[WORKED_WITH]-> (<db> Database <1|1|0>) <OUTGOING>])", visitor.groups[1].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={expert=(<expert> no labels <1|1|1>), you=(<you> no labels <1|1|1>)}, relationshipsDirectory={}, nodes=[(<you> no labels <1|1|1>), (<expert> no labels <1|1|1>)], relationships=[(<you> no labels <1|1|1>) -[FRIEND]- (<expert> no labels <1|1|1>) <null>])", visitor.groups[2].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement4() {
        val pattern = """MATCH (p:Product {productName:"Chocolade"})<-[:PRODUCT]-(:Order)<-[:PURCHASED]-(c:Customer)
                RETURN distinct c.companyName;"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 1)

        assertEquals("Group<4>(mode=MATCH, diameter=2, nodesDirectory={p=(p [Product] <1|1|0>), g4n1=(g4n1 [Order] <2|1|1>), c=(c [Customer] <1|0|1>)}, nodes=[(p [Product] <1|1|0>), (g4n1 [Order] <2|1|1>), (c [Customer] <1|0|1>)], relationships=[(p [Product] <1|1|0>) <-[PRODUCT]- (g4n1 [Order] <2|1|1>) <INCOMING>, (g4n1 [Order] <2|1|1>) <-[PURCHASED]- (c [Customer] <1|0|1>) <INCOMING>])", visitor.groups[0].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement5() {
        val pattern = """MATCH (c:Customer {companyName:"Drachenblut Delikatessen"})
                OPTIONAL MATCH (p:Product)<-[pu:PRODUCT]-(:Order)<-[:PURCHASED]-(c)
                RETURN p.productName, toInt(sum(pu.unitPrice * pu.quantity)) AS volume
                ORDER BY volume DESC;"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 2)

        assertEquals("Group(mode=MATCH, nodesDirectory={c=(<c> Customer <0|0|0>)}, relationshipsDirectory={}, nodes=[(<c> Customer <0|0|0>)], relationships=[])", visitor.groups[0].toString())

        assertEquals("Group(mode=OPTIONAL, nodesDirectory={p=(<p> Product <1|1|0>), c=(<c> no labels <1|0|1>)}, relationshipsDirectory={pu=MyRelationshipPrototype(parsingDirection=INCOMING, variable=pu)}, nodes=[(<p> Product <1|1|0>), (Order <2|1|1>), (<c> no labels <1|0|1>)], relationships=[(<p> Product <1|1|0>) <-[<pu> PRODUCT]- (Order <2|1|1>) <INCOMING>, (Order <2|1|1>) <-[PURCHASED]- (<c> no labels <1|0|1>) <INCOMING>])", visitor.groups[1].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement6() {
        val pattern = """MATCH (:Order)<-[:SOLD]-(e:Employee)
                RETURN e.name, count(*) AS cnt
                ORDER BY cnt DESC LIMIT 10"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 1)

        assertEquals("Group(mode=MATCH, nodesDirectory={e=(<e> Employee <1|0|1>)}, relationshipsDirectory={}, nodes=[(Order <1|1|0>), (<e> Employee <1|0|1>)], relationships=[(Order <1|1|0>) <-[SOLD]- (<e> Employee <1|0|1>) <INCOMING>])", visitor.groups[0].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement7() {
        val pattern = """MATCH (country)-[v:VOTE]->()
                WITH country, count(DISTINCT v.year) AS nb_voting ,collect(DISTINCT v.year) AS year_voting
                MATCH ()-[p:VOTE]->(country)
                WITH country, nb_voting,year_voting, count(DISTINCT p.year) AS nb_participation ,collect(DISTINCT p.year) AS year_participation
                RETURN country.name,nb_voting,year_voting,nb_participation,year_participation
                ORDER BY nb_participation DESC"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 2)

        assertEquals("Group(mode=MATCH, nodesDirectory={country=(<country> no labels <1|0|1>)}, relationshipsDirectory={v=MyRelationshipPrototype(parsingDirection=OUTGOING, variable=v)}, nodes=[(<country> no labels <1|0|1>), (no labels <1|1|0>)], relationships=[(<country> no labels <1|0|1>) -[<v> VOTE]-> (no labels <1|1|0>) <OUTGOING>])", visitor.groups[0].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={country=(<country> no labels <1|1|0>)}, relationshipsDirectory={p=MyRelationshipPrototype(parsingDirection=OUTGOING, variable=p)}, nodes=[(no labels <1|0|1>), (<country> no labels <1|1|0>)], relationships=[(no labels <1|0|1>) -[<p> VOTE]-> (<country> no labels <1|1|0>) <OUTGOING>])", visitor.groups[1].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement8() {
        val pattern = """MATCH ()-[up:VOTE]->(country)
                WHERE up.score >= 8
                WITH country, up.year AS year, SUM(up.score) AS up_score
                MATCH ()-[all1:VOTE]->(country)
                WHERE all1.year = year
                WITH country, year, up_score , SUM(all1.score) AS all1_score
                RETURN country.name,year, all1_score,up_score
                ORDER BY year,all1_score DESC"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 2)

        assertEquals("Group(mode=MATCH, nodesDirectory={country=(<country> no labels <1|1|0>)}, relationshipsDirectory={up=MyRelationshipPrototype(parsingDirection=OUTGOING, variable=up)}, nodes=[(no labels <1|0|1>), (<country> no labels <1|1|0>)], relationships=[(no labels <1|0|1>) -[<up> VOTE]-> (<country> no labels <1|1|0>) <OUTGOING>])", visitor.groups[0].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={country=(<country> no labels <1|1|0>)}, relationshipsDirectory={all1=MyRelationshipPrototype(parsingDirection=OUTGOING, variable=all1)}, nodes=[(no labels <1|0|1>), (<country> no labels <1|1|0>)], relationships=[(no labels <1|0|1>) -[<all1> VOTE]-> (<country> no labels <1|1|0>) <OUTGOING>])", visitor.groups[1].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement9() {
        val pattern = """MATCH (nt:NodeType)--(:NodeDomain { name: "Process" })
WITH COLLECT(nt.name) AS processNodeTypes
MATCH (m) WHERE LENGTH(FILTER(lbl IN labels(m) WHERE lbl IN processNodeTypes)) > 0
RETURN m LIMIT 50;"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 2)

        assertEquals("Group(mode=MATCH, nodesDirectory={nt=(<nt> NodeType <1|1|1>)}, relationshipsDirectory={}, nodes=[(<nt> NodeType <1|1|1>), (NodeDomain <1|1|1>)], relationships=[(<nt> NodeType <1|1|1>) -[]- (NodeDomain <1|1|1>) <null>])", visitor.groups[0].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={m=(<m> no labels <0|0|0>)}, relationshipsDirectory={}, nodes=[(<m> no labels <0|0|0>)], relationships=[])", visitor.groups[1].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement10() {
        val pattern = """MATCH (css:CssFile)--(vw:MvcView)
OPTIONAL MATCH (vw)--(ctl:MvcController)
RETURN css, vw, ctl;"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 2)

        assertEquals("Group(mode=MATCH, nodesDirectory={vw=(<vw> MvcView <1|1|1>), css=(<css> CssFile <1|1|1>)}, relationshipsDirectory={}, nodes=[(<css> CssFile <1|1|1>), (<vw> MvcView <1|1|1>)], relationships=[(<css> CssFile <1|1|1>) -[]- (<vw> MvcView <1|1|1>) <null>])", visitor.groups[0].toString())

        assertEquals("Group(mode=OPTIONAL, nodesDirectory={vw=(<vw> no labels <1|1|1>), ctl=(<ctl> MvcController <1|1|1>)}, relationshipsDirectory={}, nodes=[(<vw> no labels <1|1|1>), (<ctl> MvcController <1|1|1>)], relationships=[(<vw> no labels <1|1|1>) -[]- (<ctl> MvcController <1|1|1>) <null>])", visitor.groups[1].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement11() {
        val pattern = """MATCH (checkin:CheckIn { name: "Change set 2231" })
MATCH (css_file)-[:INCLUDED_IN]->(checkin)
MATCH (css_file)--(vw:MvcView)
MATCH (vw)-[*1..3]-(feature:Feature)
RETURN DISTINCT checkin, css_file, vw, feature;"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 4)

        assertEquals("Group(mode=MATCH, nodesDirectory={checkin=(<checkin> CheckIn <0|0|0>)}, relationshipsDirectory={}, nodes=[(<checkin> CheckIn <0|0|0>)], relationships=[])", visitor.groups[0].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={css_file=(<css_file> no labels <1|0|1>), checkin=(<checkin> no labels <1|1|0>)}, relationshipsDirectory={}, nodes=[(<css_file> no labels <1|0|1>), (<checkin> no labels <1|1|0>)], relationships=[(<css_file> no labels <1|0|1>) -[INCLUDED_IN]-> (<checkin> no labels <1|1|0>) <OUTGOING>])", visitor.groups[1].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={css_file=(<css_file> no labels <1|1|1>), vw=(<vw> MvcView <1|1|1>)}, relationshipsDirectory={}, nodes=[(<css_file> no labels <1|1|1>), (<vw> MvcView <1|1|1>)], relationships=[(<css_file> no labels <1|1|1>) -[]- (<vw> MvcView <1|1|1>) <null>])", visitor.groups[2].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={vw=(<vw> no labels <1|1|1>), feature=(<feature> Feature <1|1|1>)}, relationshipsDirectory={}, nodes=[(<vw> no labels <1|1|1>), (<feature> Feature <1|1|1>)], relationships=[(<vw> no labels <1|1|1>) -[]- (<feature> Feature <1|1|1>) <null>])", visitor.groups[3].toString())
    }

    @Test
    @Throws(Throwable::class)
    fun statement12() {
        val pattern = """MATCH (checkin:CheckIn { name: "Change set 2231"})
MATCH (css_file)-[:INCLUDED_IN]->(checkin)
MATCH (css_file)--(vw:MvcView)
MATCH (vw)-[*1..3]-(feature:Feature)
MATCH (t_case:TestCase)--(t_suite:TestSuite)-[*1..2]-(feature)
RETURN DISTINCT t_suite.name AS `Test Suite`, t_case.name AS `Test CASE`;"""

        val visitor = Visitor.setupVisitor(pattern)

        
        assertEquals(visitor.groups.size, 5)

        assertEquals("Group(mode=MATCH, nodesDirectory={checkin=(<checkin> CheckIn <0|0|0>)}, relationshipsDirectory={}, nodes=[(<checkin> CheckIn <0|0|0>)], relationships=[])", visitor.groups[0].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={css_file=(<css_file> no labels <1|0|1>), checkin=(<checkin> no labels <1|1|0>)}, relationshipsDirectory={}, nodes=[(<css_file> no labels <1|0|1>), (<checkin> no labels <1|1|0>)], relationships=[(<css_file> no labels <1|0|1>) -[INCLUDED_IN]-> (<checkin> no labels <1|1|0>) <OUTGOING>])", visitor.groups[1].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={css_file=(<css_file> no labels <1|1|1>), vw=(<vw> MvcView <1|1|1>)}, relationshipsDirectory={}, nodes=[(<css_file> no labels <1|1|1>), (<vw> MvcView <1|1|1>)], relationships=[(<css_file> no labels <1|1|1>) -[]- (<vw> MvcView <1|1|1>) <null>])", visitor.groups[2].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={vw=(<vw> no labels <1|1|1>), feature=(<feature> Feature <1|1|1>)}, relationshipsDirectory={}, nodes=[(<vw> no labels <1|1|1>), (<feature> Feature <1|1|1>)], relationships=[(<vw> no labels <1|1|1>) -[]- (<feature> Feature <1|1|1>) <null>])", visitor.groups[3].toString())

        assertEquals("Group(mode=MATCH, nodesDirectory={t_case=(<t_case> TestCase <1|1|1>), feature=(<feature> no labels <1|1|1>), t_suite=(<t_suite> TestSuite <2|2|2>)}, relationshipsDirectory={}, nodes=[(<t_case> TestCase <1|1|1>), (<t_suite> TestSuite <2|2|2>), (<feature> no labels <1|1|1>)], relationships=[(<t_case> TestCase <1|1|1>) -[]- (<t_suite> TestSuite <2|2|2>) <null>, (<t_suite> TestSuite <2|2|2>) -[]- (<feature> no labels <1|1|1>) <null>])", visitor.groups[4].toString())
    }
}
