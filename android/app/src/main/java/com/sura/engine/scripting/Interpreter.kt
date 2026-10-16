package com.sura.engine.scripting

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Scoped variable storage with a parent chain (used for functions and blocks). */
class Environment(val parent: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) { values[name] = value }
    fun containsLocal(name: String): Boolean = values.containsKey(name)

    fun lookup(name: String): Any? {
        var env: Environment? = this
        while (env != null) {
            if (env.values.containsKey(name)) return env.values[name]
            env = env.parent
        }
        return null
    }

    fun has(name: String): Boolean {
        var env: Environment? = this
        while (env != null) {
            if (env.values.containsKey(name)) return true
            env = env.parent
        }
        return false
    }

    fun assign(name: String, value: Any?): Boolean {
        var env: Environment? = this
        while (env != null) {
            if (env.values.containsKey(name)) { env.values[name] = value; return true }
            env = env.parent
        }
        return false
    }

    fun snapshot(): Map<String, Any?> = values.toMap()
    fun names(): Set<String> = values.keys.toSet()
}

/** Signals used to unwind control flow inside the interpreter. */
private class ReturnSignal(val value: Any?) : RuntimeException("script-return", null, false, false)
private class BreakSignal : RuntimeException("script-break", null, false, false)
private class ContinueSignal : RuntimeException("script-continue", null, false, false)

/**
 * Tree walking interpreter for SuraScript. Fully offline, no reflection, safe to run per frame.
 * Numbers are doubles, strings are strings, booleans are booleans (classic dynamic scripting feel).
 */
class Interpreter {
    val globals = Environment(null)
    private val functions = mutableMapOf<String, Stmt.FuncDecl>()
    private val natives = mutableMapOf<String, (List<Any?>) -> Any?>()
    private val hostVariables = mutableMapOf<String, (() -> Any?)?>()
    private val hostSetters = mutableMapOf<String, (Any?) -> Unit>()
    var maxStepsPerCall = 2_000_000
    var printHandler: ((String) -> Unit)? = null
    private var steps = 0

    init { registerBuiltins() }

    // ------------------------------------------------------------- public API

    fun parse(source: String): ScriptProgram {
        val tokens = Lexer(source).tokenize()
        val program = Parser(tokens).parseProgram()
        return ScriptProgram(source, program.statements)
    }

    fun execute(source: String): ScriptProgram {
        steps = 0
        val program = parse(source)
        run(program.statements, globals)
        return program
    }

    fun run(program: ScriptProgram) { run(program.statements, globals) }

    fun callFunction(name: String, args: List<Any?> = emptyList()): Any? {
        val fn = functions[name] ?: throw ScriptException("function '$name' is not defined")
        steps = 0
        return invoke(fn, args)
    }

    fun hasFunction(name: String): Boolean = functions.containsKey(name)
    fun functionNames(): Set<String> = functions.keys.toSet()

    fun setGlobal(name: String, value: Any?) { globals.define(name, value) }
    fun getGlobal(name: String): Any? = globals.lookup(name)
    fun globalsSnapshot(): Map<String, Any?> = globals.snapshot()

    /** Expose an engine value to scripts (read-only unless a setter is supplied). */
    fun bindVariable(name: String, getter: () -> Any?, setter: ((Any?) -> Unit)? = null) {
        hostVariables[name] = getter
        if (setter != null) hostSetters[name] = setter
    }

    fun registerNative(name: String, fn: (List<Any?>) -> Any?) { natives[name] = fn }
    fun removeNative(name: String) { natives.remove(name) }

    // ------------------------------------------------------------- execution

    private fun run(statements: List<Stmt>, env: Environment) {
        for (stmt in statements) executeStatement(stmt, env)
    }

    private fun executeStatement(stmt: Stmt, env: Environment) {
        bump()
        when (stmt) {
            is Stmt.ExprStmt -> evaluate(stmt.expr, env)
            is Stmt.VarDecl -> {
                val value = stmt.init?.let { evaluate(it, env) }
                env.define(stmt.name, value)
            }
            is Stmt.Block -> run(stmt.statements, Environment(env))
            is Stmt.If -> {
                if (truthy(evaluate(stmt.condition, env))) executeStatement(stmt.thenBranch, env)
                else stmt.elseBranch?.let { executeStatement(it, env) }
            }
            is Stmt.While -> {
                while (truthy(evaluate(stmt.condition, env))) {
                    bump()
                    try { executeStatement(stmt.body, env) }
                    catch (b: BreakSignal) { break }
                    catch (c: ContinueSignal) { continue }
                }
            }
            is Stmt.ForRange -> {
                val from = toNumber(evaluate(stmt.from, env)).toInt()
                val to = toNumber(evaluate(stmt.to, env)).toInt()
                var i = from
                while (i <= to) {
                    bump()
                    env.define(stmt.variable, i.toDouble())
                    try { executeStatement(stmt.body, env) }
                    catch (b: BreakSignal) { break }
                    catch (c: ContinueSignal) { i++; continue }
                    i++
                }
            }
            is Stmt.FuncDecl -> functions[stmt.name] = stmt
            is Stmt.Return -> throw ReturnSignal(stmt.value?.let { evaluate(it, env) })
            is Stmt.Break -> throw BreakSignal()
            is Stmt.Continue -> throw ContinueSignal()
        }
    }

