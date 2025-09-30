package front.parser

import ast.Assign
import ast.BinOp
import ast.Binary
import ast.Block
import ast.BoolLit
import ast.Call
import ast.CharLit
import ast.Decl
import ast.Expr
import ast.ExprStmt
import ast.FloatLit
import ast.FnDecl
import ast.Ident
import ast.IfBranch
import ast.IfStmt
import ast.IntLit
import ast.MethodCall
import ast.NamedType
import ast.Program
import ast.Stmt
import ast.StringLit
import ast.TypeRef
import ast.UnOp
import ast.Unary
import ast.VarDecl
import ast.WhileStmt
import diagnostics.CoalError
import diagnostics.Diagnostic
import diagnostics.ErrorCode
import diagnostics.Severity
import diagnostics.Span
import front.lexer.Token
import front.lexer.TokenKind
import kotlin.collections.plusAssign

class Parser(
    private val sourceText: String,
    private val tokens: List<Token>,
    private val fileName: String = "<stdin>"
) {
    private val cur = TokenCursor(tokens, fileName)

    fun parseProgram(): Program {
        val decls = mutableListOf<Decl>()
        while(!cur.atEnd()) decls += parseFnDecl()
        return Program(decls)
    }

    private fun parseFnDecl(): FnDecl {
        val fnTok = cur.expect(TokenKind.Fn, ErrorCode.ExpectedToken)
        val nameTok = cur.expectIdent(ErrorCode.ExpectedToken)
        cur.expect(TokenKind.LParen, ErrorCode.ExpectedToken)
        //TODO: Expand later for parameters
        cur.expect(TokenKind.RParen, ErrorCode.ExpectedToken)
        val body = parseBlock()

        return FnDecl(nameTok.lexeme, emptyList(), null, body, Span.merge(fnTok.span, body.span))
    }

    private fun parseBlock(): Block {
        val l = cur.expect(TokenKind.LBrace, ErrorCode.ExpectedToken)
        val stmts = mutableListOf<Stmt>()
        while(!cur.check(TokenKind.RBrace) && !cur.atEnd()) stmts += parseStmt()
        val r = cur.expect(TokenKind.RBrace, ErrorCode.ExpectedToken)

        return Block(stmts, Span.merge(l.span, r.span))
    }

    private fun parseStmt(): Stmt {
        return when {
            cur.check(TokenKind.Var) || cur.check(TokenKind.Const) -> parseVarDecl()
            cur.check(TokenKind.If) -> parseIfStmt()
            cur.check(TokenKind.While) -> parseWhileStmt()
            cur.check(TokenKind.Identifier) -> {
                if(cur.lookaheadIsAssignOp()) parseAssignStmt()
                else {
                    val e = parseExpr()
                    ExprStmt(e, e.span)
                }
            }

            else -> {
                val e = parseExpr()
                ExprStmt(e, e.span)
            }
        }
    }

    private fun parseVarDecl(): VarDecl {
        val isConst = cur.match(TokenKind.Const).also {
            if(!it) cur.expect(TokenKind.Var, ErrorCode.ExpectedToken)
        }

        val nameTok = cur.expectIdent(ErrorCode.ExpectedToken)
        var annotated: TypeRef? = null
        if(cur.match(TokenKind.Colon)) annotated = parseTypeRef()

        var init: Expr? = null
        val eqTok = if(cur.match(TokenKind.Equal)) tokens[(cur.i - 1).coerceAtLeast(0)] else null
        if(eqTok != null) init = parseExpr()

        val start = (annotated?.span ?: nameTok.span)
        val end = (init?.span ?: nameTok.span)
        val sp = Span.merge(start, end)

        return VarDecl(nameTok.lexeme, annotated, init, isConst, sp)
    }

    private fun parseAssignStmt(): Stmt {
        val nameTok = cur.expectIdent(ErrorCode.ExpectedToken)
        val start = nameTok.span

        if(cur.match(TokenKind.PlusEqual)) {
            val rhs = parseExpr()
            val bin = Binary(BinOp.Add, Ident(nameTok.lexeme, nameTok.span), rhs, Span.merge(start, rhs.span))
            return Assign(nameTok.lexeme, bin, Span.merge(start, rhs.span))
        }

        cur.expect(TokenKind.Equal, ErrorCode.ExpectedToken)
        val value = parseExpr()
        return Assign(nameTok.lexeme, value, Span.merge(start, value.span))
    }

    private fun parseIfStmt(): Stmt {
        val ifTok = cur.expect(TokenKind.If, ErrorCode.ExpectedToken)
        cur.expect(TokenKind.LParen, ErrorCode.ExpectedToken)
        val cond0 = parseExpr()
        cur.expect(TokenKind.RParen, ErrorCode.ExpectedToken)
        val then0 = parseBlock()
        val branches = mutableListOf(IfBranch(cond0, then0, Span.merge(cond0.span, then0.span)))

        while(cur.match(TokenKind.Elif)) {
            cur.expect(TokenKind.LParen, ErrorCode.ExpectedToken)
            val c = parseExpr()
            cur.expect(TokenKind.RParen, ErrorCode.ExpectedToken)
            val b = parseBlock()
            branches += IfBranch(c, b, Span.merge(c.span, b.span))
        }

        val elseB = if(cur.match(TokenKind.Else)) parseBlock() else null
        val endSpan = elseB?.span ?: branches.last().span
        return IfStmt(branches, elseB, Span.merge(ifTok.span, endSpan))
    }

    private fun parseWhileStmt(): Stmt {
        val whileTok = cur.expect(TokenKind.While, ErrorCode.ExpectedToken)
        cur.expect(TokenKind.LParen, ErrorCode.ExpectedToken)
        val cond = parseExpr()
        cur.expect(TokenKind.RParen, ErrorCode.ExpectedToken)
        val body = parseBlock()

        return WhileStmt(cond, body, Span.merge(whileTok.span, body.span))
    }

    private fun parseTypeRef(): TypeRef {
        val t = when {
            cur.match(TokenKind.KwInt) -> NamedType("int", tokens[cur.i - 1].span)
            cur.match(TokenKind.KwFloat) -> NamedType("float", tokens[cur.i - 1].span)
            cur.match(TokenKind.KwBool) -> NamedType("bool", tokens[cur.i - 1].span)
            cur.match(TokenKind.KwChar) -> NamedType("char", tokens[cur.i - 1].span)
            cur.match(TokenKind.KwString) -> NamedType("string", tokens[cur.i - 1].span)
            cur.check(TokenKind.Identifier) -> {
                val id = cur.expectIdent(ErrorCode.ExpectedToken)
                NamedType(id.lexeme, id.span)
            }

            else -> cur.errorHere(ErrorCode.VarNeedsType, listOf("<type>"))
        }

        return t
    }

    private fun precedenceOf(k: TokenKind): Int = when(k) {
        is TokenKind.OrOr -> 10
        is TokenKind.AndAnd -> 20
        is TokenKind.EqualEqual, is TokenKind.BangEqual -> 30
        is TokenKind.Lt, is TokenKind.LtEq, is TokenKind.Gt, is TokenKind.GtEq -> 40
        is TokenKind.Plus, is TokenKind.Minus -> 50
        is TokenKind.Star, is TokenKind.Slash, is TokenKind.Percent -> 60
        is TokenKind.Caret -> 70
        else -> -1
    }

    private fun binOpOf(k: TokenKind): BinOp = when(k) {
        is TokenKind.Plus -> BinOp.Add
        is TokenKind.Minus -> BinOp.Sub
        is TokenKind.Star -> BinOp.Mul
        is TokenKind.Slash -> BinOp.Div
        is TokenKind.Percent -> BinOp.Mod
        is TokenKind.Caret -> BinOp.Pow
        is TokenKind.EqualEqual -> BinOp.Eq
        is TokenKind.BangEqual -> BinOp.Ne
        is TokenKind.Lt -> BinOp.Lt
        is TokenKind.LtEq -> BinOp.Le
        is TokenKind.Gt -> BinOp.Gt
        is TokenKind.GtEq -> BinOp.Ge
        is TokenKind.AndAnd -> BinOp.And
        is TokenKind.OrOr -> BinOp.Or
        else -> cur.errorHere(ErrorCode.UnsupportedBinary, listOf(k.toString(), "?", "?"))
    }

    private fun parseExpr(): Expr = parseBinaryExpr(0)
    private fun parseBinaryExpr(minPrec: Int): Expr {
        var lhs = parseUnary()
        while(true) {
            val tok = cur.peek()
            val prec = precedenceOf(tok.kind)
            if(prec < minPrec) break

            val opTok = cur.advance()
            var rhs = parseUnary()

            while(true) {
                val nextTok = cur.peek()
                val nextPrec = precedenceOf(nextTok.kind)
                if(nextPrec > prec) {
                    val op2 = cur.advance()
                    var rhs2 = parseUnary()
                    while(true) {
                        val afterTok = cur.peek()
                        val afterPrec = precedenceOf(afterTok.kind)
                        if(afterPrec > precedenceOf(op2.kind)) {
                            rhs2 = parseBinaryExpr(afterPrec)
                        } else break
                    }

                    rhs = Binary(binOpOf(op2.kind), rhs, rhs2, Span.merge(rhs.span, rhs2.span))
                } else break
            }

            lhs = Binary(binOpOf(opTok.kind), lhs, rhs, Span.merge(lhs.span, rhs.span))
        }

        return lhs
    }

    private fun parseUnary(): Expr =
        if(cur.match(TokenKind.Bang)) {
            val opTok = tokens[cur.i - 1]
            val e = parseUnary()
            Unary(UnOp.Not, e, Span.merge(opTok.span, e.span))
        } else parsePostfix()

    private fun parsePostfix(): Expr {
        var expr = parsePrimary()
        while(cur.match(TokenKind.Dot)) {
            val dot = tokens[cur.i - 1]
            val methodTok = cur.expectIdent(ErrorCode.ExpectedToken)
            cur.expect(TokenKind.LParen, ErrorCode.ExpectedToken)
            val args = mutableListOf<Expr>()
            if(!cur.check(TokenKind.RParen)) {
                do { args += parseExpr() } while(cur.match(TokenKind.Comma))
            }

            val rpar = cur.expect(TokenKind.RParen, ErrorCode.ExpectedToken)
            expr = MethodCall(expr, methodTok.lexeme, args, Span.merge(expr.span, rpar.span))
        }

        return expr
    }

    private fun parsePrimary(): Expr {
        val t = cur.peek()
        return when(t.kind) {
            is TokenKind.IntLiteral    -> { cur.advance(); IntLit(t.intValue!!, t.span) }
            is TokenKind.FloatLiteral  -> { cur.advance(); FloatLit(t.floatValue!!, t.span) }
            is TokenKind.True          -> { cur.advance(); BoolLit(true, t.span) }
            is TokenKind.False         -> { cur.advance(); BoolLit(false, t.span) }
            is TokenKind.CharLiteral   -> { cur.advance(); CharLit(t.charValue!!, t.span) }
            is TokenKind.StringLiteral -> { cur.advance(); StringLit(t.stringValue!!, t.span) }
            is TokenKind.Identifier -> {
                val id = cur.advance()
                if(cur.match(TokenKind.LParen)) {
                    val args = mutableListOf<Expr>()
                    if(!cur.check(TokenKind.RParen)) {
                        do { args += parseExpr() } while (cur.match(TokenKind.Comma))
                    }

                    val rpar = cur.expect(TokenKind.RParen, ErrorCode.ExpectedToken)
                    Call(id.lexeme, args, Span.merge(id.span, rpar.span))
                } else Ident(id.lexeme, id.span)
            }

            is TokenKind.LParen -> {
                val lpar = cur.advance()
                val e = parseExpr()
                val rpar = cur.expect(TokenKind.RParen, ErrorCode.ExpectedToken)

                when(e) {
                    is Binary     -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is Unary      -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is MethodCall -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is Call       -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is Ident      -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is IntLit     -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is FloatLit   -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is BoolLit    -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is CharLit    -> e.copy(span = Span.merge(lpar.span, rpar.span))
                    is StringLit  -> e.copy(span = Span.merge(lpar.span, rpar.span))
                }
            }

            else -> cur.errorHere(ErrorCode.ExpectedExpr)
        }
    }
}