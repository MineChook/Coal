package unit

import front.lexer.Lexer
import front.parser.Parser
import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class ParserSmokeTest {
    @Test
    fun tokenizesAndParses() {
        val src =
            """
                fn main() {
                    var x = 5
                    if(x == 5 && !(x == 4) || false) { }
                }
            """.trimIndent()

        val toks = Lexer(src, "<mem>").lex()
        toks.shouldNotBeEmpty()

        val ast = Parser(src, toks, "<mem>").parseProgram()
    }
}