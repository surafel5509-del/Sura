package com.sura.engine.scripting

enum class TokenType {
    NUMBER, STRING, IDENT, KEYWORD, OP, LPAREN, RPAREN, LBRACE, RBRACE,
    LBRACKET, RBRACKET, COMMA, SEMI, DOT, EOF
}

data class Token(val type: TokenType, val text: String, val line: Int, val number: Double = 0.0)

class ScriptException(message: String, val line: Int = 0) : Exception(if (line > 0) "line $line: $message" else message)

/** Hand written lexer for SuraScript, the built-in gameplay scripting language. */
class Lexer(private val source: String) {
    private var pos = 0
    private var line = 1
    private val tokens = mutableListOf<Token>()

    companion object {
        val KEYWORDS = setOf(
            "var", "let", "if", "else", "while", "for", "in", "function", "return",
            "true", "false", "null", "break", "continue", "and", "or", "not"
        )
        private const val OPERATORS = "+-*/%=<>!&|^~?:"
    }

    fun tokenize(): List<Token> {
        while (pos < source.length) {
            val c = source[pos]
            when {
                c == '\n' -> { line++; pos++ }
                c == ' ' || c == '\t' || c == '\r' -> pos++
                c == '/' && pos + 1 < source.length && source[pos + 1] == '/' -> skipLineComment()
                c == '/' && pos + 1 < source.length && source[pos + 1] == '*' -> skipBlockComment()
                c.isDigit() || (c == '.' && pos + 1 < source.length && source[pos + 1].isDigit()) -> readNumber()
                c.isLetter() || c == '_' -> readIdent()
                c == '"' || c == '\'' -> readString(c)
                else -> readSymbol(c)
            }
        }
        tokens.add(Token(TokenType.EOF, "", line))
        return tokens
    }

    private fun skipLineComment() {
        while (pos < source.length && source[pos] != '\n') pos++
    }

    private fun skipBlockComment() {
        pos += 2
        while (pos + 1 < source.length && !(source[pos] == '*' && source[pos + 1] == '/')) {
            if (source[pos] == '\n') line++
            pos++
        }
        pos += 2
    }

    private fun readNumber() {
        val start = pos
        var seenDot = false
        while (pos < source.length) {
            val c = source[pos]
            if (c.isDigit()) { pos++; continue }
            if (c == '.' && !seenDot) { seenDot = true; pos++; continue }
            break
        }
        val text = source.substring(start, pos)
        val value = text.toDoubleOrNull() ?: throw ScriptException("invalid number '$text'", line)
        tokens.add(Token(TokenType.NUMBER, text, line, value))
    }

    private fun readIdent() {
        val start = pos
        while (pos < source.length && (source[pos].isLetterOrDigit() || source[pos] == '_')) pos++
        val text = source.substring(start, pos)
        tokens.add(Token(if (KEYWORDS.contains(text)) TokenType.KEYWORD else TokenType.IDENT, text, line))
    }

    private fun readString(quote: Char) {
        pos++
        val sb = StringBuilder()
        while (pos < source.length && source[pos] != quote) {
            val c = source[pos]
            if (c == '\\' && pos + 1 < source.length) {
                pos++
                when (val esc = source[pos]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    '\'' -> sb.append('\'')
                    else -> sb.append(esc)
                }
            } else {
                if (c == '\n') line++
                sb.append(c)
            }
            pos++
        }
        if (pos >= source.length) throw ScriptException("unterminated string", line)
        pos++
        tokens.add(Token(TokenType.STRING, sb.toString(), line))
    }

    private fun readSymbol(c: Char) {
        when (c) {
            '(' -> { tokens.add(Token(TokenType.LPAREN, "(", line)); pos++ }
            ')' -> { tokens.add(Token(TokenType.RPAREN, ")", line)); pos++ }
            '{' -> { tokens.add(Token(TokenType.LBRACE, "{", line)); pos++ }
            '}' -> { tokens.add(Token(TokenType.RBRACE, "}", line)); pos++ }
            '[' -> { tokens.add(Token(TokenType.LBRACKET, "[", line)); pos++ }
            ']' -> { tokens.add(Token(TokenType.RBRACKET, "]", line)); pos++ }
            ',' -> { tokens.add(Token(TokenType.COMMA, ",", line)); pos++ }
            ';' -> { tokens.add(Token(TokenType.SEMI, ";", line)); pos++ }
            '.' -> {
                if (pos + 1 < source.length && source[pos + 1] == '.') {
                    if (pos + 2 < source.length && source[pos + 2] == '=') {
                        tokens.add(Token(TokenType.OP, "..=", line)); pos += 3
                    } else {
                        tokens.add(Token(TokenType.OP, "..", line)); pos += 2
                    }
                } else {
                    tokens.add(Token(TokenType.DOT, ".", line)); pos++
                }
            }
            else -> {
                if (!OPERATORS.contains(c)) throw ScriptException("unexpected character '$c'", line)
                val two = if (pos + 1 < source.length) "${c}${source[pos + 1]}" else ""
                val three = if (pos + 2 < source.length) "$two${source[pos + 2]}" else ""
                when {
                    three == "..=" -> { tokens.add(Token(TokenType.OP, "..=", line)); pos += 3 }
                    three == "..." -> { tokens.add(Token(TokenType.OP, "...", line)); pos += 3 }
                    two == "==" || two == "!=" || two == "<=" || two == ">=" -> {
                        tokens.add(Token(TokenType.OP, two, line)); pos += 2
                    }
                    two == "&&" || two == "||" -> { tokens.add(Token(TokenType.OP, two, line)); pos += 2 }
                    two == "+=" || two == "-=" || two == "*=" || two == "/=" || two == "%=" -> {
                        tokens.add(Token(TokenType.OP, two, line)); pos += 2
                    }
                    else -> { tokens.add(Token(TokenType.OP, c.toString(), line)); pos++ }
                }
            }
        }
    }
}
