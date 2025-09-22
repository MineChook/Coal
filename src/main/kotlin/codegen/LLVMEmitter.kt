package codegen

import ast.*

class LLVMEmitter {
    private val moduleHeader = StringBuilder()
    private val globals = StringBuilder()
    private val fns = StringBuilder()

    private val strNames = LinkedHashMap<String, String>()
    private var strCounter = 0

    private val declaredDbg = HashSet<String>()
    private var tmp = 0

    fun emit(prog: Program): String {
        moduleHeader.clear()
        globals.clear()
        fns.clear()
        strNames.clear()
        declaredDbg.clear()
        tmp = 0

        moduleHeader.appendLine("; ModuleID = \"coal-module\"")
        moduleHeader.appendLine("source_filename = \"coal\"")
        moduleHeader.appendLine()

        for(d in prog.decls) {
            when(d) {
                is FnDecl -> emitFn(d)
            }
        }

        if(strNames.isNotEmpty()) {
            globals.appendLine("; string constants")
            for((content, gname) in strNames) {
                val encoded = encodeCString(content)
                val nBytes = encoded.length + 1
                globals.appendLine("$gname = private unnamed_addr constant [$nBytes x i8] c\"$encoded\\00\"")
            }

            globals.appendLine()
        }

        return buildString {
            append(moduleHeader)
            append(globals)
            append(fns)
        }
    }

    private fun emitFn(fn: FnDecl) {
        val fname = "@${mangle(fn.name)}"
        fns.appendLine("define i32 $fname() {")
        fns.appendLine("entry:")

        for(s in fn.body.stmts) {
            when(s) {
                is VarDecl -> {
                    val t = (s.annotedType ?: inferType(s.init!!)) as NamedType
                    val llTy = llTypeOf(t)
                    val vreg = "%${s.name}"

                    fns.appendLine("  $vreg = alloca $llTy")

                    when(val c = constOf(s.init!!)) {
                        is Const.Immediate -> {
                            fns.appendLine("  store ${c.ty} ${c.text}, ${llTy}* $vreg")
                        }

                        is Const.StringStruct -> {
                            val lit = "{ ${c.ptrTy} ${c.ptrExpr}, i32 ${c.len} }"
                            fns.appendLine("  store $llTy $lit, ${llTy}* $vreg")
                        }
                    }

                    val dbgName = "__dbg_${fn.name}_${s.name}"
                    ensureDbgGlobal(dbgName, llTy)

                    val tmpv = nextTmp()
                    fns.appendLine("  $tmpv = load $llTy, ${llTy}* $vreg")
                    fns.appendLine("  store $llTy $tmpv, ${llTy}* @${dbgName}")
                    fns.appendLine()
                }
            }
        }

        fns.appendLine("  ret i32 0")
        fns.appendLine("}")
        fns.appendLine()
    }

    private fun inferType(expr: Expr): TypeRef = when(expr) {
        is IntLit -> NamedType("int")
        is FloatLit -> NamedType("float")
        is BoolLit -> NamedType("bool")
        is CharLit -> NamedType("char")
        is StringLit -> NamedType("string")
        is Ident -> error("cannot infer type of identifier '${expr.name}' without context")
    }

    private fun llTypeOf(t: NamedType): String = when(t.name) {
        "int" -> "i32"
        "float" -> "double"
        "bool" -> "i1"
        "char" -> "i8"
        "string" -> "{ i8*, i32 }"
        else -> error("unknown type: ${t.name}")
    }

    private sealed interface Const {
        data class Immediate(val ty: String, val text: String) : Const
        data class StringStruct(val ptrTy: String, val ptrExpr: String, val len: Int) : Const
    }

    private fun constOf(e: Expr): Const = when (e) {
        is IntLit -> Const.Immediate("i32", e.value.toString())
        is FloatLit -> Const.Immediate("double", e.value.toString())
        is BoolLit -> Const.Immediate("i1", if (e.value) "1" else "0")
        is CharLit -> Const.Immediate("i8", e.value.toString())
        is StringLit -> {
            val (ptrTy, gep, len) = stringPtrExpr(e.value)
            Const.StringStruct(ptrTy, gep, len)
        }

        is Ident -> error("cannot get constant value of identifier '${e.name}'")
    }

    private fun stringPtrExpr(content: String): Triple<String, String, Int> {
        val gname = strNames.getOrPut(content) { "@.str.${strCounter++}" }
        val enc = encodeCString(content)
        val nBytes = enc.length + 1
        val gep = "getelementptr inbounds ([$nBytes x i8], [$nBytes x i8]* $gname, i32 0, i32 0)"
        return Triple("i8*", gep, enc.length)
    }

    private fun encodeCString(s: String): String {
        val sb = StringBuilder()
        for(ch in s) {
            when(ch) {
                '\\' -> sb.append("\\5C")
                '\"' -> sb.append("\\22")
                '\n' -> sb.append("\\0A")
                '\r' -> sb.append("\\0D")
                '\t' -> sb.append("\\09")
                else -> {
                    val code = ch.code
                    if(code in 32..126) sb.append(ch)
                    else sb.append("\\" + code.toString(16).uppercase().padStart(2, '0'))
                }
            }
        }
        return sb.toString()
    }

    private fun ensureDbgGlobal(name: String, llTy: String) {
        if(declaredDbg.add(name)) {
            globals.appendLine("@$name = global $llTy ${zeroInit(llTy)}")
        }
    }

    private fun zeroInit(llTy: String): String = when (llTy) {
        "i1" -> "0"
        "i8" -> "0"
        "i32" -> "0"
        "double" -> "0.0"
        "{ i8*, i32 }" -> "{ i8* null, i32 0 }"
        else -> error("cannot zero-initialize type: $llTy")
    }

    private fun mangle(name: String): String = name
    private fun nextTmp(): String = "%t${tmp++}"
}