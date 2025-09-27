package cli

import codegen.LLVMEmitter
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
    data class Result(val success: Boolean, val llvm: String?, val stdout: String, val stderr: String, val error: Throwable?)

    fun compileToLLVM(input: Path, keepLLOut: Path? = null): Result {
        val src = input.readText()
        return runCatching {
            val tokens = Lexer(src, input.toString()).lex()
            val program = Parser(tokens, input.toString()).parseProgram()
            val ir = LLVMEmitter().emit(program)

            if(keepLLOut != null) {
                Files.createDirectories(keepLLOut.parent)
                keepLLOut.writeText(ir)
            }

            Result(true, ir, "", "", null)
        }.getOrElse { t ->
            Result(false, null, "", (t.message ?: t.toString()), t)
        }
    }
}