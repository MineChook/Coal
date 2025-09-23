package codegen.ir

class FunctionBuilder(
    private val mod: ModuleBuilder,
    private val name: String,
    private val retTy: String,
    private val params: List<Pair<String, String>>
) {
    private val body = StringBuilder()
    private var tmp = 0
    private var blockOpen = false

    fun start(): FunctionBuilder {
        val sigParams = if(params.isEmpty()) "" else params.joinToString(", ") { (t, n) -> "$t %$n" }
        body.appendLine("define $retTy @$name($sigParams) {")
        return this
    }

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
    fun end(): FunctionBuilder {
        body.appendLine("}")
        body.appendLine()
        mod.appendFunctionIR(body.toString())
        return this
    }
}