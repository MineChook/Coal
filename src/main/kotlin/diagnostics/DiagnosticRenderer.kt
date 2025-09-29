package diagnostics

/**
 * The class for taking a [Diagnostic] and rendering it to a human-readable string for debugging purposes
 */
object DiagnosticRenderer {
    /**
     * Renders a [Diagnostic] to a human-readable string
     *
     * @param diag The diagnostic to render
     * @param fileText The text of the file the diagnostic is in, used to extract snippets
     * @return A human-readable string representation of the diagnostic
     */
    fun render(diag: Diagnostic, fileText: String): String {
        val header = "${diag.file}:${diag.span.line}:${diag.span.col}: ${diag.severity.name.lowercase()}[${diag.code.code}]: ${diag.formatMessage()}"
        val (snippet, caret) = snippetWithCaret(fileText, diag.span)
        val notes = if(diag.notes.isEmpty()) "" else diag.notes.joinToString("\n") { "note: $it" }

        return buildString {
            appendLine(header)
            if(snippet.isNotEmpty()) {
                appendLine(snippet)
                appendLine(caret)
            }

            if(notes.isNotEmpty()) appendLine(notes)
        }.trimEnd()
    }

    private fun snippetWithCaret(fileText: String, span: Span): Pair<String, String> {
        var lineStart = fileText.lastIndexOf('\n', (span.start - 1).coerceAtLeast(0))
        if(lineStart == -1) lineStart = 0 else lineStart += 1
        val lineEnd = fileText.indexOf('\n', span.start).let { if(it == -1) fileText.length else it }
        val lineText = fileText.substring(lineStart, lineEnd)
        val caret = " ".repeat((span.start - lineStart).coerceAtLeast(0)) + "^"

        return lineText to caret
    }
}