    private fun invoke(fn: Stmt.FuncDecl, args: List<Any?>): Any? {
        val env = Environment(globals)
        for ((index, param) in fn.params.withIndex()) {
            env.define(param, args.getOrNull(index))
        }
        return try {
            run(fn.body, env)
            null
        } catch (r: ReturnSignal) {
            r.value
        }
    }

    private fun evaluate(expr: Expr, env: Environment): Any? {
        bump()
        return when (expr) {
            is Expr.Num -> expr.value
            is Expr.Str -> expr.value
            is Expr.Bool -> expr.value
            is Expr.NullLit -> null
            is Expr.Var -> resolveVariable(expr.name, env)
            is Expr.Bin -> binary(expr.op, evaluate(expr.left, env), evaluate(expr.right, env))
            is Expr.Logical -> {
                val left = evaluate(expr.left, env)
                if (expr.op == "and" || expr.op == "&&") { if (!truthy(left)) false else truthy(evaluate(expr.right, env)) }
                else { if (truthy(left)) true else truthy(evaluate(expr.right, env)) }
            }
            is Expr.Un -> {
                val value = evaluate(expr.operand, env)
                when (expr.op) {
                    "-" -> -toNumber(value)
                    "+" -> toNumber(value)
                    "!", "not" -> !truthy(value)
                    else -> throw ScriptException("unknown unary operator '${expr.op}'")
                }
            }
            is Expr.Assign -> assign(expr, env)
            is Expr.Call -> call(expr, env)
            is Expr.Member -> memberAccess(evaluate(expr.target, env), expr.name)
            is Expr.Index -> indexAccess(evaluate(expr.target, env), evaluate(expr.index, env))
            is Expr.Ternary -> if (truthy(evaluate(expr.cond, env))) evaluate(expr.ifTrue, env) else evaluate(expr.ifFalse, env)
        }
    }

    private fun resolveVariable(name: String, env: Environment): Any? {
        if (env.has(name)) return env.lookup(name)
        hostVariables[name]?.let { return it() }
        return env.lookup(name)
    }

    private fun assign(expr: Expr.Assign, env: Environment): Any? {
        val value = evaluate(expr.value, env)
        val newValue: Any? = if (expr.op == "=") value else {
            val current = currentValueOf(expr.target, env)
            binary(expr.op.substring(0, expr.op.length - 1), current, value)
        }
        when (val target = expr.target) {
            is Expr.Var -> {
                val assigned = env.assign(target.name, newValue)
                if (!assigned) {
                    if (hostSetters.containsKey(target.name)) hostSetters[target.name]?.invoke(newValue)
                    else env.define(target.name, newValue)
                }
            }
            is Expr.Member -> setMember(evaluate(target.target, env), target.name, newValue)
            is Expr.Index -> setIndex(evaluate(target.target, env), evaluate(target.index, env), newValue)
            else -> throw ScriptException("invalid assignment target")
        }
        return newValue
    }

    private fun currentValueOf(target: Expr, env: Environment): Any? = when (target) {
        is Expr.Var -> resolveVariable(target.name, env)
        is Expr.Member -> memberAccess(evaluate(target.target, env), target.name)
        is Expr.Index -> indexAccess(evaluate(target.target, env), evaluate(target.index, env))
        else -> null
    }

    private fun call(expr: Expr.Call, env: Environment): Any? {
        if (expr.callee is Expr.Var) {
            val name = expr.callee.name
            val args = expr.args.map { evaluate(it, env) }
            natives[name]?.let { return it(args) }
            functions[name]?.let { return invoke(it, args) }
            throw ScriptException("call to undefined function '$name'")
        }
        if (expr.callee is Expr.Member) {
            val target = evaluate(expr.callee.target, env)
            val args = expr.args.map { evaluate(it, env) }
            return callMethod(target, expr.callee.name, args)
        }
        throw ScriptException("unsupported call expression")
    }

