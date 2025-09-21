import cli.CLIArguments.parseArgs
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
        println("Compiling ${path.toAbsolutePath()}..")
    } catch(e: RuntimeException) {
        System.err.println(e.message)
        exitProcess(2)
    }
}