package codegen

import ast.*

class LLVMEmitter {
    private sealed interface RValue {
        data class Immediate(val llTy: String, val text: String) : RValue
        data class ValueReg(val llTy: String, val reg: String) : RValue
        data class Aggregate(val literal: String) : RValue
    }

    private val moduleHeader = StringBuilder()
    private val globals = StringBuilder()
    private val fns = StringBuilder()

    private val strNames = LinkedHashMap<String, String>()
    private var strCounter = 0

    private val declaredDbg = HashSet<String>()

    private data class LocalSlot(val llTy: String, val reg: String)
    private var tmp = 0
    private var locals: MutableMap<String, LocalSlot> = mutableMapOf()
    private var currentFnName: String = ""

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
        currentFnName = fn.name
        locals = mutableMapOf()

        val fname = "@${mangle(fn.name)}"
        fns.appendLine("define i32 $fname() {")
        fns.appendLine("entry:")

        for(s in fn.body.stmts) {
            when(s) {
                is VarDecl -> lowerVarDecl(fn, s)
                is Assign -> lowerAssign(fn, s)
            }
        }

        fns.appendLine("  ret i32 0")
        fns.appendLine("}")
        fns.appendLine()
    }

    private fun lowerVarDecl(fn: FnDecl, decl: VarDecl) {
        val t = (decl.annotatedType ?: inferType(decl.init!!)) as NamedType
        val llTy = llTypeOf(t)
        val reg = "%${decl.name}"

        fns.appendLine("  $reg = alloca $llTy")

        if(decl.init != null) {
            val rhs = valueOfExpr(decl.init, expectedLlTy = llTy)
            when(rhs) {
                is RValue.Immediate -> fns.appendLine("  store ${rhs.llTy} ${rhs.text}, ${llTy}* $reg")
                is RValue.ValueReg  -> fns.appendLine("  store ${rhs.llTy} ${rhs.reg}, ${llTy}* $reg")
                is RValue.Aggregate -> fns.appendLine("  store $llTy ${rhs.literal}, ${llTy}* $reg")
            }
        } else {
            fns.appendLine("  store $llTy ${zeroInit(llTy)}, ${llTy}* $reg")
        }

        locals[decl.name] = LocalSlot(llTy, reg)

        mirrorToDebugGlobal(fn.name, decl.name, llTy, reg)
        fns.appendLine()
     }

    private fun lowerAssign(fn: FnDecl, asg: Assign) {
        val slot = locals[asg.name] ?: error("undefined variable: ${asg.name}")
        val llTy = slot.llTy
        val reg = slot.reg

        val rhs = valueOfExpr(asg.value, llTy)
        when (rhs) {
            is RValue.Immediate -> fns.appendLine("  store ${rhs.llTy} ${rhs.text}, ${llTy}* $reg")
            is RValue.ValueReg -> fns.appendLine("  store ${rhs.llTy} ${rhs.reg}, ${llTy}* $reg")
            is RValue.Aggregate -> fns.appendLine("  store $llTy ${rhs.literal}, ${llTy}* $reg")
        }

        mirrorToDebugGlobal(fn.name, asg.name, llTy, reg)
        fns.appendLine()
    }

    private fun valueOfExpr(e: Expr, expectedLlTy: String? = null) : RValue = when(e) {
        is IntLit -> RValue.Immediate("i32", e.value.toString())
        is FloatLit -> RValue.Immediate("double", e.value.toString())
        is BoolLit -> RValue.Immediate("i1", if (e.value) "1" else "0")
        is CharLit -> RValue.Immediate("i8", e.value.toString())
        is StringLit -> {
            val (ptrTy, gep, len) = stringPtrExpr(e.value)
            RValue.Aggregate("{ $ptrTy $gep, i32 $len }")
        }

        is Ident -> {
            val slot = locals[e.name] ?: error("undefined variable: ${e.name}")
            val t = nextTmp()
            fns.appendLine("  $t = load ${slot.llTy}, ${slot.llTy}* ${slot.reg}")
            RValue.ValueReg(slot.llTy, t)
        }
    }

    private fun inferType(expr: Expr): TypeRef = when(expr) {
        is IntLit -> NamedType("int")
        is FloatLit -> NamedType("float")
        is BoolLit -> NamedType("bool")
        is CharLit -> NamedType("char")
        is StringLit -> NamedType("string")
        is Ident -> {
            val slot = locals[expr.name]
            if(slot != null) {
                val tname = when(slot.llTy) {
                    "i32" -> "int"
                    "double" -> "float"
                    "i1" -> "bool"
                    "i8" -> "char"
                    "{ i8*, i32 }" -> "string"
                    else -> error("cannot infer type from LLVM type: ${slot.llTy}")
                }

                NamedType(tname)
            } else {
                error("cannot infer type of identifier '${expr.name}' without context")
            }
        }
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

    private fun mirrorToDebugGlobal(fnName: String, varName: String, llTy: String, allocaReg: String) {
        val dbg = "__dbg_${fnName}_${varName}"
        ensureDbgGlobal(dbg, llTy)
        val t = nextTmp()
        fns.appendLine("  $t = load $llTy, ${llTy}* $allocaReg")
        fns.appendLine("  store $llTy $t, ${llTy}* @$dbg")
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