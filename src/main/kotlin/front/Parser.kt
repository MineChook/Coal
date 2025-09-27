package front

import ast.*
import diagnostics.CoalError
import diagnostics.Diagnostic
import diagnostics.ErrorCode
import diagnostics.Severity

class Parser(
    private val sourceText: String,
    private val tokens: List<Token>,
    private val fileName: String = "<stdin>"
) {
    private var i = 0
    private val constVars = mutableSetOf<String>()

    fun parseProgram(): Program {
        val decls = mutableListOf<Decl>()
        while(!check(TokenKind.EOF)) {
            decls += parseFnDecl()
        }

        return Program(decls)
    }

    private fun parseFnDecl(): FnDecl {
        consume(TokenKind.Fn, ErrorCode.ExpectedToken)
        val name = consumeIdent(ErrorCode.ExpectedToken)
        consume(TokenKind.LParen, ErrorCode.ExpectedToken)
        consume(TokenKind.RParen, ErrorCode.ExpectedToken)

        val body = parseBlock()
        return FnDecl(name, emptyList(), null, body)
    }

    private fun parseBlock(): Block {
        consume(TokenKind.LBrace, ErrorCode.ExpectedToken)
        val stmts = mutableListOf<Stmt>()
        while(!check(TokenKind.RBrace) && !check(TokenKind.EOF)) {
            stmts += parseStmt()
        }

        consume(TokenKind.RBrace, ErrorCode.ExpectedToken)
        return Block(stmts)
    }

    private fun parseStmt(): Stmt {
        return when {
            check(TokenKind.Var) || check(TokenKind.Const) -> parseVarDecl()
            check(TokenKind.If) -> parseIfStmt()
            check(TokenKind.While) -> parseWhileStmt()
            check(TokenKind.Identifier) -> {
                if(peekNextIsAssignOp()) parseAssignStmt() else ExprStmt(parseExpr())
            }

            else -> {
                val e = parseExpr()
                ExprStmt(e)
            }
        }
    }

    private fun parseVarDecl(): VarDecl {
        val isConst = match(TokenKind.Const)
        if(!isConst) {
            consume(TokenKind.Var, ErrorCode.ExpectedToken)
        }

        val name = consumeIdent(ErrorCode.ExpectedToken)

        var annotated: TypeRef? = null
        if(match(TokenKind.Colon)) {
            annotated = parseTypeRef()
        }

        var init: Expr? = null
        if(match(TokenKind.Equal)) {
            init = parseExpr()
        }

        if(isConst && init == null) {
            errorHere(ErrorCode.ConstNeedsInit)
        }

        if(init == null && annotated == null) {
            errorHere(ErrorCode.VarNeedsType)
        }

        if(init != null && annotated != null) {
            val initTy = try { inferType(init) } catch(_: RuntimeException) { null }
            if(initTy != null && initTy != annotated) {
                errorHere(ErrorCode.CompareTypeMismatch)
            }
        }

        if(isConst) constVars.add(name)
        return VarDecl(name, annotated, init, isConst)
    }

    private fun parseAssignStmt(): Stmt {
        val name = consumeIdent(ErrorCode.ExpectedToken)
        val opToken = peek()

        val isPlusEq = check(TokenKind.PlusEqual)
        if(isPlusEq) {
            advance()
            val rhs = parseExpr()
            if(name in constVars) {
                errorHere(ErrorCode.AssignToConst)
            }

            return Assign(name, Binary(BinOp.Add, Ident(name), rhs))
        }

        consume(TokenKind.Equal, ErrorCode.ExpectedToken)
        val value = parseExpr()
        if(name in constVars) {
            errorHere(ErrorCode.AssignToConst)
        }

        return Assign(name, value)
    }

    private fun parseIfStmt(): Stmt {
        consume(TokenKind.If, ErrorCode.ExpectedToken)
        consume(TokenKind.LParen, ErrorCode.ExpectedToken)
        val cond0 = parseExpr()
        consume(TokenKind.RParen, ErrorCode.ExpectedToken)
        val then0 = parseBlock()

        val branches = mutableListOf(IfBranch(cond0, then0))
        while(match(TokenKind.Elif)) {
            consume(TokenKind.LParen, ErrorCode.ExpectedToken)
            val c = parseExpr()
            consume(TokenKind.RParen, ErrorCode.ExpectedToken)
            val b = parseBlock()
            branches += IfBranch(c, b)
        }

        val elseB = if(match(TokenKind.Else)) parseBlock() else null
        return IfStmt(branches, elseB)
    }

    private fun parseWhileStmt(): Stmt {
        consume(TokenKind.While, ErrorCode.ExpectedToken)
        consume(TokenKind.LParen, ErrorCode.ExpectedToken)

        val cond = parseExpr()
        consume(TokenKind.RParen, ErrorCode.ExpectedToken)

        val body = parseBlock()
        return WhileStmt(body)
    }

    private fun parseTypeRef(): TypeRef {
        val t = when {
            match(TokenKind.KwInt) -> "int"
            match(TokenKind.KwFloat) -> "float"
            match(TokenKind.KwBool) -> "bool"
            match(TokenKind.KwChar) -> "char"
            match(TokenKind.KwString) -> "string"
            check(TokenKind.Identifier) -> consumeIdent(ErrorCode.ExpectedToken)
            else -> errorHere(ErrorCode.VarNeedsType)
        }

        return NamedType(t)
    }

    private fun inferType(expr: Expr): TypeRef = when(expr) {
        is IntLit -> NamedType("int")
        is FloatLit -> NamedType("float")
        is BoolLit -> NamedType("bool")
        is CharLit -> NamedType("char")
        is StringLit -> NamedType("string")

        is Unary -> when(expr.op) {
            UnOp.Not -> {
                val t = inferType(expr.expr)
                require(t == NamedType("bool")) { errorHere(ErrorCode.NotConditionBool) }
                NamedType("bool")
            }
        }

        is Binary -> when(expr.op) {
            BinOp.And, BinOp.Or -> {
                val lt = inferType(expr.left)
                val rt = inferType(expr.right)
                require(lt == NamedType("bool") && rt == NamedType("bool")) {
                    errorHere(ErrorCode.LogicNeedsBool)
                }

                NamedType("bool")
            }

            BinOp.Eq, BinOp.Ne, BinOp.Lt, BinOp.Le, BinOp.Gt, BinOp.Ge -> {
                val lt = inferType(expr.left) as NamedType
                val rt = inferType(expr.right) as NamedType
                require(lt == rt) { errorHere(ErrorCode.CompareTypeMismatch) }
                when(expr.op) {
                    BinOp.Eq, BinOp.Ne -> NamedType("bool")
                    else -> {
                        require(lt.name in listOf("int", "float", "char")) { errorHere(ErrorCode.RelopTypeInvalid) }
                        NamedType("bool")
                    }
                }
            }

            else -> {
                val lt = inferType(expr.left) as NamedType
                val rt = inferType(expr.right) as NamedType
                require(lt == rt) { errorHere(ErrorCode.CompareTypeMismatch) }
                require(lt.name in listOf("int", "float", "string")) { errorHere(ErrorCode.InvalidType) }

                lt
            }
        }

        is Ident, is Call -> errorHere(ErrorCode.CannotInferType)
        is MethodCall -> when(expr.method) {
            "toString" -> NamedType("string")
            "toInt" -> NamedType("int")
            "toFloat" -> NamedType("float")
            else -> errorHere(ErrorCode.UnknownMethod)
        }
    }

    private fun parseExpr(): Expr = parseBinaryExpr(0)

    private fun parseBinaryExpr(minPrec: Int): Expr {
        var lhs = parseUnary()
        while(true) {
            val tok = peek()
            val prec = precedenceOf(tok.kind)
            if (prec < minPrec) break

            val opTok = advance()
            var rhs = parseUnary()

            while(true) {
                val nextTok = peek()
                val nextPrec = precedenceOf(nextTok.kind)
                if(nextPrec > prec) {
                    val op2 = advance()
                    var rhs2 = parseUnary()

                    while(true) {
                        val afterTok = peek()
                        val afterPrec = precedenceOf(afterTok.kind)
                        if(afterPrec > precedenceOf(op2.kind)) {
                            rhs2 = parseBinaryExpr(afterPrec)
                        } else break
                    }

                    rhs = Binary(binOpOf(op2.kind), rhs, rhs2)
                } else break
            }

            lhs = Binary(binOpOf(opTok.kind), lhs, rhs)
        }

        return lhs
    }

    private fun parseUnary(): Expr {
        return if(match(TokenKind.Bang)) {
            Unary(UnOp.Not, parseUnary())
        } else {
            parsePostfix()
        }
    }

    private fun parsePostfix(): Expr {
        var expr = parsePrimary()
        while(match(TokenKind.Dot)) {
            val method = consumeIdent(ErrorCode.ExpectedToken)
            consume(TokenKind.LParen, ErrorCode.ExpectedToken)
            val args = mutableListOf<Expr>()
            if(!check(TokenKind.RParen)) {
                do {
                    args += parseExpr()
                } while(match(TokenKind.Comma))
            }

            consume(TokenKind.RParen, ErrorCode.ExpectedToken)
            expr = MethodCall(expr, method, args)
        }

        return expr
    }

    private fun parsePrimary(): Expr {
        val t = peek()
        return when (t.kind) {
            is TokenKind.IntLiteral -> { advance(); IntLit(t.intValue!!) }
            is TokenKind.FloatLiteral -> { advance(); FloatLit(t.floatValue!!) }
            is TokenKind.True -> { advance(); BoolLit(true) }
            is TokenKind.False -> { advance(); BoolLit(false) }
            is TokenKind.CharLiteral -> { advance(); CharLit(t.charValue!!) }
            is TokenKind.StringLiteral -> { advance(); StringLit(t.stringValue!!) }
            is TokenKind.Identifier -> {
                val name = advance().lexeme
                if(match(TokenKind.LParen)) {
                    val args = mutableListOf<Expr>()
                    if(!check(TokenKind.RParen)) {
                        do {
                            args += parseExpr()
                        } while(match(TokenKind.Comma))
                    }

                    consume(TokenKind.RParen, ErrorCode.ExpectedToken)
                    Call(name, args)
                } else {
                    Ident(name)
                }
            }

            is TokenKind.LParen -> {
                advance()
                val e = parseExpr()
                consume(TokenKind.RParen, ErrorCode.ExpectedToken)
                e
            }

            else -> errorHere(ErrorCode.ExpectedExpr)
        }
    }

    // helpers
    private fun consume(kind: TokenKind, error: ErrorCode): Token {
        if(check(kind)) return advance()
        errorHere(error)
    }

    private fun consumeIdent(error: ErrorCode): String {
        if(check(TokenKind.Identifier)) return advance().lexeme
        errorHere(error)
    }

    private fun match(kind: TokenKind): Boolean {
        if(check(kind)) {
            advance()
            return true
        }

        return false
    }

    private fun check(kind: TokenKind): Boolean = peek().kind::class == kind::class
    private fun peek(): Token = tokens[i]
    private fun advance(): Token = tokens[i++]

    private fun errorHere(error: ErrorCode): Nothing {
        throw CoalError(Diagnostic(Severity.ERROR, error, fileName, peek().span, listOf(error.template)))
    }

    private fun precedenceOf(kind: TokenKind): Int = when(kind) {
        is TokenKind.OrOr -> 10
        is TokenKind.AndAnd -> 20
        is TokenKind.EqualEqual, is TokenKind.BangEqual -> 30
        is TokenKind.Lt, is TokenKind.LtEq, is TokenKind.Gt, is TokenKind.GtEq -> 40
        is TokenKind.Plus, is TokenKind.Minus -> 50
        is TokenKind.Star, is TokenKind.Slash, is TokenKind.Percent -> 60
        is TokenKind.Caret -> 70
        else -> -1
    }

    private fun binOpOf(kind: TokenKind): BinOp = when(kind) {
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
        else -> errorHere(ErrorCode.UnsupportedBinary)
    }

    private fun peekNextIsAssignOp(): Boolean {
        if(!check(TokenKind.Identifier)) return false
        val saved = i
        advance()

        val isAssign = check(TokenKind.Equal) || check(TokenKind.PlusEqual)
        i = saved
        return isAssign
    }
}
