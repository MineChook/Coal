package codegen.ir

data class StringRef(
    val arrayBytes: Int,
    val globalName: String,
    val constGEP: String
)