    private fun callMethod(target: Any?, name: String, args: List<Any?>): Any? {
        when (name) {
            "length", "size" -> return when (target) {
                is String -> target.length.toDouble()
                is List<*> -> target.size.toDouble()
                else -> 0.0
            }
            "toString" -> return valueToString(target)
            "toInt" -> return toNumber(target).toInt().toDouble()
            "toFloat" -> return toNumber(target)
            "upper", "toUpper" -> return (target as? String)?.uppercase() ?: ""
            "lower", "toLower" -> return (target as? String)?.lowercase() ?: ""
            "trim" -> return (target as? String)?.trim() ?: ""
            "split" -> {
                val s = target as? String ?: return emptyList<Any?>()
                val sep = args.firstOrNull()?.let { valueToString(it) } ?: ","
                return s.split(sep)
            }
            "contains" -> return (target as? String)?.contains(valueToString(args.firstOrNull())) ?: false
            "substring" -> {
                val s = target as? String ?: return ""
                val start = toNumber(args.getOrNull(0)).toInt()
                val end = if (args.size > 1) toNumber(args[1]).toInt() else s.length
                return s.substring(start.coerceIn(0, s.length), end.coerceIn(0, s.length))
            }
            "abs" -> return abs(toNumber(target))
            "floor" -> return floor(toNumber(target))
            "ceil" -> return ceil(toNumber(target))
            "round" -> return kotlin.math.round(toNumber(target))
            "distance" -> {
                val other = args.getOrNull(0)
                return kotlin.math.hypot(toNumber(target), toNumber(other))
            }
        }
        throw ScriptException("unknown method '$name' on ${typeName(target)}")
    }

    private fun memberAccess(target: Any?, name: String): Any? {
        when (target) {
            is Map<*, *> -> return target[name]
            is String -> when (name) {
                "length", "size" -> return target.length.toDouble()
                "upper" -> return target.uppercase()
                "lower" -> return target.lowercase()
            }
            is List<*> -> when (name) {
                "length", "size" -> return target.size.toDouble()
                "first" -> return target.firstOrNull()
                "last" -> return target.lastOrNull()
            }
            null -> return null
        }
        if (name == "length" || name == "size") return 0.0
        return null
    }

    @Suppress("UNCHECKED_CAST")
    private fun setMember(target: Any?, name: String, value: Any?) {
        if (target is MutableMap<*, *>) {
            (target as MutableMap<String, Any?>)[name] = value
            return
        }
        throw ScriptException("object of type ${typeName(target)} has no writable property '$name'")
    }

