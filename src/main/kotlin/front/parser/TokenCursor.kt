package front.parser

import diagnostics.CoalError
import diagnostics.Diagnostic
import diagnostics.ErrorCode
import diagnostics.Severity
import front.lexer.Token
import front.lexer.TokenKind

/**
 * Provides the "cursor" functionality for parsing tokens, this is basically the iterator over the token list
 *
 * @param tokens The list of tokens to parse
 * @param fileName The name of the file being parsed (for error reporting)
 */
internal class TokenCursor(
    private val tokens: List<Token>,
    private val fileName: String
) {
    var i = 0
        private set

    /** Returns the current token without advancing the cursor */
    fun peek(): Token = tokens[i]
    /** Returns the token n positions ahead without advancing the cursor */
    fun peek(n: Int): Token = tokens[(i + n).coerceAtMost(tokens.lastIndex)]
    /** Advances the cursor and returns the current token */
    fun advance(): Token = tokens[i++]
    /** Advances the cursor by n positions and returns the current token */
    fun check(kind: TokenKind): Boolean = peek().kind::class == kind::class
    /** If the current token matches the given kind, advances the cursor and returns true, otherwise returns false */
    fun match(kind: TokenKind): Boolean = if(check(kind)) { advance(); true } else false
    /** Returns true if the cursor has reached the end of the token list */
    fun atEnd(): Boolean = check(TokenKind.EOF)

    /**
     * If the current token matches the given kind, advances the cursor and returns the token
     *
     * @param kind The expected token kind
     * @param code The error code to use if the token does not match
     * @return The matched token
     * @throws CoalError if the current token does not match the expected kind
     */
    fun expect(kind: TokenKind, code: ErrorCode): Token {
        if(check(kind)) return advance()
        errorHere(code, listOf(kind.toString(), peek().kind.toString()))
    }

    /**
     * If the current token is an identifier, advances the cursor and returns the token
     *
     * @param code The error code to use if the token is not an identifier
     * @return The matched identifier token
     * @throws CoalError if the current token is not an identifier
     */
    fun expectIdent(code: ErrorCode): Token {
        if(check(TokenKind.Identifier)) return advance()
        errorHere(code, listOf("Identifier", peek().kind.toString()))
    }

    /**
     * Throws a CoalError at the current token's position with the given error code and arguments
     *
     * @param code The error code for the diagnostic
     * @param args The arguments for the diagnostic message
     */
    fun errorHere(code: ErrorCode, args: List<String> = emptyList()): Nothing {
        val t = peek()
        throw CoalError(Diagnostic(Severity.ERROR, code, fileName, t.span, args))
    }

    /**
     * Looks ahead to see if the next tokens form an assignment operation (e.g., identifier followed by '=' or '+=')
     *
     * @return true if the next tokens form an assignment operation, false otherwise
     */
    fun lookaheadIsAssignOp(): Boolean {
        if(!check(TokenKind.Identifier)) return false
        val saved = i
        advance()
        val result = check(TokenKind.Equal) || check(TokenKind.PlusEqual)
        i = saved

        return result
    }
}