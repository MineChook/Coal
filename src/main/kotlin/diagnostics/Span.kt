package diagnostics

import kotlinx.serialization.Serializable

@Serializable
data class Span(
    val start: Int,
    val end: Int,
    val line: Int,
    val col: Int
) {
    companion object {
        fun at(line: Int, col: Int, start: Int = 0, end: Int = start) = Span(start, end, line, col)

        fun merge(a: Span, b: Span): Span = Span(
            minOf(a.start, b.start),
            maxOf(a.end, b.end),
            a.line,
            a.col
        )
    }
}