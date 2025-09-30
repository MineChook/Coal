package front.lexer

import diagnostics.CoalError
import diagnostics.Diagnostic
import diagnostics.ErrorCode
import diagnostics.Severity
import diagnostics.Span

/**
 * The heart of the lexer. Takes a source string and produces a list of tokens for the parser to consume
 *
 * @param source The source code to lex
 * @param fileName The name of the source file (for error reporting)
 */
class Lexer(
    private val source: String,
    private val fileName: String = "<stdin>"
) {
    private var i = 0
    private var line = 1
    private var col = 1
    private val tokens = mutableListOf<Token>()

    private val keywords = mapOf(
        "fn" to TokenKind.Fn,
        "var" to TokenKind.Var,
        "const" to TokenKind.Const,
        "true" to TokenKind.True,
        "false" to TokenKind.False,
        "int" to TokenKind.KwInt,
        "float" to TokenKind.KwFloat,
        "bool" to TokenKind.KwBool,
        "char" to TokenKind.KwChar,
        "string" to TokenKind.KwString,
        "if" to TokenKind.If,
        "elif" to TokenKind.Elif,
        "else" to TokenKind.Else,
        "while" to TokenKind.While
    )

    /**
     * Lex the source code into a list of tokens
     *
     * @return A list of tokens
     */
    fun lex(): List<Token> {
        while(true) {
            skipWhitespaceAndComments()
            if(isAtEnd()) break

            val start = i
            val sLine = line
            val sCol = col
            val c = advance()

            when {
                isAlpha(c) || c == '_' -> lexIdentOrKeyword(start, sLine, sCol)
                isDigit(c) -> lexNumber(start, sLine, sCol)
                c == '"' -> lexStringLiteral(start, sLine, sCol)
                c == '\'' -> lexChar(start, sLine, sCol)
                else -> lexSymbolOrError(c, start, sLine, sCol)
            }
        }

        tokens += Token(TokenKind.EOF, "", Span(i, i, line, col))
        return tokens
    }

    private fun skipWhitespaceAndComments() {
        while(!isAtEnd()) {
            val c = peek()
            when(c) {
                ' ', '\t', '\r', '\n' -> { advance() }
                '/' -> {
                    if(peek(1) == '/') while(!isAtEnd() && peek() != '\n') advance()
                    else return
                }

                else -> return
            }
        }
    }

    private fun lexIdentOrKeyword(start: Int, line0: Int, col0: Int) {
        while(isAlphaNum(peek())) advance()
        val end = i
        val text = source.substring(start, end)
        val kind = keywords[text] ?: TokenKind.Identifier

        tokens += Token(kind, text, Span(start, end, line0, col0))
    }

    private fun lexNumber(start: Int, line0: Int, col0: Int) {
        while(isDigit(peek()) || peek() == '_') advance()

        var isFloat = false
        if(peek() == '.' && isDigit(peek(1))) {
            isFloat = true
            advance()
            while(isDigit(peek()) || peek() == '_') advance()
        }

        val end = i
        val raw = source.substring(start, end)
        val clean = raw.replace("_", "")
        if(isFloat) {
            val v = clean.toDouble()
            tokens += Token(TokenKind.FloatLiteral, raw, Span(start, end, line0, col0), floatValue = v)
        } else {
            val v = clean.toLong()
            tokens += Token(TokenKind.IntLiteral, raw, Span(start, end, line0, col0), intValue = v)
        }
    }

    private fun lexStringLiteral(start: Int, line0: Int, col0: Int) {
        val sb = StringBuilder()
        var terminated = false

        while(!isAtEnd()) {
            val c = advance()
            when(c) {
                '"' -> {
                    terminated = true
                    break
                }

                '\\' -> {
                    val esc = advanceOrError(ErrorCode.UnterminatedString, line0, col0)
                    sb.append(when(esc) {
                        '"' -> '"'
                        '\\'-> '\\'
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        else -> lexError(ErrorCode.UnknownEscapeSequence, start, line0, col0)
                    })
                }

                '\n' -> lexError(ErrorCode.UnterminatedString, start, line0, col0)
                else -> sb.append(c)
            }
        }

        if(!terminated) lexError(ErrorCode.UnterminatedString, start, line0, col0)
        val end = i
        val raw = source.substring(start, end)

        tokens += Token(TokenKind.StringLiteral, raw, Span(start, end, line0, col0), stringValue = sb.toString())
    }

    private fun lexChar(start: Int, line0: Int, col0: Int) {
        if(isAtEnd()) lexError(ErrorCode.UnterminatedChar, start, line0, col0)
        val c = advance()
        val value = when(c) {
            '\\' -> {
                val esc = advanceOrError(ErrorCode.UnterminatedChar, line0, col0)
                when (esc) {
                    '\'' -> '\''
                    '\\' -> '\\'
                    'n' -> '\n'
                    't' -> '\t'
                    'r' -> '\r'
                    else -> lexError(ErrorCode.UnknownEscapeSequence, start, line0, col0)
                }
            }

            '\'' -> lexError(ErrorCode.EmptyCharLiteral, start, line0, col0)
            '\n' -> lexError(ErrorCode.UnterminatedChar, start, line0, col0)
            else -> c
        }

        if(!match('\'')) lexError(ErrorCode.UnterminatedChar, start, line0, col0)
        val end = i
        val raw = source.substring(start, end)

        tokens += Token(TokenKind.CharLiteral, raw, Span(start, end, line0, col0), charValue = value.code)
    }

    private fun lexSymbolOrError(c: Char, start: Int, line0: Int, col0: Int) {
        when(c) {
            '(' -> add(TokenKind.LParen, start, line0, col0)
            ')' -> add(TokenKind.RParen, start, line0, col0)
            '{' -> add(TokenKind.LBrace, start, line0, col0)
            '}' -> add(TokenKind.RBrace, start, line0, col0)
            ':' -> add(TokenKind.Colon, start, line0, col0)
            ',' -> add(TokenKind.Comma, start, line0, col0)
            '+' -> {
                if(peek() == '=') {
                    advance()
                    add(TokenKind.PlusEqual, start, line0, col0)
                } else {
                    add(TokenKind.Plus, start, line0, col0)
                }
            }
            '-' -> add(TokenKind.Minus, start, line0, col0)
            '*' -> add(TokenKind.Star, start, line0, col0)
            '^' -> add(TokenKind.Caret, start, line0, col0)
            '/' -> add(TokenKind.Slash, start, line0, col0)
            '%' -> add(TokenKind.Percent, start, line0, col0)
            '=' -> {
                if(peek() == '=') {
                    advance()
                    add(TokenKind.EqualEqual, start, line0, col0)
                } else {
                    add(TokenKind.Equal, start, line0, col0)
                }
            }

            '!' -> {
                if(peek() == '=') {
                    advance()
                    add(TokenKind.BangEqual, start, line0, col0)
                } else {
                    add(TokenKind.Bang, start, line0, col0)
                }
            }

            '<' -> {
                if(peek() == '=') {
                    advance()
                    add(TokenKind.LtEq, start, line0, col0)
                } else {
                    add(TokenKind.Lt, start, line0, col0)
                }
            }

            '>' -> {
                if(peek() == '=') {
                    advance()
                    add(TokenKind.GtEq, start, line0, col0)
                } else {
                    add(TokenKind.Gt, start, line0, col0)
                }
            }

            '&' -> {
                if(peek() == '&') {
                    advance()
                    add(TokenKind.AndAnd, start, line0, col0)
                } else {
                    lexError(ErrorCode.UnexpectedChar, start, line0, col0)
                }
            }

            '|' -> {
                if(peek() == '|') {
                    advance()
                    add(TokenKind.OrOr, start, line0, col0)
                } else {
                    lexError(ErrorCode.UnexpectedChar, start, line0, col0)
                }
            }

            ';' -> { /* ignore semicolons */ }
            '.' -> {
                if(peek() == '.') {
                    advance()
                    add(TokenKind.DotDot, start, line0, col0)
                } else {
                    add(TokenKind.Dot, start, line0, col0)
                }
            }

            else -> lexError(ErrorCode.UnexpectedChar, start, line0, col0)
        }
    }

    private fun add(kind: TokenKind, start: Int, line0: Int, col0: Int) {
        tokens += Token(kind, source.substring(start, i), Span(start, i, line0, col0))
    }

    // Utility functions
    private fun isAtEnd() = i >= source.length
    private fun peek(offset: Int = 0): Char = if(i + offset < source.length) source[i + offset] else '\u0000'

    private fun advance(): Char {
        val ch = source[i++]
        if(ch == '\n') {
            line++
            col = 1
        } else col++

        return ch
    }

    private fun match(expected: Char): Boolean {
        if(isAtEnd() || source[i] != expected) return false
        advance()
        return true
    }

    private fun isAlpha(c: Char) = (c in 'A'..'Z') || (c in 'a'..'z')
    private fun isDigit(c: Char) = c in '0'..'9'
    private fun isAlphaNum(c: Char) = isAlpha(c) || isDigit(c) || c == '_'

    private fun advanceOrError(error: ErrorCode, line0: Int, col0: Int): Char {
        if(isAtEnd()) lexError(error, i, line0, col0)
        return advance()
    }

    private fun lexError(error: ErrorCode, startIndex: Int, line0: Int, col0: Int): Nothing {
        throw CoalError(Diagnostic(Severity.ERROR, error, fileName, Span(startIndex, startIndex, line0, col0), listOf(error.template)))
    }
 }
