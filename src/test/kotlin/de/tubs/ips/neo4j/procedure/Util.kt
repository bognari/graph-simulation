package de.tubs.ips.neo4j.procedure

import org.junit.Assert
import org.neo4j.driver.internal.value.NullValue
import org.neo4j.driver.internal.value.StringValue
import org.neo4j.driver.v1.Driver
import org.neo4j.driver.v1.Record
import java.util.concurrent.TimeUnit

fun testResults(l1: List<Record>, l2: List<Map<String, Any?>>) {
    Assert.assertEquals(l1.size, l2.size)

    for ((index, record) in l1.withIndex()) {
        val r2 = l2[index]
        for (key in record.keys()) {
            when {
                record[key] is StringValue -> Assert.assertEquals("${record[key]}", "\"${r2[key]}\"")
                r2[key] == null -> Assert.assertTrue(record[key] is NullValue)
                else -> Assert.assertEquals("${record[key]}", "${r2[key]}")
            }
        }
    }

    for ((index, mapValue) in l2.withIndex()) {
        val r1 = l1[index]
        for (entry in mapValue) {
            when {
                r1[entry.key] is StringValue -> Assert.assertEquals("${r1[entry.key]}", "\"${entry.value}\"")
                entry.value == null -> Assert.assertTrue(r1[entry.key] is NullValue)
                else -> Assert.assertEquals("${r1[entry.key]}", "${entry.value}")
            }
        }
    }
}

fun testResultsUnordered(l1: List<Record>, l2: List<Map<String, Any?>>) {
    Assert.assertEquals(l1.size, l2.size)

    for (record in l1) {
        Assert.assertTrue(l2.any {
            return@any record.keys().none { key ->
                record[key].toString() != it[key].toString()
            }
        })
    }

    for (mapValue in l2) {
        Assert.assertTrue(l1.any {
            for (entry in mapValue) {
                if (it[entry.key].toString() != entry.value.toString()) {
                    return@any false
                }
            }
            return@any true
        })
    }
}

enum class Mode {
    NORMAL, SHARED, PARALLEL,
}

enum class Func {
    dualID, dualLabel, strongID, strongLabel, normal
}

fun run(driver: Driver, func: Func, mode: Mode, query: String, i: Int, r: Int) {
    driver.session().use({ session ->
        val result = if (func != Func.normal) {
            session.run("CALL simulation.$func(\"$query\", \"$mode\")")
        } else {
            session.run(query)
        }

        val number = result.list().size
        val sum = result.consume()
        val ra = sum.resultAvailableAfter(TimeUnit.MILLISECONDS)
        val rc = sum.resultConsumedAfter(TimeUnit.MILLISECONDS)
        print("$i, ")
        print("$func, ")
        print("$mode, ")
        print("$ra, ")
        print("$rc, ")
        print("${ra + rc}, ")
        println("$number")

        Assert.assertEquals(r, number)
    })
}

fun createTests(size: Int): Iterable<Array<Any>> {
    val list = ArrayList<Array<Any>>()
    for (i in 0 until size) {
        for (f in Func.values()) {
            Mode.values()
                    .filterNot { f == Func.normal && it != Mode.NORMAL }
                    .mapTo(list) { arrayOf(i, f, it) }
        }
    }

    return list
}


