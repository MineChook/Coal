package codegen.ir

/**
 * Manages a table of interned strings for LLVM IR generation
 * Each unique string is stored once in the global section, and can be referenced
 *
 * @param globals StringBuilder to which global string definitions are appended
 */
class StringTable(private val globals: StringBuilder) {
    private var counter = 0
    private val interned = LinkedHashMap<String, StringRef>()

    /**
     * Interns a string, returning a StringRef that can be used in LLVM IR
     * If the string has already been interned, returns the existing StringRef
     *
     * @param s The string to intern
     * @return StringRef containing length, global name, and GEP for the string
     */
    fun intern(s: String): StringRef {
        return interned.getOrPut(s) {
            val encoded = encodeCString(s)
            val nBytes = utf8BytesLen(s) + 1
            val gname = "@.str.${counter++}"
            globals.appendLine("$gname = private unnamed_addr constant [$nBytes x i8] c\"$encoded\\00\"")

            val constGEP = "getelementptr inbounds ([$nBytes x i8], ptr $gname, i32 0, i32 0)"
            StringRef(nBytes, gname, constGEP)
        }
    }

    private fun utf8BytesLen(s: String) = s.toByteArray(Charsets.UTF_8).size
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
}