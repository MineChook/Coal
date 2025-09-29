package front.parser

import diagnostics.CoalError
import diagnostics.Diagnostic
import diagnostics.ErrorCode
import diagnostics.Severity
import front.lexer.Token
import front.lexer.TokenKind

internal class TokenCursor(
    private val tokens: List<Token>,
    private val fileName: String
) {
    var i = 0
        private set

    fun peek(): Token = tokens[i]
    fun peek(n: Int): Token = tokens[(i + n).coerceAtMost(tokens.lastIndex)]
    fun advance(): Token = tokens[i++]
    fun check(kind: TokenKind): Boolean = peek().kind::class == kind::class
    fun match(kind: TokenKind): Boolean = if(check(kind)) { advance(); true } else false
    fun atEnd(): Boolean = check(TokenKind.EOF)

    fun expect(kind: TokenKind, code: ErrorCode): Token {
        if(check(kind)) return advance()
        errorHere(code, listOf(kind.toString(), peek().kind.toString()))
    }

    fun expectIdent(code: ErrorCode): Token {
        if(check(TokenKind.Identifier)) return advance()
        errorHere(code, listOf("Identifier", peek().kind.toString()))
    }

    fun errorHere(code: ErrorCode, args: List<String> = emptyList()): Nothing {
        val t = peek()
        throw CoalError(Diagnostic(Severity.ERROR, code, fileName, t.span, args))
    }

    fun lookaheadIsAssignOp(): Boolean {
        if(!check(TokenKind.Identifier)) return false
        val saved = i
        advance()
        val result = check(TokenKind.Equal) || check(TokenKind.PlusEqual)
        i = saved

        return result
    }
}