package codegen.ir

class BlockBuilder(
    private val fn: FunctionBuilder,
    private val out: StringBuilder
) {
    fun alloca(ty: String, name: String? = null): String {
        val reg = name?.let { "%$it" } ?: fn.nextTmp()
        out.appendLine("  $reg = alloca $ty")
        return reg
    }

    fun store(ty: String, value: String, ptr: String) {
        out.appendLine("  store $ty $value, $ty* $ptr")
    }

    fun load(ty: String, ptr: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = load $ty, $ty* $ptr")
        return t
    }

    fun ret(ty: String, value: String) {
        out.appendLine("  ret $ty $value")
    }

    fun retVoid() {
        out.appendLine("  ret void")
    }

    fun add(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = add $ty $a, $b")
        return t
    }

    fun sub(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = sub $ty $a, $b")
        return t
    }

    fun mul(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = mul $ty $a, $b")
        return t
    }

    fun sdiv(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = sdiv $ty $a, $b")
        return t
    }

    fun srem(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = srem $ty $a, $b")
        return t
    }

    fun fadd(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fadd double $a, $b")
        return t
    }

    fun fsub(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fsub double $a, $b")
        return t
    }

    fun fmul(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fmul double $a, $b")
        return t
    }

    fun fdiv(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fdiv double $a, $b")
        return t
    }

    fun callPrintf(fmtGep: String, vararg args: Pair<String,String>): String {
        val t = fn.nextTmp()
        val argsText = if(args.isEmpty()) "" else ", " + args.joinToString(", ") { (ty, op) -> "$ty $op" }
        out.appendLine("  $t = call i32 (i8*, ...) @printf(i8* $fmtGep$argsText)")
        return t
    }

    fun extractValue(aggTy: String, aggVal: String, index: Int): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = extractvalue $aggTy $aggVal, $index")
        return t
    }

    fun br(label: String) {
        out.appendLine("  br label %$label")
    }

    fun brCond(condTy: String, cond: String, thenLabel: String, elseLabel: String) {
        out.appendLine("  br $condTy $cond, label %$thenLabel, label %$elseLabel")
    }
}