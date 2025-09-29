package front.parser

import ast.*
import diagnostics.*
import kotlin.reflect.typeOf

class TypeChecker(
    private val fileName: String,
    private val sourceText: String
) {
    private data class VarInfo(val type: NamedType, val isConst: Boolean, val span: Span)

    private val env = ArrayDeque<LinkedHashMap<String, VarInfo>>()
    private val functions = linkedMapOf<String, Pair<List<NamedType>, NamedType?>>() // name -> (params, returnType)

    fun check(program: Program) {
        program.decls.forEach { d ->
            if(d is FnDecl) functions[d.name] = emptyList<NamedType>() to NamedType("int", d.span)
        }

        program.decls.forEach { d ->
            when(d) {
                is FnDecl -> checkFn(d)
            }
        }
    }

    private fun checkFn(fn: FnDecl) {
        pushScope()
        checkBlock(fn.body)
        popScope()
    }

    private fun checkBlock(block: Block) {
        pushScope()
        block.stmts.forEach(::checkStmt)
        popScope()
    }

    private fun checkStmt(stmt: Stmt) {
        when(stmt) {
            is VarDecl -> {
                if(current().containsKey(stmt.name)) error(stmt.span, ErrorCode.RedeclaredVariable, stmt.name)
                val ty = stmt.annotatedType?.let { ensureNamed(it) }
                val initTy = stmt.init?.let { typeOf(it) }
                val finalTy = when {
                    ty != null && initTy != null -> {
                        if(ty.name != initTy.name) error(stmt.span, ErrorCode.TypeMismatch, ty.name, initTy.name)
                        ty
                    }

                    ty != null -> ty
                    initTy != null -> initTy
                    else -> error(stmt.span, ErrorCode.VarNeedsType, stmt.name)
                }

                if(stmt.isConst && stmt.init == null) error(stmt.span, ErrorCode.ConstNeedsInit, stmt.name)
                declare(stmt.name, finalTy, stmt.isConst, stmt.span)
            }

            is Assign -> {
                val v = resolve(stmt.name) ?: error(stmt.span, ErrorCode.UndefinedVariable, stmt.name)
                if(v.isConst) error(stmt.span, ErrorCode.AssignToConst, stmt.name)
                val rhsTy = typeOf(stmt.value)
                if(rhsTy.name != v.type.name) error(stmt.span, ErrorCode.TypeMismatch, v.type.name, rhsTy.name)
            }

            is ExprStmt -> { typeOf(stmt.expr) }
            is IfStmt -> {
                stmt.branches.forEach { br ->
                    val cTy = typeOf(br.condition)
                    if(cTy.name != "bool") error(br.condition.span, ErrorCode.NonBoolCondition, cTy.name)
                    checkBlock(br.body)
                }

                stmt.elseBranch?.let { checkBlock(it) }
            }

            is WhileStmt -> {
                val cTy = typeOf(stmt.condition)
                if(cTy.name != "bool") error(stmt.condition.span, ErrorCode.NonBoolCondition, cTy.name)
                checkBlock(stmt.body)
            }
        }
    }

    private fun typeOf(e: Expr): NamedType {
        return when(e) {
            is IntLit -> NamedType("int", e.span)
            is FloatLit -> NamedType("float", e.span)
            is BoolLit -> NamedType("bool", e.span)
            is CharLit -> NamedType("char", e.span)
            is StringLit -> NamedType("string", e.span)

            is Ident -> {
                val v = resolve(e.name) ?: error(e.span, ErrorCode.UndefinedVariable, e.name)
                v.type
            }

            is Unary -> when(e.op) {
                UnOp.Not -> {
                    val t = typeOf(e.expr)
                    if(t.name != "bool") error(e.span, ErrorCode.NotConditionBool, "!", t.name)
                    NamedType("bool", e.span)
                }
            }

            is Binary -> {
                when(e.op) {
                    BinOp.And, BinOp.Or -> {
                        val lt = typeOf(e.left); val rt = typeOf(e.right)
                        if(lt.name != "bool" || rt.name != "bool") error(e.span, ErrorCode.LogicNeedsBool, e.op.name)
                        NamedType("bool", e.span)
                    }

                    BinOp.Eq, BinOp.Ne, BinOp.Lt, BinOp.Le, BinOp.Gt, BinOp.Ge -> {
                        val lt = typeOf(e.left); val rt = typeOf(e.right)
                        if(lt.name != rt.name) error(e.span, ErrorCode.CompareTypeMismatch, lt.name, rt.name)
                        if(e.op == BinOp.Eq || e.op == BinOp.Ne) return NamedType("bool", e.span)
                        if(lt.name !in listOf("int", "float", "char")) error(e.span, ErrorCode.RelopTypeInvalid, e.op.name, lt.name)

                        NamedType("bool", e.span)
                    }

                    else -> {
                        val lt = typeOf(e.left); val rt = typeOf(e.right)
                        if(lt.name != rt.name) error(e.span, ErrorCode.TypeMismatch, lt.name, rt.name)

                        if(lt.name == "string") {
                            if(e.op != BinOp.Add) error(e.span, ErrorCode.StringsOnlyAdd)
                            return NamedType("string", e.span)
                        }

                        if(lt.name !in listOf("int", "float")) error(e.span, ErrorCode.InvalidType, lt.name)
                        lt
                    }
                }
            }

            is Call -> {
                val sig = functions[e.callee] ?: error(e.span, ErrorCode.UnknownFunction, e.callee)
                val (paramTys, retTy) = sig
                if(e.args.size != paramTys.size) error(e.span, ErrorCode.ArityMismatch, e.callee, "${e.args.size}", "${paramTys.size}")

                e.args.forEachIndexed { idx, arg ->
                    val got = typeOf(arg)
                    val want = paramTys[idx]
                    if(got.name != want.name) error(arg.span, ErrorCode.TypeMismatch, want.name, got.name)
                }

                retTy ?: NamedType("int", e.span)
            }

            is MethodCall -> {
                if(e.args.isNotEmpty()) error(e.span, ErrorCode.UnsupportedConversion, "method ${e.method}", "no-args")

                val recvTy = typeOf(e.receiver)
                when(e.method) {
                    "toString" -> NamedType("string", e.span)
                    "toInt" -> when(recvTy.name) {
                        "float", "string", "int", "char", "bool" -> NamedType("int", e.span)
                        else -> error(e.span, ErrorCode.UnsupportedConversion, recvTy.name, "int")
                    }

                    "toFloat" -> when(recvTy.name) {
                        "int", "string", "float", "char", "bool" -> NamedType("float", e.span)
                        else -> error(e.span, ErrorCode.UnsupportedConversion, recvTy.name, "float")
                    }

                    else -> error(e.span, ErrorCode.UnknownMethod, e.method)
                }
            }
        }
    }

    private fun pushScope() = env.addLast(linkedMapOf())
    private fun popScope() = env.removeLast()
    private fun current(): LinkedHashMap<String, VarInfo> = env.lastOrNull() ?: run { pushScope(); env.last() }
    private fun declare(name: String, ty: NamedType, isConst: Boolean, span: Span) {
        current()[name] = VarInfo(ty, isConst, span)
    }

    private fun resolve(name: String): VarInfo? {
        for(scope in env.asReversed()) scope[name]?.let { return it }
        return null
    }

    private fun error(span: Span, code: ErrorCode, vararg args: String): Nothing {
        throw CoalError(Diagnostic(Severity.ERROR, code, fileName, span, args.toList()))
    }

    private fun ensureNamed(t: TypeRef): NamedType = t as? NamedType ?: error(t.span, ErrorCode.InvalidType, "<non-named>")
}