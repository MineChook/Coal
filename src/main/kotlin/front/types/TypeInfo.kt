package front.types

import ast.Expr
import ast.NamedType
import java.util.IdentityHashMap

class TypeInfo {
    val exprTypes: IdentityHashMap<Expr, NamedType> = IdentityHashMap()
    val varTypes: MutableMap<Pair<String, String>, NamedType> = linkedMapOf()

    fun typeOf(expr: Expr): NamedType = exprTypes[expr] ?: error("TypeInfo missing type for expr: ${expr::class.simpleName}")
    fun typeOfVar(fn: String, name: String): NamedType = varTypes[fn to name] ?: error("TypeInfo missing type for var: $fn::$name")
}