    @Suppress("UNCHECKED_CAST")
    private fun indexAccess(target: Any?, index: Any?): Any? {
        return when (target) {
            is List<*> -> {
                val i = toNumber(index).toInt()
                if (i < 0 || i >= target.size) null else target[i]
            }
            is Map<*, *> -> target[valueToString(index)]
            is String -> {
                val i = toNumber(index).toInt()
                if (i < 0 || i >= target.length) "" else target[i].toString()
            }
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setIndex(target: Any?, index: Any?, value: Any?) {
        when (target) {
            is MutableList<*> -> {
                val i = toNumber(index).toInt()
                if (i >= 0 && i < target.size) (target as MutableList<Any?>)[i] = value
            }
            is MutableMap<*, *> -> (target as MutableMap<String, Any?>)[valueToString(index)] = value
            else -> throw ScriptException("cannot index ${typeName(target)}")
        }
    }

    // ------------------------------------------------------------- operators

    private fun binary(op: String, leftRaw: Any?, rightRaw: Any?): Any? {
        if (op == "+") {
            if (leftRaw is String || rightRaw is String) return valueToString(leftRaw) + valueToString(rightRaw)
            if (leftRaw is List<*> && rightRaw is List<*>) return leftRaw + rightRaw
        }
        val left = toNumber(leftRaw)
        val right = toNumber(rightRaw)
        return when (op) {
            "+" -> left + right
            "-" -> left - right
            "*" -> left * right
            "/" -> if (right == 0.0) 0.0 else left / right
            "%" -> if (right == 0.0) 0.0 else left % right
            "^" -> left.pow(right)
            "==" -> valuesEqual(leftRaw, rightRaw)
            "!=" -> !valuesEqual(leftRaw, rightRaw)
            "<" -> left < right
            "<=" -> left <= right
            ">" -> left > right
            ">=" -> left >= right
            else -> throw ScriptException("unknown operator '$op'")
        }
    }

    private fun valuesEqual(a: Any?, b: Any?): Boolean {
        if (a == null || b == null) return a == null && b == null
        if (a is String || b is String) return valueToString(a) == valueToString(b)
        if (a is Boolean || b is Boolean) return truthy(a) == truthy(b) && (a is Boolean && b is Boolean)
        return toNumber(a) == toNumber(b)
    }

    // ------------------------------------------------------------- conversions

    fun truthy(value: Any?): Boolean = when (value) {
        null -> false
        is Boolean -> value
        is Double -> value != 0.0
        is Int -> value != 0
        is Float -> value != 0f
        is String -> value.isNotEmpty() && value != "false"
        is List<*> -> value.isNotEmpty()
        else -> true
    }

    fun toNumber(value: Any?): Double = when (value) {
        null -> 0.0
        is Double -> value
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is Float -> value.toDouble()
        is Boolean -> if (value) 1.0 else 0.0
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    fun valueToString(value: Any?): String = when (value) {
        null -> "null"
        is Double -> if (value == floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()
        is Float -> value.toString()
        is Boolean -> if (value) "true" else "false"
        else -> value.toString()
    }

    fun typeName(value: Any?): String = when (value) {
        null -> "null"
        is Double, is Int, is Long, is Float -> "number"
        is String -> "string"
        is Boolean -> "bool"
        is List<*> -> "list"
        is Map<*, *> -> "map"
        else -> value::class.java.simpleName
    }

    private fun bump() {
        steps++
        if (steps > maxStepsPerCall) {
            steps = 0
            throw ScriptException("script exceeded step budget ($maxStepsPerCall) - possible infinite loop")
        }
    }

    fun resetStepBudget() { steps = 0 }

    // ------------------------------------------------------------- built-ins

    private fun registerBuiltins() {
        natives["print"] = { args ->
            val text = args.joinToString(" ") { valueToString(it) }
            printHandler?.invoke(text)
            null
        }
        natives["sqrt"] = { abs(sqrt(toNumber(it.getOrNull(0)))) }
        natives["abs"] = { abs(toNumber(it.getOrNull(0))) }
        natives["floor"] = { floor(toNumber(it.getOrNull(0))) }
        natives["ceil"] = { ceil(toNumber(it.getOrNull(0))) }
        natives["round"] = { kotlin.math.round(toNumber(it.getOrNull(0))) }
        natives["sin"] = { sin(toNumber(it.getOrNull(0))) }
        natives["cos"] = { cos(toNumber(it.getOrNull(0))) }
        natives["min"] = { min(toNumber(it.getOrNull(0)), toNumber(it.getOrNull(1))) }
        natives["max"] = { max(toNumber(it.getOrNull(0)), toNumber(it.getOrNull(1))) }
        natives["clamp"] = { v -> min(max(toNumber(v.getOrNull(0)), toNumber(v.getOrNull(1))), toNumber(v.getOrNull(2))) }
        natives["lerp"] = { v ->
            val a = toNumber(v.getOrNull(0)); val b = toNumber(v.getOrNull(1)); val t = toNumber(v.getOrNull(2))
            a + (b - a) * t
        }
        natives["str"] = { valueToString(it.getOrNull(0)) }
        natives["num"] = { toNumber(it.getOrNull(0)) }
        natives["len"] = { value ->
            when (val v = value.getOrNull(0)) {
                is String -> v.length.toDouble()
                is List<*> -> v.size.toDouble()
                is Map<*, *> -> v.size.toDouble()
                else -> 0.0
            }
        }
        natives["keys"] = { value ->
            val map = value.getOrNull(0)
            if (map is Map<*, *>) map.keys.toList() else emptyList<Any?>()
        }
        natives["type"] = { typeName(it.getOrNull(0)) }
        natives["print"]?.let { printer -> natives["println"] = printer }
    }
}

/** Facade that keeps a script plus its interpreter, with hot reload support. */
class ScriptInstance(val name: String, source: String) {
    val interpreter = Interpreter()
    var source: String = source
        private set
    var program: ScriptProgram? = null
        private set
    var lastError: String? = null
        private set
    val hasError: Boolean get() = lastError != null

    fun load(): Boolean {
        return try {
            val parsed = interpreter.parse(source)
            interpreter.run(parsed)
            program = parsed
            lastError = null
            true
        } catch (e: ScriptException) {
            lastError = e.message
            false
        } catch (t: Throwable) {
            lastError = t.message ?: t.toString()
            false
        }
    }

    fun call(function: String, args: List<Any?> = emptyList()): Any? {
        if (!interpreter.hasFunction(function)) return null
        return try {
            interpreter.callFunction(function, args)
        } catch (e: ScriptException) {
            lastError = e.message
            null
        } catch (t: Throwable) {
            lastError = t.message ?: t.toString()
            null
        }
    }

    /** Swap in new source text without recreating the instance (live reload). */
    fun reload(newSource: String): Boolean {
        source = newSource
        return load()
    }

    fun callSafe(function: String, args: List<Any?> = emptyList()): Any? =
        if (interpreter.hasFunction(function)) call(function, args) else null
}
