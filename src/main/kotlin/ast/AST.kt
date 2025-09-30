package ast

import diagnostics.Span
import kotlinx.serialization.Serializable

@Serializable data class Program(val decls: List<Decl>)

@Serializable sealed interface Decl { val span: Span }
@Serializable data class FnDecl(
    val name: String,
    val params: List<Param>,
    val returnType: TypeRef?,
    val body: Block,
    override val span: Span
) : Decl

@Serializable data class Param(val name: String, val type: TypeRef, val span: Span)
@Serializable data class Block(val stmts: List<Stmt>, val span: Span)

@Serializable sealed interface Stmt { val span: Span }
@Serializable data class VarDecl(
    val name: String,
    val annotatedType: TypeRef?,
    val init: Expr?,
    val isConst: Boolean,
    override val span: Span
) : Stmt

@Serializable data class Assign(
    val name: String,
    val value: Expr,
    override val span: Span
) : Stmt

@Serializable data class ExprStmt(val expr: Expr, override val span: Span) : Stmt

@Serializable sealed interface Expr { val span: Span }
@Serializable data class Binary(
    val op: BinOp,
    val left: Expr,
    val right: Expr,
    override val span: Span
) : Expr

@Serializable enum class BinOp { Add, Sub, Mul, Div, Mod, Pow, Eq, Ne, Lt, Le, Gt, Ge, And, Or }

@Serializable data class Call(
    val callee: String,
    val args: List<Expr>,
    override val span: Span
) : Expr

@Serializable data class MethodCall(
    val receiver: Expr,
    val method: String,
    val args: List<Expr>,
    override val span: Span
) : Expr

@Serializable data class IfStmt(
    val branches: List<IfBranch>,
    val elseBranch: Block?,
    override val span: Span
) : Stmt

@Serializable data class IfBranch(val condition: Expr, val body: Block, val span: Span)

@Serializable data class WhileStmt(
    val condition: Expr,
    val body: Block,
    override val span: Span
) : Stmt

@Serializable data class Unary(
    val op: UnOp,
    val expr: Expr,
    override val span: Span
) : Expr

@Serializable enum class UnOp { Not }

@Serializable data class Ident(val name: String, override val span: Span) : Expr
@Serializable data class IntLit(val value: Long, override val span: Span) : Expr
@Serializable data class FloatLit(val value: Double, override val span: Span) : Expr
@Serializable data class BoolLit(val value: Boolean, override val span: Span) : Expr
@Serializable data class CharLit(val value: Int, override val span: Span) : Expr
@Serializable data class StringLit(val value: String, override val span: Span) : Expr

@Serializable sealed interface TypeRef { val span: Span }
@Serializable data class NamedType(val name: String, override val span: Span) : TypeRef