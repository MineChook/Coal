package codegen.ir

class ModuleBuilder(
    private val moduleName: String = "coal-module",
    private val sourceName: String = "coal"
) {
    private val header = StringBuilder()
    private val globals = StringBuilder()
    private val decls = StringBuilder()
    private val fns = StringBuilder()

    private val stringTable = StringTable(globals)

    init {
        header.appendLine("; ModuleID = \"$moduleName\"")
        header.appendLine("source_filename = \"$sourceName\"")
    }

    fun declarePrintf() { decls.appendLine("declare i32 @printf(ptr, ...)") }
    fun declareSnprintf() { decls.appendLine("declare i32 @snprintf(ptr, i64, ptr, ...)") }
    fun declareMalloc() { decls.appendLine("declare ptr @malloc(i64)") }
    fun declareMemcpy() { decls.appendLine("declare ptr @memcpy(ptr, ptr, i64)") }
    fun declareStrtol() { decls.appendLine("declare i64 @strtol(ptr, ptr, i32)") }
    fun declareStrtod() { decls.appendLine("declare double @strtod(ptr, ptr)") }

    fun declare(name: String, sig: String) {
        decls.appendLine("declare $sig @$name")
    }

    fun global(name: String, llTy: String, init: String, attrs: String? = null) {
        val at = attrs?.let { " $it" } ?: ""
        globals.appendLine("@$name =$at global $llTy $init")
    }

    fun internCString(raw: String): StringRef = stringTable.intern(raw)

    fun function(name: String, retTy: String = "i32", params: List<Pair<String, String>> = emptyList()): FunctionBuilder {
        return FunctionBuilder(this, name, retTy, params)
    }

    internal fun appendFunctionIR(fnIR: String) {
        fns.append(fnIR)
    }

    override fun toString(): String = buildString {
        append(header)
        if(decls.isNotEmpty()) {
            append(decls).appendLine()
        }

        if(globals.isNotEmpty()) {
            append(globals).appendLine()
        }

        append(fns)
    }
}