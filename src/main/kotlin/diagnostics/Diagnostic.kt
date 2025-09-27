package diagnostics

enum class Severity { ERROR, WARNING }

data class Diagnostic(
    val severity: Severity,
    val code: ErrorCode,
    val file: String,
    val span: Span,
    val messageArgs: List<String> = emptyList(),
    val notes: List<String> = emptyList()
) {
    fun formatMessage(): String {
        var msg = code.template
        messageArgs.forEachIndexed { idx, arg ->
            msg = msg.replace("{$idx}", arg)
        }

        return msg
    }
}

class CoalError(val diag: Diagnostic): RuntimeException(diag.formatMessage())