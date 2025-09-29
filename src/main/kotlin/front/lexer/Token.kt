package front.lexer

import diagnostics.Span

/**
 * Holds the different kinds of tokens that can be produced by the lexer
 */
sealed class TokenKind {
    // KEYWORDS
    data object Fn : TokenKind()
    data object Var : TokenKind()
    data object Const : TokenKind()
    data object True : TokenKind()
    data object False : TokenKind()
    data object KwInt : TokenKind()
    data object KwFloat : TokenKind()
    data object KwBool : TokenKind()
    data object KwChar : TokenKind()
    data object KwString : TokenKind()
    data object If : TokenKind()
    data object Elif : TokenKind()
    data object Else : TokenKind()
    data object While : TokenKind()

    // IDENTIFIERS & LITERALS
    data object Identifier : TokenKind()
    data object IntLiteral : TokenKind()
    data object FloatLiteral : TokenKind()
    data object CharLiteral : TokenKind()
    data object StringLiteral : TokenKind()

    // SYMBOLS
    data object LParen : TokenKind()
    data object RParen : TokenKind()
    data object LBrace : TokenKind()
    data object RBrace : TokenKind()
    data object Colon : TokenKind()
    data object Comma : TokenKind()
    data object Equal : TokenKind()
    data object EqualEqual : TokenKind()
    data object Bang : TokenKind()
    data object BangEqual : TokenKind()
    data object PlusEqual : TokenKind()
    data object Dot : TokenKind()
    data object DotDot : TokenKind()
    data object Plus : TokenKind()
    data object Minus : TokenKind()
    data object Star : TokenKind()
    data object Caret : TokenKind()
    data object Slash : TokenKind()
    data object Percent : TokenKind()
    data object Lt : TokenKind()
    data object LtEq : TokenKind()
    data object Gt : TokenKind()
    data object GtEq : TokenKind()
    data object AndAnd : TokenKind()
    data object OrOr : TokenKind()

    // END
    data object EOF : TokenKind()

    override fun toString(): String = this::class.simpleName ?: super.toString()
}

/**
 * The Token class representing well, a token
 *
 * @param kind The kind of token
 * @param lexeme The actual text of the token
 * @param span The span of the token in the source code
 * @param intValue The integer value of the token, if applicable
 * @param floatValue The float value of the token, if applicable
 * @param charValue The character value of the token, if applicable
 * @param stringValue The string value of the token, if applicable
 */
data class Token(
    val kind: TokenKind,
    val lexeme: String,
    val span: Span,
    val intValue: Long? = null,
    val floatValue: Double? = null,
    val charValue: Int? = null,
    val stringValue: String? = null
)
