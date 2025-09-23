package codegen

import ast.*
import codegen.ir.BlockBuilder
import codegen.ir.ModuleBuilder

class LLVMEmitter {
    private sealed interface RValue {
        data class Immediate(val llTy: String, val text: String) : RValue
        data class ValueReg(val llTy: String, val reg: String) : RValue
        data class Aggregate(val literal: String) : RValue
    }

    private data class LocalSlot(val llTy: String, val reg: String)

    private lateinit var mod: ModuleBuilder
    private val locals = LinkedHashMap<String, LocalSlot>()
    private var currentFn = ""

    fun emit(prog: Program): String {
        mod = ModuleBuilder()
        mod.declarePrintf()

        for(d in prog.decls) {
            when(d) {
                is FnDecl -> emitFn(d)
            }
        }

        return mod.toString()
    }

    private fun emitFn(fn: FnDecl) {
        currentFn = fn.name
        locals.clear()

        val fb = mod.function(fn.name, retTy = "i32").start()
        val b = fb.block("entry")

        for(s in fn.body.stmts) {
            when(s) {
                is VarDecl -> lowerVarDecl(b, fn.name, s)
                is Assign -> lowerAssign(b, fn.name, s)
                is ExprStmt -> valueOfExpr(b, s.expr)
            }
        }

        b.ret("i32", "0")
        fb.end()
    }

    private fun lowerVarDecl(b: BlockBuilder, fnName: String, decl: VarDecl) {
        val tyName = (decl.annotatedType ?: inferType(b, decl.init!!)) as NamedType
        val llTy = llTypeOf(tyName)
        val slotPtr = b.alloca(llTy, decl.name)

        if(decl.init != null) {
            val rhs = valueOfExpr(b, decl.init, llTy)
            when(rhs) {
                is RValue.Immediate -> b.store(rhs.llTy, rhs.text, slotPtr)
                is RValue.ValueReg -> b.store(rhs.llTy, rhs.reg, slotPtr)
                is RValue.Aggregate -> b.store(llTy, rhs.literal, slotPtr)
            }
        } else {
            b.store(llTy, zeroInit(llTy), slotPtr)
        }

        locals[decl.name] = LocalSlot(llTy, slotPtr)
        val t = b.load(llTy, slotPtr)
        val dbgName = "__dbg_${fnName}_${decl.name}"

        mod.global(dbgName, llTy, zeroInit(llTy))
        b.store(llTy, t, "@$dbgName")
    }

    private fun lowerAssign(b: BlockBuilder, fnName: String, asg: Assign) {
        val slot = locals[asg.name] ?: error("Undefined variable ${asg.name}")
        val rhs = valueOfExpr(b, asg.value, slot.llTy)
        when(rhs) {
            is RValue.Immediate -> b.store(rhs.llTy, rhs.text, slot.reg)
            is RValue.ValueReg -> b.store(rhs.llTy, rhs.reg, slot.reg)
            is RValue.Aggregate -> b.store(slot.llTy, rhs.literal, slot.reg)
        }

        val t = b.load(slot.llTy, slot.reg)
        val dbgName = "__dbg_${fnName}_${asg.name}"
        mod.global(dbgName, slot.llTy, zeroInit(slot.llTy))
        b.store(slot.llTy, t, "@$dbgName")
    }

    private fun valueOfExpr(b: BlockBuilder, e: Expr, expected: String? = null): RValue = when(e) {
        is IntLit -> RValue.Immediate("i32", e.value.toString())
        is FloatLit -> RValue.Immediate("double", e.value.toString())
        is BoolLit -> RValue.Immediate("i1", if(e.value) "1" else "0")
        is CharLit -> RValue.Immediate("i8", e.value.toString())
        is StringLit -> {
            val fmt = mod.internCString(e.value)
            val len = utf8Len(e.value)
            RValue.Aggregate("{ i8* ${fmt.gep}, i32 $len }")
        }

        is Ident -> {
            val slot = locals[e.name] ?: error("Undefined variable ${e.name}")
            val t = b.load(slot.llTy, slot.reg)
            RValue.ValueReg(slot.llTy, t)
        }

        is Binary -> lowerBinary(b, e)
        is Call -> when(e.callee) {
            "print" -> lowerBuiltinPrint(b, e.args, false)
            "println" -> lowerBuiltinPrint(b, e.args, true)
            else -> error("Unknown function ${e.callee}")
        }
    }

