package com.sura.engine.scripting

/** Abstract syntax tree for SuraScript. */
sealed class Expr {
    data class Num(val value: Double) : Expr()
    data class Str(val value: String) : Expr()
    data class Bool(val value: Boolean) : Expr()
    data class NullLit(val placeholder: Int = 0) : Expr()
    data class Var(val name: String) : Expr()
    data class Bin(val op: String, val left: Expr, val right: Expr) : Expr()
    data class Logical(val op: String, val left: Expr, val right: Expr) : Expr()
    data class Un(val op: String, val operand: Expr) : Expr()
    data class Assign(val target: Expr, val op: String, val value: Expr) : Expr()
    data class Call(val callee: Expr, val args: List<Expr>) : Expr()
    data class Member(val target: Expr, val name: String) : Expr()
    data class Index(val target: Expr, val index: Expr) : Expr()
    data class Ternary(val cond: Expr, val ifTrue: Expr, val ifFalse: Expr) : Expr()
}

sealed class Stmt {
    data class ExprStmt(val expr: Expr) : Stmt()
    data class VarDecl(val name: String, val init: Expr?) : Stmt()
    data class Block(val statements: List<Stmt>) : Stmt()
    data class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
    data class While(val condition: Expr, val body: Stmt) : Stmt()
    data class ForRange(val variable: String, val from: Expr, val to: Expr, val body: Stmt) : Stmt()
    data class FuncDecl(val name: String, val params: List<String>, val body: List<Stmt>) : Stmt()
    data class Return(val value: Expr?) : Stmt()
    object Break : Stmt()
    object Continue : Stmt()
}

/** A parsed and ready-to-run SuraScript program. */
class ScriptProgram(val source: String, val statements: List<Stmt>) {
    val functionNames: List<String> get() = statements.filterIsInstance<Stmt.FuncDecl>().map { it.name }
}
