import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

private data class Args(val input: String?, val emitTokens: Boolean)

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
        println("Compiling ${path.toAbsolutePath()}..")
    } catch(e: RuntimeException) {
        System.err.println(e.message)
        exitProcess(2)
    }
}

private fun parseArgs(argv: Array<String>): Args {
    var input: String? = null
    var emitTokens = false

    var i = 0
    while(i < argv.size) {
        when(val a = argv[i]) {
            "--input", "-i" -> {
                if(i + 1 >= argv.size) error("Missing argument for $a")
                input = argv[++i]
            }

            "--emit-tokens" -> emitTokens = true
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

    return Args(input, emitTokens)
}

private fun printUsageAndExit(code: Int) {
    println("Usage: coalc --input <file.coal> [options]")
    println("Options:")
    println("  --input, -i <file>       Specify input file")
    println("  --emit-tokens            Emit tokens")
    println("  --help, -h               Show this help message")
}