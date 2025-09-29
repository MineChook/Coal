package front.types

import ast.Expr
import ast.NamedType
import java.util.IdentityHashMap

/**
 * Holds type information for expressions and variables
 *
 * @property exprTypes maps expressions to their types
 * @property varTypes maps (function name, variable name) pairs to their types
 */
class TypeInfo {
    val exprTypes: IdentityHashMap<Expr, NamedType> = IdentityHashMap()
    val varTypes: MutableMap<Pair<String, String>, NamedType> = linkedMapOf()

    /**
     * Retrieves the type of the given expression
     *
     * @param expr the expression whose type is to be retrieved
     * @return the type of the expression
     */
    fun typeOf(expr: Expr): NamedType = exprTypes[expr] ?: error("TypeInfo missing type for expr: ${expr::class.simpleName}")

    /**
     * Retrieves the type of the given variable in the context of a specific function
     *
     * @param fn the name of the function
     * @param name the name of the variable
     * @return the type of the variable
     */
    fun typeOfVar(fn: String, name: String): NamedType = varTypes[fn to name] ?: error("TypeInfo missing type for var: $fn::$name")
}