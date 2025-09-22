package front

import ast.*

class Parser(
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
        consume(TokenKind.Fn, "expected 'fn'")
        val name = consumeIdent("expected function name")
        consume(TokenKind.LParen, "expected '(' after function name")
        consume(TokenKind.RParen, "expected ')' after '('")

        val body = parseBlock()
        return FnDecl(name, emptyList(), null, body)
    }

    private fun parseBlock(): Block {
        consume(TokenKind.LBrace, "expected '{' to start block")
        val stmts = mutableListOf<Stmt>()
        while(!check(TokenKind.RBrace) && !check(TokenKind.EOF)) {
            stmts += parseStmt()
        }

        consume(TokenKind.RBrace, "expected '}' to end block")
        return Block(stmts)
    }

    private fun parseStmt(): Stmt {
        return if(check(TokenKind.Var) || check(TokenKind.Const)) {
            parseVarDecl()
        } else {
            parseAssignStmt()
        }
    }

    private fun parseVarDecl(): VarDecl {
        val isConst = match(TokenKind.Const)
        if(!isConst) {
            consume(TokenKind.Var, "expected 'var' or 'const'")
        }

        val name = consumeIdent("expected variable name")

        var annotated: TypeRef? = null
        if(match(TokenKind.Colon)) {
            annotated = parseTypeRef()
        }

        var init: Expr? = null
        if(match(TokenKind.Equal)) {
            init = parseExpr()
        }

        if(isConst && init == null) {
            errorHere("const variable '$name' must be initialized")
        }

        if(init == null && annotated == null) {
            errorHere("variable '$name' needs a type if not initialized")
        }

        if(init != null && annotated != null) {
            val initTy = inferType(init)
            if(annotated != initTy) {
                errorHere("type mismatch: cannot assign '${initTy}' to variable '$name' of type '${annotated}'")
            }
        }

        if(isConst) constVars.add(name)
        return VarDecl(name, annotated, init, isConst)
    }

    private fun parseAssignStmt(): Stmt {
        val name = consumeIdent("expected identifier")
        consume(TokenKind.Equal, "expected '=' in assignment")
        val value = parseExpr()

        if(name in constVars) {
            errorHere("cannot assign to const variable '$name'")
        }

        return Assign(name, value)
    }

    private fun parseTypeRef(): TypeRef {
        val t = when {
            match(TokenKind.KwInt) -> "int"
            match(TokenKind.KwFloat) -> "float"
            match(TokenKind.KwBool) -> "bool"
            match(TokenKind.KwChar) -> "char"
            match(TokenKind.KwString) -> "string"
            check(TokenKind.Identifier) -> consumeIdent("expected type name")
            else -> errorHere("expected type")
        }

        return NamedType(t)
    }

    private fun inferType(expr: Expr): TypeRef = when(expr) {
        is IntLit -> NamedType("int")
        is FloatLit -> NamedType("float")
        is BoolLit -> NamedType("bool")
        is CharLit -> NamedType("char")
        is StringLit -> NamedType("string")
        is Ident -> errorHere("cannot infer type of identifier '${expr.name}' without context")
    }

    private fun parseExpr(): Expr {
        val t = peek()
        return when(t.kind) {
            is TokenKind.IntLiteral -> {
                advance()
                IntLit(t.intValue!!)
            }

            is TokenKind.FloatLiteral -> {
                advance()
                FloatLit(t.floatValue!!)
            }

            is TokenKind.True -> {
                advance()
                BoolLit(true)
            }

            is TokenKind.False -> {
                advance()
                BoolLit(false)
            }

            is TokenKind.CharLiteral -> {
                advance()
                CharLit(t.charValue!!)
            }

            is TokenKind.StringLiteral -> {
                advance()
                StringLit(t.stringValue!!)
            }

            is TokenKind.Identifier -> {
                advance()
                Ident(t.lexeme)
            }

            else -> errorHere("expected expression")
        }
    }

    // helpers
    private fun consume(kind: TokenKind, msg: String): Token {
        if(check(kind)) return advance()
        errorHere(msg)
    }

    private fun consumeIdent(msg: String): String {
        if(check(TokenKind.Identifier)) return advance().lexeme
        errorHere(msg)
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

    private fun errorHere(message: String): Nothing {
        val token = peek()
        val where = "$fileName:${token.span.line}:${token.span.col}"
        throw RuntimeException("$where: $message (found '${token.kind}')")
    }
}