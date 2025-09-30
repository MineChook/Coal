package cli

import codegen.LLVMEmitter
import diagnostics.CoalError
import diagnostics.Diagnostic
import diagnostics.DiagnosticRenderer
import diagnostics.ErrorCode
import diagnostics.Severity
import diagnostics.Span
import front.lexer.Lexer
import front.parser.Parser
import front.types.TypeChecker
import front.types.TypeInfo
import java.nio.file.Files
import java.nio.file.Path

/**
 * Just for tests so they can run in-process
 */
object CompilerFacade {
    data class Result(val success: Boolean, val llvm: String? = null, val stderr: String? = null, val errorCode: String? = null)

    /**
     * Compiles the file at [path] to LLVM IR
     */
    fun compileToLLVM(path: Path, out: Path?): Result {
        val text = Files.readString(path)
        return try {
            val tokens = Lexer(text, path.toString()).lex()
            val ast = Parser(text, tokens, path.toString()).parseProgram()
            val typeInfo = TypeInfo()
            TypeChecker(path.toString(), text, typeInfo).check(ast)

            val llvm = LLVMEmitter(typeInfo, path.toString()).emit(ast)
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