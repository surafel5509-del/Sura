package com.sura.engine.scripting

/** Recursive descent parser with precedence climbing for binary operators. */
class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parseProgram(): ScriptProgram {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            skipSeparators()
            if (isAtEnd()) break
            statements.add(declaration())
        }
        return ScriptProgram("", statements)
    }

    // ------------------------------------------------------------ statements

    private fun declaration(): Stmt {
        return when {
            checkKeyword("function") -> functionDeclaration()
            checkKeyword("var") || checkKeyword("let") -> variableDeclaration()
            else -> statement()
        }
    }

    private fun functionDeclaration(): Stmt {
        consumeKeyword("function")
        val name = consumeType(TokenType.IDENT, "expect function name").text
        consumeType(TokenType.LPAREN, "expect '(' after function name")
        val params = mutableListOf<String>()
        if (!check(TokenType.RPAREN)) {
            do {
                params.add(consumeType(TokenType.IDENT, "expect parameter name").text)
            } while (matchType(TokenType.COMMA))
        }
        consumeType(TokenType.RPAREN, "expect ')' after parameters")
        consumeType(TokenType.LBRACE, "expect '{' before function body")
        val body = blockBody()
        return Stmt.FuncDecl(name, params, body)
    }

    private fun variableDeclaration(): Stmt {
        advance()
        val name = consumeType(TokenType.IDENT, "expect variable name").text
        var init: Expr? = null
        if (matchOp("=")) init = expression()
        matchType(TokenType.SEMI)
        return Stmt.VarDecl(name, init)
    }

    private fun statement(): Stmt {
        return when {
            matchType(TokenType.LBRACE) -> Stmt.Block(blockBody())
            checkKeyword("if") -> ifStatement()
            checkKeyword("while") -> whileStatement()
            checkKeyword("for") -> forStatement()
            checkKeyword("return") -> returnStatement()
            checkKeyword("break") -> { advance(); matchType(TokenType.SEMI); Stmt.Break }
            checkKeyword("continue") -> { advance(); matchType(TokenType.SEMI); Stmt.Continue }
            else -> expressionStatement()
        }
    }

    private fun ifStatement(): Stmt {
        consumeKeyword("if")
        consumeType(TokenType.LPAREN, "expect '(' after if")
        val condition = expression()
        consumeType(TokenType.RPAREN, "expect ')' after condition")
        val thenBranch = statement()
        var elseBranch: Stmt? = null
        skipSeparators()
        if (checkKeyword("else")) {
            advance()
            elseBranch = statement()
        }
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun whileStatement(): Stmt {
        consumeKeyword("while")
        consumeType(TokenType.LPAREN, "expect '(' after while")
        val condition = expression()
        consumeType(TokenType.RPAREN, "expect ')' after condition")
        return Stmt.While(condition, statement())
    }

    private fun forStatement(): Stmt {
        consumeKeyword("for")
        consumeType(TokenType.LPAREN, "expect '(' after for")
        val variable = consumeType(TokenType.IDENT, "expect loop variable").text
        consumeKeyword("in")
        val from = expression()
        if (!matchOp("..") && !matchOp("..=") && !matchOp("...")) {
            throw ScriptException("expect '..' range operator in for loop", peek().line)
        }
        val to = expression()
        consumeType(TokenType.RPAREN, "expect ')' after range")
        return Stmt.ForRange(variable, from, to, statement())
    }

    private fun returnStatement(): Stmt {
        consumeKeyword("return")
        val value = if (check(TokenType.SEMI) || check(TokenType.RBRACE) || isAtEnd()) null else expression()
        matchType(TokenType.SEMI)
        return Stmt.Return(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        matchType(TokenType.SEMI)
        return Stmt.ExprStmt(expr)
    }

    private fun blockBody(): List<Stmt> {
        val body = mutableListOf<Stmt>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            skipSeparators()
            if (check(TokenType.RBRACE)) break
            body.add(declaration())
        }
        consumeType(TokenType.RBRACE, "expect '}' after block")
        return body
    }

    // ------------------------------------------------------------ expressions

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr {
        val expr = ternary()
        if (checkOp("=") || checkOp("+=") || checkOp("-=") || checkOp("*=") || checkOp("/=") || checkOp("%=")) {
            val op = advance().text
            val value = assignment()
            if (expr !is Expr.Var && expr !is Expr.Member && expr !is Expr.Index) {
                throw ScriptException("invalid assignment target", peek().line)
            }
            return Expr.Assign(expr, op, value)
        }
        return expr
    }

    private fun ternary(): Expr {
        val expr = or()
        if (matchOp("?")) {
            val ifTrue = expression()
            consumeOp(":", "expect ':' in conditional expression")
            val ifFalse = ternary()
            return Expr.Ternary(expr, ifTrue, ifFalse)
        }
        return expr
    }

    private fun or(): Expr {
        var expr = and()
        while (checkKeyword("or") || checkOp("||")) {
            val op = advance().text
            val right = and()
            expr = Expr.Logical(op, expr, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (checkKeyword("and") || checkOp("&&")) {
            val op = advance().text
            val right = equality()
            expr = Expr.Logical(op, expr, right)
        }
        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()
        while (checkOp("==") || checkOp("!=")) {
            val op = advance().text
            expr = Expr.Bin(op, expr, comparison())
        }
        return expr
    }

    private fun comparison(): Expr {
        var expr = term()
        while (checkOp("<") || checkOp("<=") || checkOp(">") || checkOp(">=")) {
            val op = advance().text
            expr = Expr.Bin(op, expr, term())
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()
        while (checkOp("+") || checkOp("-")) {
            val op = advance().text
            expr = Expr.Bin(op, expr, factor())
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()
        while (checkOp("*") || checkOp("/") || checkOp("%")) {
            val op = advance().text
            expr = Expr.Bin(op, expr, unary())
        }
        return expr
    }

    private fun unary(): Expr {
        if (checkOp("-") || checkOp("!") || checkKeyword("not") || checkOp("+")) {
            val op = advance().text
            return Expr.Un(op, unary())
        }
        return postfix()
    }

    private fun postfix(): Expr {
        var expr = primary()
        while (true) {
            when {
                matchType(TokenType.DOT) -> {
                    val name = consumeType(TokenType.IDENT, "expect property name after '.'").text
                    expr = Expr.Member(expr, name)
                }
                matchType(TokenType.LBRACKET) -> {
                    val index = expression()
                    consumeType(TokenType.RBRACKET, "expect ']' after index")
                    expr = Expr.Index(expr, index)
                }
                check(TokenType.LPAREN) -> {
                    advance()
                    val args = mutableListOf<Expr>()
                    if (!check(TokenType.RPAREN)) {
                        do { args.add(expression()) } while (matchType(TokenType.COMMA))
                    }
                    consumeType(TokenType.RPAREN, "expect ')' after arguments")
                    expr = Expr.Call(expr, args)
                }
                else -> return expr
            }
        }
    }

    private fun primary(): Expr {
        val token = peek()
        return when (token.type) {
            TokenType.NUMBER -> { advance(); Expr.Num(token.number) }
            TokenType.STRING -> { advance(); Expr.Str(token.text) }
            TokenType.KEYWORD -> when (token.text) {
                "true" -> { advance(); Expr.Bool(true) }
                "false" -> { advance(); Expr.Bool(false) }
                "null" -> { advance(); Expr.NullLit() }
                else -> throw ScriptException("unexpected keyword '${token.text}'", token.line)
            }
            TokenType.IDENT -> { advance(); Expr.Var(token.text) }
            TokenType.LPAREN -> {
                advance()
                val expr = expression()
                consumeType(TokenType.RPAREN, "expect ')' after expression")
                expr
            }
            else -> throw ScriptException("unexpected token '${token.text}'", token.line)
        }
    }

    // ------------------------------------------------------------ helpers

    private fun skipSeparators() { while (check(TokenType.SEMI)) advance() }
    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current.coerceAtMost(tokens.size - 1)]
    private fun advance(): Token { if (!isAtEnd()) current++; return tokens[(current - 1).coerceAtLeast(0)] }
    private fun check(type: TokenType): Boolean = peek().type == type
    private fun checkOp(op: String): Boolean = peek().type == TokenType.OP && peek().text == op
    private fun checkKeyword(word: String): Boolean = peek().type == TokenType.KEYWORD && peek().text == word

    private fun matchType(type: TokenType): Boolean { if (check(type)) { advance(); return true }; return false }
    private fun matchOp(op: String): Boolean { if (checkOp(op)) { advance(); return true }; return false }

    private fun consumeType(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        throw ScriptException("$message (found '${peek().text}')", peek().line)
    }

    private fun consumeKeyword(word: String) {
        if (checkKeyword(word)) { advance(); return }
        throw ScriptException("expect '$word' (found '${peek().text}')", peek().line)
    }

    private fun consumeOp(op: String, message: String) {
        if (matchOp(op)) return
        throw ScriptException("$message (found '${peek().text}')", peek().line)
    }
}
