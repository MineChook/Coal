# Contributing to Coal - Beginner's Guide

This guide explains how to contribute to the Coal programming language, assuming you've never programmed before.

## What You'll Learn
- How the Coal compiler is organized
- Where to find different parts of the code
- How to make your first contribution
- How to test your changes

## Understanding the Codebase Structure

### Directory Layout
```
Coal/
├── src/main/kotlin/           # Main compiler source code
│   ├── Main.kt               # Entry point - coordinates everything
│   ├── front/                # Frontend - understands Coal syntax
│   │   ├── Lexer.kt         # Breaks code into tokens
│   │   ├── Parser.kt        # Builds structure from tokens
│   │   └── Token.kt         # Defines what tokens look like
│   ├── ast/                  # Defines program structure  
│   │   └── AST.kt           # Data structures for Coal programs
│   ├── codegen/             # Backend - generates executable code
│   │   ├── LLVMEmitter.kt   # Converts to LLVM IR
│   │   └── ir/              # LLVM IR generation helpers
│   └── cli/                 # Command line interface
├── examples/                 # Example Coal programs
├── coal/                    # Test Coal programs
└── build.gradle.kts         # Build configuration
```

## How the Pieces Work Together

### 1. Main.kt - The Coordinator
This file is like the conductor of an orchestra. It:
- Reads your command line arguments
- Coordinates the lexer, parser, and code generator
- Handles file input/output
- Calls external tools like `clang`

Key functions:
```kotlin
fun main(argv: Array<String>) {
    // Parse command line args
    // Read source file  
    // Run lexer -> parser -> codegen
    // Compile to executable
}
```

### 2. front/Lexer.kt - The Word Breaker
The lexer is like someone who highlights different parts of a sentence with different colors:
- Keywords (`fn`, `var`) → Blue
- Names (`hello`, `myVariable`) → Black  
- Symbols (`+`, `=`, `(`) → Red
- Numbers (`42`, `3.14`) → Green
- Strings (`"hello"`) → Purple

Important parts:
```kotlin
class Lexer(private val source: String) {
    // Reads through source character by character
    // Builds list of tokens
    // Handles keywords, identifiers, symbols, literals
}
```

### 3. front/Parser.kt - The Structure Builder  
The parser is like a grammar teacher who understands sentence structure:
- "This is a variable declaration"
- "This is a function call"
- "This expression has addition"

Key methods:
```kotlin
fun parseProgram(): Program        // Parse entire program
fun parseFnDecl(): FnDecl         // Parse function definition
fun parseVarDecl(): VarDecl       // Parse variable declaration  
fun parseExpr(): Expr             // Parse expressions like 5 + 3
```

### 4. ast/AST.kt - The Data Structures
This defines how Coal programs are represented in memory. Think of it like the skeleton that holds everything together:

```kotlin
// A Coal program is a list of declarations (mostly functions)
data class Program(val decls: List<Decl>)

// A function has a name and body
data class FnDecl(
    val name: String,      // Function name like "main"
    val body: Block        // The code inside { }
)

// A variable declaration  
data class VarDecl(
    val name: String,      // Variable name like "x"
    val init: Expr?        // Initial value like "5 + 3"
)
```

### 5. codegen/LLVMEmitter.kt - The Translator
This converts the structured program into executable code:
- Takes the AST (structured representation)
- Generates LLVM IR (low-level instructions)
- LLVM IR gets compiled to machine code

## Making Your First Contribution

### Start Small: Adding a New Operator

Let's add the `**` (power) operator as an example. You'll need to modify 4 files:

#### Step 1: Add the Token (Lexer.kt)
Find the section where operators are handled and add:
```kotlin
'*' -> {
    if(peek() == '*') {
        advance()  // Consume second *
        add(TokenKind.StarStar, start, line0, col0)
    } else {
        add(TokenKind.Star, start, line0, col0)
    }
}
```

#### Step 2: Add to Token Types (Token.kt)
Add a new token kind:
```kotlin
object StarStar : TokenKind()  // **
```

#### Step 3: Add to AST (AST.kt)  
Add to the binary operators:
```kotlin
enum class BinOp { Add, Sub, Mul, Div, Mod, Pow }
```

