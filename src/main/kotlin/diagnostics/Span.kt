package diagnostics

data class Span(
    val start: Int,
    val end: Int,
    val line: Int,
    val col: Int
)
