package diagnostics

import kotlinx.serialization.Serializable

/**
 * A span in the source code, represented by start and end indices, line number, and column number
 */
@Serializable
data class Span(
    val start: Int,
    val end: Int,
    val line: Int,
    val col: Int
) {
    companion object {
        /**
         * Create a span at a specific line and column, with optional start and end indices
         */
        fun at(line: Int, col: Int, start: Int = 0, end: Int = start) = Span(start, end, line, col)

        /**
         * Merge two spans into one that covers both
         */
        fun merge(a: Span, b: Span): Span = Span(
            minOf(a.start, b.start),
            maxOf(a.end, b.end),
            a.line,
            a.col
        )
    }
}