#### Step 4: Add Parsing (Parser.kt)
Add precedence for the power operator (higher than multiplication):
```kotlin
private fun precedence(op: BinOp): Int = when(op) {
    BinOp.Add, BinOp.Sub -> 1
    BinOp.Mul, BinOp.Div, BinOp.Mod -> 2  
    BinOp.Pow -> 3  // Higher precedence
}
```

#### Step 5: Add Code Generation (LLVMEmitter.kt)
Generate the appropriate LLVM instruction:
```kotlin
BinOp.Pow -> {
    // Generate call to pow() function
    val powCall = b.call("pow", "double", listOf("double" to lhs, "double" to rhs))
    RValue.ValueReg("double", powCall)
}
```

### Testing Your Changes

1. **Build the compiler**:
   ```bash
   ./gradlew build
   ```

2. **Test with a simple program**:
   Create `test_power.coal`:
   ```coal
   fn main() {
       var result: float = 2.0 ** 3.0
       println(result.toString())
   }
   ```

3. **Run it**:
   ```bash
   ./gradlew run --args="--input test_power.coal"
   ./test_power  # Should print 8.0
   ```

### Debugging Your Changes

If something goes wrong:

1. **Check tokens**:
   ```bash
   ./gradlew run --args="--input test_power.coal --emit-tokens"
   ```
   Look for your `StarStar` token.

2. **Check AST**:
   ```bash  
   ./gradlew run --args="--input test_power.coal --emit-ast"
   ```
   Look for your `Pow` binary operation.

3. **Check generated code**:
   ```bash
   ./gradlew run --args="--input test_power.coal --emit-ir"  
   ```
   Look for power-related LLVM instructions.

## Other Contribution Ideas

### Easy (Good First Issues)
- Add new operators: `&&`, `||`, `==`, `!=`, `<`, `>`, `<=`, `>=`
- Add new keywords: `if`, `else`, `while`, `return`
- Improve error messages in the parser
- Add more example programs

### Medium  
- Add function parameters and return values
- Add control flow statements (if/else, while loops)
- Add arrays or lists
- Add more built-in functions

### Advanced
- Add a type checker to catch errors before code generation
- Add support for user-defined types/structs
- Optimize the generated LLVM IR
- Add debugging information

## General Tips

### Reading the Code
1. Start with `Main.kt` to understand the overall flow
2. Follow a simple example through each stage:
   - Code → Tokens (Lexer) → AST (Parser) → LLVM IR (Codegen)
3. Don't try to understand everything at once - pick one small piece

### Making Changes
1. Always test with simple examples first
2. Make small changes and test frequently  
3. Use the `--emit-tokens`, `--emit-ast`, and `--emit-ir` flags to debug
4. Look at existing similar code for patterns to follow

### Getting Help
- Ask questions in GitHub issues
- Look at how existing operators/features are implemented
- Start with the simplest possible version of a feature

## Understanding LLVM IR

LLVM IR is like assembly language but more readable. Here are common instructions you'll see:

```llvm  
%1 = alloca i32              ; Allocate space for integer
%2 = add i32 5, 3           ; Add two integers: 5 + 3
store i32 %2, ptr %1        ; Store result in allocated space
%3 = load i32, ptr %1       ; Load value from memory
call i32 @printf(ptr %fmt, i32 %3)  ; Call printf function
```

Key concepts:
- `%1`, `%2` are temporary registers (like variables)
- `i32` means 32-bit integer, `ptr` means pointer
- Everything is explicitly typed
- Operations are very basic (add, store, load, call)

## Next Steps

1. **Read the main tutorial**: `LEARN_LANGUAGE_BUILDING.md`
2. **Try the examples**: Run programs in the `examples/` directory
3. **Pick a small feature**: Start with adding a simple operator
4. **Join the community**: Participate in GitHub discussions

Remember: Every expert was once a beginner. The Coal codebase is well-organized and perfect for learning compiler construction. Don't be afraid to experiment and break things - that's how you learn!

## Common Mistakes to Avoid

1. **Trying to understand everything at once** - Focus on one small piece
2. **Not testing incrementally** - Test after each small change  
3. **Ignoring error messages** - They usually tell you exactly what's wrong
4. **Not using the debugging flags** - `--emit-tokens`, `--emit-ast`, `--emit-ir` are your friends
5. **Making changes too big** - Start with tiny modifications

Happy contributing! 🎉