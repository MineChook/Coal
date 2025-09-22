package cli

import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

data class Args(
    val input: String?,
    val output: String?,
    val emitTokens: Boolean,
    val emitJsonTokens: Boolean,
    val emitAst: Boolean,
    val emitIR: Boolean
)

object CLIArguments {
    fun parseArgs(argv: Array<String>): Args {
        var input: String? = null
        var output: String? = null
        var emitTokens = false
        var emitJsonTokens = false
        var emitAst = false
        var emitIR = false

        var i = 0
        while(i < argv.size) {
            when(val a = argv[i]) {
                "--input", "-i" -> {
                    if(i + 1 >= argv.size) error("Missing argument for $a")
                    input = argv[++i]
                }

                "--output", "-o" -> {
                    if(i + 1 >= argv.size) error("Missing argument for $a")
                    output = argv[++i]
                }

                "--emit-tokens" -> emitTokens = true
                "--emit-json-tokens" -> emitJsonTokens = true
                "--emit-ast" -> emitAst = true
                "--emit-ir" -> emitIR = true

                "--help", "-h" -> {
                    printUsageAndExit(0)
                }

                else -> {
                    System.err.println("Unknown argument: $a")
                    printUsageAndExit(1)
                }
            }
            i++
        }

        if(input == null) {
            System.err.println("Error: --input is required")
            printUsageAndExit(1)
        }

        return Args(input, output, emitTokens, emitJsonTokens, emitAst, emitIR)
    }

    private fun printUsageAndExit(code: Int) {
        println("Usage: coalc --input <file.coal> [options]")
        println("Options:")
        println("  --input, -i <file>       Specify input file")
        println("  --output, -o <file>      Specify output file")
        println("  --emit-tokens            Emit tokens")
        println("  --emit-json-tokens       Dump tokens as JSON")
        println("  --emit-ast               Emit AST as JSON")
        println("  --emit-ir                Emit IR as JSON")
        println("  --help, -h               Show this help message")

        exitProcess(code)
    }

    fun writeOut(args: Args, content: String) {
        if(args.output != null) {
            Files.writeString(Path.of(args.output), content)
        } else {
            println(content)
        }
    }
}