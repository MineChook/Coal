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

    fun allocaArray(len: Int, elemTy: String, name: String? = null): String {
        val reg = name?.let { "%$name" } ?: fn.nextTmp()
        out.appendLine("  $reg = alloca [$len x $elemTy]")
        return reg
    }

    fun gepFirst(basePtr: String, nelems: Int, elemTy: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = getelementptr inbounds [$nelems x $elemTy], ptr $basePtr, i32 0, i32 0")
        return t
    }

    fun gepGlobalFirst(ref: StringRef): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = getelementptr inbounds [${ref.arrayBytes} x i8], ptr ${ref.globalName}, i32 0, i32 0")
        return t
    }

    fun store(ty: String, value: String, ptrReg: String) {
        out.appendLine("  store $ty $value, ptr $ptrReg")
    }

    fun load(ty: String, ptrReg: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = load $ty, ptr $ptrReg")
        return t
    }

    fun callPrintf(fmtPtr: String, vararg args: Pair<String,String>): String {
        val t = fn.nextTmp()
        val more = if (args.isEmpty()) "" else ", " + args.joinToString(", ") { (ty, op) -> "$ty $op" }
        out.appendLine("  $t = call i32 (ptr, ...) @printf(ptr $fmtPtr$more)")
        return t
    }

    fun callSnprintf(bufPtr: String, cap: Int, fmtPtr: String, vararg arg: Pair<String,String>): String {
        val t = fn.nextTmp()
        val rest = if (arg.isEmpty()) "" else ", " + arg.joinToString(", ") { (ty, op) -> "$ty $op" }
        out.appendLine("  $t = call i32 @snprintf(ptr $bufPtr, i64 $cap, ptr $fmtPtr$rest)")
        return t
    }

    fun call(name: String, retTy: String, vararg args: Pair<String,String>): String {
        val t = fn.nextTmp()
        val actuals = if (args.isEmpty()) "" else args.joinToString(", ") { (ty, op) -> "$ty $op" }
        out.appendLine("  $t = call $retTy @$name(${actuals})")
        return t
    }

    fun zext(fromTy: String, v: String, toTy: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = zext $fromTy $v to $toTy")
        return t
    }

    fun gepByteOffset(basePtr: String, offsetI32: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = getelementptr i8, ptr $basePtr, i32 $offsetI32")
        return t
    }

    fun packString(ptrReg: String, lenReg: String): String {
        val a0 = insertValue("{ ptr, i32 }", "undef", "ptr", ptrReg, 0)
        return insertValue("{ ptr, i32 }", a0, "i32", lenReg, 1)
    }

    fun insertValue(aggTy: String, aggVal: String, elemTy: String, elem: String, index: Int): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = insertvalue $aggTy $aggVal, $elemTy $elem, $index")
        return t
    }

    fun extractValue(aggTy: String, aggVal: String, index: Int): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = extractvalue $aggTy $aggVal, $index")
        return t
    }

    fun ret(ty: String, value: String) {
        out.appendLine("  ret $ty $value")
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

    fun pow(a: String, b: String): String {
        val lhsF = sitofp("i32", a, "double")
        val rhsF = sitofp("i32", b, "double")
        val f = fn.nextTmp()
        out.appendLine("  $f = call double @llvm.pow.f64(double $lhsF, double $rhsF)") // double pow
        val result = fptosi("double", f, "i32")
        return result

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

    fun sadd(a: String, b: String): String {
        val t = fn.nextTmp()

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

    fun fpow(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = call double @llvm.pow.f64(double $a, double $b)")
        return t
    }

    fun br(label: String) {
        out.appendLine("  br label %$label")
    }

    fun brCond(condTy: String, cond: String, thenLabel: String, elseLabel: String) {
        out.appendLine("  br $condTy $cond, label %$thenLabel, label %$elseLabel")
    }

    // float to int
    fun fptosi(fromType: String, value: String, toType: String): String {
        val tempRegister = fn.nextTmp()
        out.appendLine("  $tempRegister = fptosi $fromType $value to $toType")
        return tempRegister
    }

    // int to float
    fun sitofp(fromType: String, value: String, toType: String): String {
        val resultRegister = fn.nextTmp()
        out.appendLine("  $resultRegister = sitofp $fromType $value to $toType")
        return resultRegister
    }

    fun icmp(pred: String, ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = icmp $pred $ty $a, $b")
        return t
    }

    fun fcmp(pred: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fcmp $pred double $a, $b")
        return t
    }

    fun phi(ty: String, vararg incoming: Pair<String, String>): String {
        val t = fn.nextTmp()
        val inc = incoming.joinToString(", ") { "[ ${it.first}, %${it.second} ]" }
        out.appendLine("  $t = phi $ty $inc")
        return t
    }

    fun xorI1(op: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = xor i1 $op, true")
        return t
    }

    fun nextBlock(label: String): BlockBuilder = fn.block(label)
}