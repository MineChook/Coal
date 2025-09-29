package diagnostics

enum class Severity { ERROR, WARNING }

/**
 * Holds information about a specific error
 *
 * @param severity The severity of the error (e.g., ERROR, WARNING)
 * @param code The error code associated with this diagnostic
 * @param file The file where the error occurred
 * @param span The span (location) in the file where the error occurred
 * @param messageArgs Arguments to be substituted into the error message template
 * @param notes Additional notes or context about the error
 */
data class Diagnostic(
    val severity: Severity,
    val code: ErrorCode,
    val file: String,
    val span: Span,
    val messageArgs: List<String> = emptyList(),
    val notes: List<String> = emptyList()
) {
    /**
     * Formats the error message by substituting the message arguments into the error code template
     */
    fun formatMessage(): String {
        var msg = code.template
        messageArgs.forEachIndexed { idx, arg ->
            msg = msg.replace("{$idx}", arg)
        }

        return msg
    }
}

class CoalError(val diag: Diagnostic): RuntimeException(diag.formatMessage())