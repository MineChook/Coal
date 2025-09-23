package codegen.ir

class StringTable(private val globals: StringBuilder) {
    private var counter = 0
    private val interned = LinkedHashMap<String, StringRef>()

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