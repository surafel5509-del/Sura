package com.sura.engine

import com.sura.engine.scripting.Interpreter
import com.sura.engine.scripting.ScriptException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuraScriptTest {

    private fun run(code: String): Interpreter {
        val interpreter = Interpreter()
        interpreter.execute(code)
        return interpreter
    }

    @Test fun arithmeticAndPrecedence() {
        val interpreter = run("var a = 2 + 3 * 4\nvar b = (2 + 3) * 4")
        assertEquals(14.0, interpreter.getGlobal("a") as Double, 0.0001)
        assertEquals(20.0, interpreter.getGlobal("b") as Double, 0.0001)
    }

    @Test fun stringsConcatenate() {
        val interpreter = run("var greeting = \"hello\" + \" \" + \"engine\"")
        assertEquals("hello engine", interpreter.getGlobal("greeting"))
    }

    @Test fun conditionalsAndLoopsWork() {
        val interpreter = run("""
            var total = 0
            for i in 1..10 {
                if (i % 2 == 0) {
                    total = total + i
                }
            }
        """.trimIndent())
        assertEquals(30.0, interpreter.getGlobal("total") as Double, 0.0001)
    }

    @Test fun whileLoopWithBreak() {
        val interpreter = run("""
            var n = 0
            while (true) {
                n = n + 1
                if (n >= 5) { break }
            }
        """.trimIndent())
        assertEquals(5.0, interpreter.getGlobal("n") as Double, 0.0001)
    }

    @Test fun functionsAndRecursion() {
        val interpreter = run("""
            function factorial(n) {
                if (n <= 1) { return 1 }
                return n * factorial(n - 1)
            }
            var result = factorial(6)
        """.trimIndent())
        assertEquals(720.0, interpreter.getGlobal("result") as Double, 0.0001)
    }

    @Test fun nativeFunctionsAreCallable() {
        val interpreter = Interpreter()
        var printed = ""
        interpreter.printHandler = { printed += it }
        interpreter.registerNative("double") { args -> interpreter.toNumber(args.getOrNull(0)) * 2.0 }
        interpreter.execute("""
            var value = double(21)
            print("value=" + value)
        """.trimIndent())
        assertEquals(42.0, interpreter.getGlobal("value") as Double, 0.0001)
        assertEquals("value=42", printed)
    }

    @Test fun stepBudgetStopsInfiniteLoops() {
        val interpreter = Interpreter()
        interpreter.maxStepsPerCall = 5000
        var threw = false
        try {
            interpreter.execute("while (true) { }")
        } catch (e: ScriptException) {
            threw = true
        }
        assertTrue("infinite loop should be interrupted", threw)
    }

    @Test fun syntaxErrorsAreReported() {
        val interpreter = Interpreter()
        var message = ""
        try {
            interpreter.execute("function broken( { }")
        } catch (e: ScriptException) {
            message = e.message ?: ""
        }
        assertTrue(message.isNotEmpty())
    }

    @Test fun truthinessAndComparison() {
        val interpreter = run("""
            var a = 1 == 1
            var b = "x" == "x"
            var c = 5 > 3
            var d = not true
        """.trimIndent())
        assertEquals(true, interpreter.getGlobal("a"))
        assertEquals(true, interpreter.getGlobal("b"))
        assertEquals(true, interpreter.getGlobal("c"))
        assertEquals(false, interpreter.getGlobal("d"))
    }

    @Test fun memberAndIndexAccess() {
        val interpreter = run("""
            var text = "sura engine"
            var length = text.length
            var upper = text.upper()
            var first = text.substring(0, 4)
        """.trimIndent())
        assertEquals(11.0, interpreter.getGlobal("length") as Double, 0.0001)
        assertEquals("SURA ENGINE", interpreter.getGlobal("upper"))
        assertEquals("sura", interpreter.getGlobal("first"))
        assertFalse((interpreter.getGlobal("first") as String).isEmpty())
    }
}
