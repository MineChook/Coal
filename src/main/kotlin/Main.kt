import ast.Program
import cli.CLIArguments.parseArgs
import front.Lexer
import front.Parser
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(argv: Array<String>) {
    val args = parseArgs(argv)
    val path = Path.of(args.input!!)
    val source = try {
        Files.readString(path)
    } catch(e: Exception) {
        System.err.println("Error reading file ${path.toAbsolutePath()}: ${e.message}")
        return
    }

    try {
        val tokens = Lexer(source, path.fileName.toString()).lex()
        if(args.emitTokens) {
            println("TOKENS (" + tokens.size + "):")
            for(token in tokens) {
                val pos = "${token.span.line}:${token.span.col}"
                val kind = token.kind.toString()
                val lex = token.lexeme.replace("\n", "\\n").replace("\t", "\\t")
                println("  $pos\t$kind\t'$lex'")
            }

            return
        }

        if(args.emitJsonTokens) {
            val json = buildString {
                append("[\n")
                tokens.forEachIndexed { i, token ->
                    val pos = "${token.span.line}:${token.span.col}"
                    val kind = token.kind.toString()
                    val lex = token.lexeme.replace("\n", "\\n").replace("\t", "\\t").replace("\"", "\\\"")
                    append("  { \"pos\": \"$pos\", \"kind\": \"$kind\", \"lexeme\": \"$lex\" }")
                    if(i < tokens.size - 1) append(",")
                    append("\n")
                }

                append("]\n")
            }

            println(json)
            return
        }

        val program: Program = Parser(tokens, path.fileName.toString()).parseProgram()
        if(args.emitAst) {
            val json = Json { prettyPrint = true; classDiscriminator = "kind" }
            println(json.encodeToString(Program.serializer(), program))
            return
        }

        println("Parsed successfully: ${path.toAbsolutePath()}")
    } catch(e: RuntimeException) {
        System.err.println(e.message)
        exitProcess(2)
    }
}