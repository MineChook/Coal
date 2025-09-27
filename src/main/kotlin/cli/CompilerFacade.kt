package cli

import codegen.LLVMEmitter
import diagnostics.CoalError
import diagnostics.Diagnostic
import diagnostics.DiagnosticRenderer
import diagnostics.ErrorCode
import diagnostics.Severity
import diagnostics.Span
import front.Lexer
import front.Parser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Just for tests so they can run in-process
 */
object CompilerFacade {
    data class Result(val success: Boolean, val llvm: String? = null, val stderr: String? = null, val errorCode: String? = null)

    fun compileToLLVM(path: Path, out: Path?): Result {
        val text = Files.readString(path)
        return try {
            val tokens = Lexer(text, path.toString()).lex()
            val ast = Parser(text, tokens, path.toString()).parseProgram()
            val llvm = LLVMEmitter().emit(ast)
            if(out != null) Files.writeString(out, llvm)
            Result(true, llvm)
        } catch(e: CoalError) {
            val rendered = DiagnosticRenderer.render(e.diag, text)
            Result(false, stderr = rendered, errorCode = e.diag.code.code)
        } catch(e: Throwable) {
            val diag = Diagnostic(Severity.ERROR, ErrorCode.Internal, path.toString(), Span(0, 0, 1, 1), listOf(e.message ?: e::class.simpleName ?: "unknown"))
            val rendered = DiagnosticRenderer.render(diag, text)
            Result(false, stderr = rendered, errorCode = diag.code.code)
        }
    }
}