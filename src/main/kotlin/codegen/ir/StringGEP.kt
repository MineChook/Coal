package codegen.ir

data class StringGEP(
    val arrayBytes: Int,
    val globalName: String,
    val gep: String
)
