package codegen.ir

/**
 * A builder for LLVM IR functions
 *
 * @param mod The module builder to which this function belongs
 * @param name The name of the function
 * @param retTy The return type of the function
 * @param params The parameters of the function as a list of (type, name) pairs
 */
class FunctionBuilder(
    private val mod: ModuleBuilder,
    private val name: String,
    private val retTy: String,
    private val params: List<Pair<String, String>>
) {
    private val body = StringBuilder()
    private var tmp = 0
    private var blockOpen = false

    /**
     * Starts the function definition
     *
     * @return The FunctionBuilder instance for chaining
     */
    fun start(): FunctionBuilder {
        val sigParams = if(params.isEmpty()) "" else params.joinToString(", ") { (t, n) -> "$t %$n" }
        body.appendLine("define $retTy @$name($sigParams) {")
        return this
    }

    /**
     * Starts a new basic block with the given label
     *
     * @param label The label of the basic block
     * @return A BlockBuilder for building the contents of the block
     */
    fun block(label: String): BlockBuilder {
        if(!blockOpen) {
            body.appendLine("$label:")
            blockOpen = true
        } else {
            body.appendLine("$label:")
        }

        return BlockBuilder(this, body)
    }

    internal fun nextTmp(): String = "%t${tmp++}"

    /**
     * Ends the function definition and appends it to the module
     * @return The FunctionBuilder instance for chaining
     */
    fun end(): FunctionBuilder {
        body.appendLine("}")
        body.appendLine()
        mod.appendFunctionIR(body.toString())
        return this
    }
}