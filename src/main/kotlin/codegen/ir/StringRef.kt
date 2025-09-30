package codegen.ir

/**
 * Represents a reference to a string in the generated code
 *
 * @param arrayBytes The size of the string in bytes (including null terminator)
 * @param globalName The name of the global variable holding the string
 * @param constGEP The LLVM getelementptr instruction to access the string
 */
data class StringRef(
    val arrayBytes: Int,
    val globalName: String,
    val constGEP: String
)
