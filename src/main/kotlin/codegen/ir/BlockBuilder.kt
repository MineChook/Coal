package codegen.ir

/**
 * Helper to build LLVM IR instructions within a basic block
 *
 * @param fn the parent function builder
 * @param out the output string builder to append instructions to
 */
class BlockBuilder(
    private val fn: FunctionBuilder,
    private val out: StringBuilder
) {
    /**
     * Allocate space on the stack for a variable of the given type
     *
     * @param ty the LLVM type of the variable to allocate
     * @param name optional name for the allocated variable (for readability)
     * @return the register name of the allocated variable
     */
    fun alloca(ty: String, name: String? = null): String {
        val reg = name?.let { "%$it" } ?: fn.nextTmp()
        out.appendLine("  $reg = alloca $ty")
        return reg
    }

    /**
     * Allocate space on the stack for an array of the given element type and length
     *
     * @param len the number of elements in the array
     * @param elemTy the LLVM type of each element
     * @param name optional name for the allocated array (for readability)
     * @return the register name of the allocated array
     */
    fun allocaArray(len: Int, elemTy: String, name: String? = null): String {
        val reg = name?.let { "%$name" } ?: fn.nextTmp()
        out.appendLine("  $reg = alloca [$len x $elemTy]")
        return reg
    }

    /**
     * Get a pointer to the first element of an array
     *
     * @param basePtr the base pointer to the array
     * @param nelems the number of elements in the array
     * @param elemTy the LLVM type of each element
     * @return the register name of the pointer to the first element
     */
    fun gepFirst(basePtr: String, nelems: Int, elemTy: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = getelementptr inbounds [$nelems x $elemTy], ptr $basePtr, i32 0, i32 0")
        return t
    }

    /**
     * Get a pointer to the first element of a global string
     *
     * @param ref the string reference
     * @return the register name of the pointer to the first character
     */
    fun gepGlobalFirst(ref: StringRef): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = getelementptr inbounds [${ref.arrayBytes} x i8], ptr ${ref.globalName}, i32 0, i32 0")
        return t
    }

    /**
     * Store a value into a pointer
     *
     * @param ty the LLVM type of the value
     * @param value the value to store
     * @param ptrReg the pointer to store into
     */
    fun store(ty: String, value: String, ptrReg: String) {
        out.appendLine("  store $ty $value, ptr $ptrReg")
    }

    /**
     * Load a value from a pointer
     *
     * @param ty the LLVM type of the value to load
     * @param ptrReg the pointer to load from
     * @return the register name of the loaded value
     */
    fun load(ty: String, ptrReg: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = load $ty, ptr $ptrReg")
        return t
    }

    /**
     * Call the printf function with the given format string and arguments
     *
     * @param fmtPtr pointer to the format string
     * @param args pairs of (type, value) for each argument
     * @return the register name of the return value from printf (number of characters printed)
     */
    fun callPrintf(fmtPtr: String, vararg args: Pair<String,String>): String {
        val t = fn.nextTmp()
        val more = if (args.isEmpty()) "" else ", " + args.joinToString(", ") { (ty, op) -> "$ty $op" }
        out.appendLine("  $t = call i32 (ptr, ...) @printf(ptr $fmtPtr$more)")
        return t
    }

    /**
     * Call the snprintf function with the given buffer, capacity, format string, and arguments
     *
     * @param bufPtr pointer to the buffer to write into
     * @param cap the capacity of the buffer
     * @param fmtPtr pointer to the format string
     * @param arg pairs of (type, value) for each argument
     * @return the register name of the return value from snprintf (number of characters written)
     */
    fun callSnprintf(bufPtr: String, cap: Int, fmtPtr: String, vararg arg: Pair<String,String>): String {
        val t = fn.nextTmp()
        val rest = if (arg.isEmpty()) "" else ", " + arg.joinToString(", ") { (ty, op) -> "$ty $op" }
        out.appendLine("  $t = call i32 @snprintf(ptr $bufPtr, i64 $cap, ptr $fmtPtr$rest)")
        return t
    }

    /**
     * Call a function with the given name, return type, and arguments
     *
     * @param name the name of the function to call
     * @param retTy the LLVM return type of the function
     * @param args pairs of (type, value) for each argument
     * @return the register name of the return value from the function call
     */
    fun call(name: String, retTy: String, vararg args: Pair<String,String>): String {
        val t = fn.nextTmp()
        val actuals = if (args.isEmpty()) "" else args.joinToString(", ") { (ty, op) -> "$ty $op" }
        out.appendLine("  $t = call $retTy @$name(${actuals})")
        return t
    }

    /**
     * Zero-extend an integer value from a smaller type to a larger type
     *
     * @param fromTy the original LLVM type of the value
     * @param v the value to extend
     * @param toTy the target LLVM type to extend to
     * @return the register name of the extended value
     */
    fun zext(fromTy: String, v: String, toTy: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = zext $fromTy $v to $toTy")
        return t
    }

    /**
     * Get a byte offset pointer from a base pointer and an i32 offset
     *
     * @param basePtr the base pointer
     * @param offsetI32 the i32 byte offset
     * @return the register name of the resulting pointer
     */
    fun gepByteOffset(basePtr: String, offsetI32: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = getelementptr i8, ptr $basePtr, i32 $offsetI32")
        return t
    }

    /**
     * Pack a string pointer and length into an LLVM { ptr, i32 } struct
     *
     * @param ptrReg the register containing the string pointer
     * @param lenReg the register containing the string length
     * @return the register name of the packed struct
     */
    fun packString(ptrReg: String, lenReg: String): String {
        val a0 = insertValue("{ ptr, i32 }", "undef", "ptr", ptrReg, 0)
        return insertValue("{ ptr, i32 }", a0, "i32", lenReg, 1)
    }

    /**
     * Insert a value into an aggregate (struct or array) at the given index
     *
     * @param aggTy the LLVM type of the aggregate
     * @param aggVal the current value of the aggregate
     * @param elemTy the LLVM type of the element to insert
     * @param elem the value of the element to insert
     * @param index the index at which to insert the element
     * @return the register name of the new aggregate value
     */
    fun insertValue(aggTy: String, aggVal: String, elemTy: String, elem: String, index: Int): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = insertvalue $aggTy $aggVal, $elemTy $elem, $index")
        return t
    }

    /**
     * Extract a value from an aggregate (struct or array) at the given index
     *
     * @param aggTy the LLVM type of the aggregate
     * @param aggVal the current value of the aggregate
     * @param index the index from which to extract the element
     * @return the register name of the extracted element
     */
    fun extractValue(aggTy: String, aggVal: String, index: Int): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = extractvalue $aggTy $aggVal, $index")
        return t
    }

    /**
     * Return from the current function with the given type and value
     *
     * @param ty the LLVM type of the return value
     * @param value the value to return
     */
    fun ret(ty: String, value: String) {
        out.appendLine("  ret $ty $value")
    }

    /**
     * Add two values of the given type
     *
     * @param ty the LLVM type of the values
     * @param a the first value
     * @param b the second value
     * @return the register name of the result
     */
    fun add(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = add $ty $a, $b")
        return t
    }

    /**
     * Subtract two values of the given type
     *
     * @param ty the LLVM type of the values
     * @param a the first value
     * @param b the second value
     * @return the register name of the result
     */
    fun sub(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = sub $ty $a, $b")
        return t
    }

    /**
     * Multiply two values of the given type
     *
     * @param ty the LLVM type of the values
     * @param a the first value
     * @param b the second value
     * @return the register name of the result
     */
    fun mul(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = mul $ty $a, $b")
        return t
    }

    /**
     * Raise a to the power of b, where a and b are integers
     *
     * @param a the base integer value
     * @param b the exponent integer value
     * @return the register name of the result as an integer
     */
    fun pow(a: String, b: String): String {
        val lhsF = sitofp("i32", a, "double")
        val rhsF = sitofp("i32", b, "double")
        val f = fn.nextTmp()
        out.appendLine("  $f = call double @llvm.pow.f64(double $lhsF, double $rhsF)") // double pow
        val result = fptosi("double", f, "i32")
        return result

    }

    /**
     * Signed division of two values of the given type
     *
     * @param ty the LLVM type of the values
     * @param a the dividend
     * @param b the divisor
     * @return the register name of the result
     */
    fun sdiv(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = sdiv $ty $a, $b")
        return t
    }

    /**
     * Signed remainder of two values of the given type
     *
     * @param ty the LLVM type of the values
     * @param a the dividend
     * @param b the divisor
     * @return the register name of the result
     */
    fun srem(ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = srem $ty $a, $b")
        return t
    }

    /**
     * Add two double values
     *
     * @param a the first double value
     * @param b the second double value
     * @return the register name of the result
     */
    fun fadd(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fadd double $a, $b")
        return t
    }

    /**
     * Subtract two double values
     *
     * @param a the first double value
     * @param b the second double value
     * @return the register name of the result
     */
    fun fsub(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fsub double $a, $b")
        return t
    }

    /**
     * Multiply two double values
     *
     * @param a the first double value
     * @param b the second double value
     * @return the register name of the result
     */
    fun fmul(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fmul double $a, $b")
        return t
    }

    /**
     * Divide two double values
     *
     * @param a the dividend
     * @param b the divisor
     * @return the register name of the result
     */
    fun fdiv(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fdiv double $a, $b")
        return t
    }

    /**
     * Raise a to the power of b, where a and b are doubles
     *
     * @param a the base double value
     * @param b the exponent double value
     * @return the register name of the result as a double
     */
    fun fpow(a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = call double @llvm.pow.f64(double $a, double $b)")
        return t
    }

    /**
     * Unconditional branch to the given label
     *
     * @param label the label to branch to
     */
    fun br(label: String) {
        out.appendLine("  br label %$label")
    }

    /**
     * Conditional branch based on a condition
     *
     * @param condTy the LLVM type of the condition (usually i1)
     * @param cond the condition value
     * @param thenLabel the label to branch to if the condition is true
     * @param elseLabel the label to branch to if the condition is false
     */
    fun brCond(condTy: String, cond: String, thenLabel: String, elseLabel: String) {
        out.appendLine("  br $condTy $cond, label %$thenLabel, label %$elseLabel")
    }

    /**
     * Convert a floating-point value to a signed integer
     *
     * @param fromType the LLVM type of the floating-point value
     * @param value the floating-point value to convert
     * @param toType the LLVM type of the target integer
     * @return the register name of the converted integer value
     */
    fun fptosi(fromType: String, value: String, toType: String): String {
        val tempRegister = fn.nextTmp()
        out.appendLine("  $tempRegister = fptosi $fromType $value to $toType")
        return tempRegister
    }

    /**
     * Convert a signed integer value to a floating-point value
     *
     * @param fromType the LLVM type of the integer value
     * @param value the integer value to convert
     * @param toType the LLVM type of the target floating-point value
     * @return the register name of the converted floating-point value
     */
    fun sitofp(fromType: String, value: String, toType: String): String {
        val resultRegister = fn.nextTmp()
        out.appendLine("  $resultRegister = sitofp $fromType $value to $toType")
        return resultRegister
    }

    /**
     * Integer comparison between two values
     *
     * @param pred the comparison predicate (e.g., "eq", "ne", "slt", "sgt", etc.)
     * @param ty the LLVM type of the values being compared
     * @param a the first value
     * @param b the second value
     * @return the register name of the comparison result (i1)
     */
    fun icmp(pred: String, ty: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = icmp $pred $ty $a, $b")
        return t
    }

    /**
     * Floating-point comparison between two double values
     *
     * @param pred the comparison predicate (e.g., "oeq", "one", "olt", "ogt", etc.)
     * @param a the first double value
     * @param b the second double value
     * @return the register name of the comparison result (i1)
     */
    fun fcmp(pred: String, a: String, b: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = fcmp $pred double $a, $b")
        return t
    }

    /**
     * Phi node to select a value based on control flow
     *
     * @param ty the LLVM type of the values
     * @param incoming pairs of (value, label) for each incoming edge
     * @return the register name of the selected value
     */
    fun phi(ty: String, vararg incoming: Pair<String, String>): String {
        val t = fn.nextTmp()
        val inc = incoming.joinToString(", ") { "[ ${it.first}, %${it.second} ]" }
        out.appendLine("  $t = phi $ty $inc")
        return t
    }

    /**
     * Bitwise XOR of an i1 value with true (logical NOT)
     *
     * @param op the i1 value to negate
     * @return the register name of the result
     */
    fun xorI1(op: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = xor i1 $op, true")
        return t
    }

    /**
     * Truncate an integer value from a larger type to a smaller type
     *
     * @param fromTy the original LLVM type of the value
     * @param v the value to truncate
     * @param toTy the target LLVM type to truncate to
     * @return the register name of the truncated value
     */
    fun trunc(fromTy: String, v: String, toTy: String): String {
        val t = fn.nextTmp()
        out.appendLine("  $t = trunc $fromTy $v to $toTy")
        return t
    }

    /**
     * Start a new basic block in the parent function with the given label
     *
     * @param label the label for the new basic block
     */
    fun nextBlock(label: String): BlockBuilder = fn.block(label)
}