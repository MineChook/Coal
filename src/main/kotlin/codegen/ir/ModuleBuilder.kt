package codegen.ir

/**
 * Builder for an LLVM IR module
 *
 * @param moduleName Name of the module
 * @param sourceName Name of the source file
 */
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
    fun declareStrlen() { decls.appendLine("declare i64 @strlen(ptr)") }
    fun declareFgets() { decls.appendLine("declare ptr @fgets(ptr, i32, ptr)") }
    fun declareStdin() { globals.appendLine("@__stdinp = external global ptr") }

    fun declareMalloc() { decls.appendLine("declare ptr @malloc(i64)") }
    fun declareMemcpy() { decls.appendLine("declare ptr @memcpy(ptr, ptr, i64)") }
    fun declareStrtol() { decls.appendLine("declare i64 @strtol(ptr, ptr, i32)") }
    fun declareStrtod() { decls.appendLine("declare double @strtod(ptr, ptr)") }

    /**
     * Add a global variable to the module
     *
     * @param name Name of the global variable
     * @param llTy LLVM type of the global variable
     * @param init Initial value of the global variable
     * @param attrs Optional attributes for the global variable (e.g., "constant", "align 4")
     */
    fun global(name: String, llTy: String, init: String, attrs: String? = null) {
        val at = attrs?.let { " $it" } ?: ""
        globals.appendLine("@$name =$at global $llTy $init")
    }

    /**
     * Intern a C-style string and return a reference to it
     *
     * @param raw The raw string to intern
     * @return A reference to the interned string
     */
    fun internCString(raw: String): StringRef = stringTable.intern(raw)

    /**
     * Start building a function
     *
     * @param name Name of the function
     * @param retTy Return type of the function (default: "i32")
     * @param params List of parameters as pairs of (name, type) (default: empty list)
     * @return A [FunctionBuilder] to build the function body
     */
    fun function(name: String, retTy: String = "i32", params: List<Pair<String, String>> = emptyList()): FunctionBuilder {
        return FunctionBuilder(this, name, retTy, params)
    }

    /**
     * Append the IR of a completed function to the module
     *
     * @param fnIR The LLVM IR of the function
     */
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