    private fun lowerBinary(b: BlockBuilder, bin: Binary): RValue {
        val ty = (inferType(b, bin) as NamedType).name
        val llTy = if(ty == "float") "double" else "i32"

        fun asOp(rv: RValue): String = when(rv) {
            is RValue.Immediate -> rv.text
            is RValue.ValueReg -> rv.reg
            is RValue.Aggregate -> error("Cannot use aggregate in binary operation")
        }

        val lhs = valueOfExpr(b, bin.left, llTy)
        val rhs = valueOfExpr(b, bin.right, llTy)

        val res = when(bin.op) {
            BinOp.Add -> if (llTy == "i32") b.add(llTy, asOp(lhs), asOp(rhs)) else b.fadd(asOp(lhs), asOp(rhs))
            BinOp.Sub -> if (llTy == "i32") b.sub(llTy, asOp(lhs), asOp(rhs)) else b.fsub(asOp(lhs), asOp(rhs))
            BinOp.Mul -> if (llTy == "i32") b.mul(llTy, asOp(lhs), asOp(rhs)) else b.fmul(asOp(lhs), asOp(rhs))
            BinOp.Div -> if (llTy == "i32") b.sdiv(llTy, asOp(lhs), asOp(rhs)) else b.fdiv(asOp(lhs), asOp(rhs))
            BinOp.Mod -> {
                require(llTy == "i32") { "mod supported only for int" }
                b.srem(llTy, asOp(lhs), asOp(rhs))
            }
        }

        return RValue.ValueReg(llTy, res)
    }

    private fun lowerBuiltinPrint(b: BlockBuilder, args: List<Expr>, newline: Boolean): RValue {
        require(args.size == 1) { "print/println takes exactly one argument" }
        val rv = valueOfExpr(b, args[0])

        if(rv is RValue.Aggregate) {
            val fmtG = mod.internCString(if (newline) "%s\n" else "%s")
            val ptr = b.extractValue("{ i8*, i32 }", rv.literal, 0)
            b.callPrintf(fmtG.gep, "i8*" to ptr)
            return RValue.Immediate("i32", "0")
        }

        val (ty, op) = asOperand(rv)
        val fmt = when(ty) {
            "i32", "i1", "i8" -> if(newline) "%d\n" else "%d"
            "double" -> if(newline) "%f\n" else "%f"
            else -> error("unsupported print type: $ty")
        }

        val fmtG = mod.internCString(fmt)
        val argTy = when(ty) {
            "i1", "i8" -> "i32"
            else -> ty
        }

        b.callPrintf(fmtG.gep, argTy to op)
        return RValue.Immediate("i32", "0")
    }

    // utils
    private fun inferType(b: BlockBuilder, expr: Expr): TypeRef = when(expr) {
        is IntLit -> NamedType("int")
        is FloatLit -> NamedType("float")
        is BoolLit -> NamedType("bool")
        is CharLit -> NamedType("char")
        is StringLit -> NamedType("string")
        is Ident -> {
            val slot = locals[expr.name] ?: error("unknown ident in inferType: ${expr.name}")
            when(slot.llTy) {
                "i32" -> NamedType("int")
                "double" -> NamedType("float")
                "i1" -> NamedType("bool")
                "i8" -> NamedType("char")
                "{ i8*, i32 }" -> NamedType("string")
                else -> error("cannot infer from $slot")
            }
        }

        is Binary -> {
            val lt = inferType(b, expr.left) as NamedType
            val rt = inferType(b, expr.right) as NamedType
            require(lt == rt) { "type mismatch: $lt vs $rt" }
            require(lt.name == "int" || lt.name == "float") { "unsupported binop on ${lt.name}" }
            lt
        }

        is Call -> error("cannot infer call type yet")
    }

    private fun llTypeOf(t: NamedType): String = when(t.name) {
        "int" -> "i32"
        "float" -> "double"
        "bool" -> "i1"
        "char" -> "i8"
        "string" -> "{ i8*, i32 }"
        else -> error("unknown type ${t.name}")
    }

    private fun zeroInit(ty: String) = when(ty) {
        "i1", "i8", "i32" -> "0"
        "double" -> "0.0"
        "{ i8*, i32 }" -> "{ i8* null, i32 0 }"
        else -> error("no zero init for $ty")
    }

    private fun asOperand(rv: RValue): Pair<String, String> = when(rv) {
        is RValue.Immediate -> rv.llTy to rv.text
        is RValue.ValueReg -> rv.llTy to rv.reg
        is RValue.Aggregate -> error("cannot use aggregate as operand")
    }

    private fun utf8Len(s: String) = s.toByteArray(Charsets.UTF_8).size
}