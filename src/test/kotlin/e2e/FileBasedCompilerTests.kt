package e2e

import cli.CompilerFacade
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileBasedCompilerTests {
    private val resRoot: Path = Paths.get("src", "test", "resources", "programs")
    private val passDir: Path = resRoot.resolve("pass")
    private val failDir: Path = resRoot.resolve("fail")
    private val tmpLL: Path = Paths.get("build", "test-llvm")

    companion object {
        @JvmStatic
        fun passPrograms(): Stream<Path> =
            Files.walk(Paths.get("src", "test", "resources", "programs", "pass"))
                .filter { Files.isRegularFile(it) && it.extension == "coal" }
                .sorted()

        @JvmStatic
        fun failPrograms(): Stream<Path> =
            Files.walk(Paths.get("src", "test", "resources", "programs", "fail"))
                .filter { Files.isRegularFile(it) && it.extension == "coal" }
                .sorted()
    }

    @BeforeAll
    fun ensureDirs() {
        Files.createDirectories(tmpLL)
    }

    @ParameterizedTest(name = "PASS: {0}")
    @MethodSource("passPrograms")
    fun compilesProgramsInPass(path: Path) {
        val llOut = tmpLL.resolve("${path.nameWithoutExtension}.ll")
        val result = CompilerFacade.compileToLLVM(path, llOut)

        if(!result.success) {
            println("PASS test failed for: $path")
            println("STDERR:\n${result.stderr}")
        }

        result.success shouldBe true
        result.error shouldBe null
        result.llvm!!.isNotBlank() shouldBe true
    }

    @ParameterizedTest(name = "FAIL: {0}")
    @MethodSource("failPrograms")
    fun failsProgramsInFail(path: Path) {
        val expectedErrFile = path.resolveSibling("${path.nameWithoutExtension}.err")
        require(expectedErrFile.exists()) { "Missing expected error file: $expectedErrFile" }

        val errNeedle = expectedErrFile.readText(StandardCharsets.UTF_8).trim()
        errNeedle.isNotBlank() shouldBe true

        val result = CompilerFacade.compileToLLVM(path, null)
        result.success shouldBe false
        result.stderr shouldContain errNeedle
    }
}