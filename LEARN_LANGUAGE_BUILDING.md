# Complete Beginner's Guide to Building Programming Languages

## Table of Contents
1. [What is a Programming Language?](#what-is-a-programming-language)
2. [How Programming Languages Work](#how-programming-languages-work)
3. [The Coal Language Architecture](#the-coal-language-architecture)
4. [Step 1: Lexical Analysis (Tokenization)](#step-1-lexical-analysis-tokenization)
5. [Step 2: Parsing (Syntax Analysis)](#step-2-parsing-syntax-analysis)
6. [Step 3: Abstract Syntax Tree (AST)](#step-3-abstract-syntax-tree-ast)
7. [Step 4: Code Generation](#step-4-code-generation)
8. [Step 5: Putting It All Together](#step-5-putting-it-all-together)
9. [How to Contribute to Coal](#how-to-contribute-to-coal)
10. [Next Steps](#next-steps)

---

## What is a Programming Language?

Think of a programming language like a way to communicate with a computer, similar to how you use English to communicate with people. But computers only understand very simple instructions (like "add two numbers" or "store this value in memory").

A programming language lets you write human-readable code like:
```coal
var message: string = "Hello World"
println(message)
```

And the computer somehow needs to understand what this means and execute it.

## How Programming Languages Work

Building a programming language is like being a translator. Here's the journey your code takes:

1. **Your Code** (what you write): `var x: int = 5 + 3`
2. **Tokens** (individual pieces): `var`, `x`, `:`, `int`, `=`, `5`, `+`, `3`
3. **Parse Tree** (structure): Understanding that this is a variable declaration with a mathematical expression
4. **Machine Code** (what the computer runs): Binary instructions the CPU can execute

Let's see how Coal does this step by step!

## The Coal Language Architecture

Coal has 4 main components, located in these folders:

```
src/main/kotlin/
├── front/          # Frontend (Lexer + Parser)
│   ├── Lexer.kt    # Breaks code into tokens
│   ├── Parser.kt   # Builds structure from tokens
│   └── Token.kt    # Defines what tokens look like
├── ast/            # Abstract Syntax Tree
│   └── AST.kt      # Defines the structure of Coal programs
├── codegen/        # Code Generation
│   └── LLVMEmitter.kt # Converts AST to executable code
└── Main.kt         # Puts everything together
```

## Step 1: Lexical Analysis (Tokenization)

### What is Tokenization?

Imagine you're reading a sentence: "The quick brown fox jumps"

You naturally break it into words: [`The`, `quick`, `brown`, `fox`, `jumps`]

Tokenization does the same thing for code. It takes this:
```coal
var hello: string = "world"
```

And breaks it into these **tokens**:
- `var` (keyword)
- `hello` (identifier/name)
- `:` (colon symbol)
- `string` (type keyword)
- `=` (equals symbol)
- `"world"` (string literal)

### How Coal's Lexer Works

Let's look at `src/main/kotlin/front/Lexer.kt`:

```kotlin
class Lexer(
    private val source: String,        // The code you wrote
    private val fileName: String = "<stdin>"
) {
    private var i = 0                  // Current position in the code
    private var line = 1               // What line we're on
    private var col = 1                // What column we're on
```

The lexer reads your code character by character:

1. **Skip whitespace**: Spaces and newlines don't matter for meaning
2. **Recognize keywords**: `var`, `fn`, `int`, `string`, etc.
3. **Recognize identifiers**: Variable names like `hello`, `myVariable`
4. **Recognize symbols**: `+`, `-`, `=`, `(`, `)`, etc.
5. **Recognize literals**: Numbers like `42`, strings like `"hello"`

### Try It Yourself!

Run this command to see tokenization in action:
```bash
./gradlew run --args="--input coal/main.coal --emit-tokens"
```

You'll see every token the lexer found!

## Step 2: Parsing (Syntax Analysis)

### What is Parsing?

Parsing takes the flat list of tokens and builds **structure**. It's like understanding grammar in English.

The sentence "The cat sat on the mat" has structure:
- Subject: "The cat"
- Verb: "sat"
- Prepositional phrase: "on the mat"

Similarly, `var x: int = 5 + 3` has structure:
- This is a **variable declaration**
- Variable name is `x`
- Type is `int`
- Initial value is a **binary expression**: `5 + 3`

### How Coal's Parser Works

The parser in `src/main/kotlin/front/Parser.kt` follows **grammar rules**:

```kotlin
// A program is a list of function declarations
fun parseProgram(): Program {
    val decls = mutableListOf<Decl>()
    while(!check(TokenKind.EOF)) {
        decls += parseFnDecl()  // Parse each function
    }
    return Program(decls)
}
```

It uses a technique called **recursive descent parsing**:
- Each grammar rule becomes a function
- Functions call each other to build the structure

For example, to parse `5 + 3 * 2`:
1. Parse `5` (number)
2. See `+` (addition operator)  
3. Parse `3 * 2` (which is another expression)
4. Combine them into: `5 + (3 * 2)` (respecting operator precedence)

### Try It Yourself!

Run this to see the parsed structure:
```bash
./gradlew run --args="--input coal/main.coal --emit-ast"
```

## Step 3: Abstract Syntax Tree (AST)

### What is an AST?

An Abstract Syntax Tree (AST) is the **data structure** that represents your program's structure. Think of it like a family tree, but for code.

For `var x: int = 5 + 3`, the AST looks like:

```
VarDecl
├── name: "x"
├── type: NamedType("int")
└── init: Binary
    ├── op: Add
    ├── left: IntLit(5)
    └── right: IntLit(3)
```

### How Coal Defines ASTs

Look at `src/main/kotlin/ast/AST.kt`:

```kotlin
@Serializable data class Program(val decls: List<Decl>)

@Serializable sealed interface Decl
@Serializable data class FnDecl(
    val name: String,
    val params: List<Param>,
    val returnType: TypeRef?,
    val body: Block
) : Decl

@Serializable data class VarDecl(
    val name: String,
    val annotatedType: TypeRef?,
    val init: Expr?,
    val isConst: Boolean
) : Stmt
```

Each AST node is a **data class** that holds the important information:
- `VarDecl` knows the variable name, type, and initial value
- `Binary` knows the operator and left/right expressions
- `FnDecl` knows the function name, parameters, and body

### Why ASTs Matter

The AST is the "universal language" inside your compiler:
- The parser builds it from tokens
- The code generator reads it to produce machine code
- Other tools (like IDEs) can analyze it

## Step 4: Code Generation

### What is Code Generation?

Code generation takes the AST and converts it to something the computer can actually run. Coal generates **LLVM IR** (Intermediate Representation), which then gets compiled to machine code.

Think of it like translation:
- AST: The meaning/structure (universal)
- LLVM IR: Assembly-like code (platform independent)
- Machine Code: Binary instructions (platform specific)

### How Coal Generates Code

The `LLVMEmitter` in `src/main/kotlin/codegen/LLVMEmitter.kt` walks through the AST:

```kotlin
fun emit(prog: Program): String {
    mod = ModuleBuilder()
    
    // Set up runtime functions we need
    mod.declarePrintf()    // For printing
    mod.declareMalloc()    // For memory allocation
    
    // Convert each function in the program
    for(d in prog.decls) {
        when(d) {
            is FnDecl -> emitFn(d)
        }
    }
    
    return mod.toString()  // Return the LLVM IR code
}
```

For a variable declaration like `var x: int = 5 + 3`:

1. **Allocate space**: Create a slot in memory for `x`
2. **Calculate value**: Generate code to compute `5 + 3`
3. **Store result**: Put the result in `x`'s memory slot

### LLVM IR Example

The Coal code:
```coal
var x: int = 5 + 3
```

Becomes this LLVM IR:
```llvm
%x = alloca i32              ; Allocate space for x
%temp = add i32 5, 3         ; Calculate 5 + 3  
store i32 %temp, ptr %x      ; Store result in x
```

### Try It Yourself!

See the generated LLVM IR:
```bash
./gradlew run --args="--input coal/main.coal --emit-ir"
```

## Step 5: Putting It All Together

### The Complete Pipeline

Here's what happens when you run `./gradlew run --args="--input coal/main.coal"`:

1. **Read file**: Load your `.coal` file
2. **Tokenize**: `Lexer` breaks it into tokens
3. **Parse**: `Parser` builds an AST from tokens  
4. **Generate**: `LLVMEmitter` converts AST to LLVM IR
5. **Compile**: LLVM tools convert IR to executable binary
6. **Run**: Your program executes!

### The Main Function

Look at `src/main/kotlin/Main.kt`:

```kotlin
fun main(argv: Array<String>) {
    // 1. Parse command line arguments
    val args = parseArgs(argv)
    
    // 2. Read the source file
    val source = Files.readString(inputPath)
    
    // 3. Tokenize
    val tokens = Lexer(source, fileName).lex()
    
    // 4. Parse  
    val program = Parser(tokens, fileName).parseProgram()
    
    // 5. Generate code
    val ir = LLVMEmitter().emit(program)
    
    // 6. Compile to executable
    Files.writeString(llPath, ir)
    val cmd = listOf("clang", llPath.toString(), "-o", outPath.toString())
    ProcessBuilder(cmd).start().waitFor()
}
```

## How to Contribute to Coal

Now that you understand how Coal works, here are great ways to contribute:

### Easy Contributions
1. **Add new operators**: Modify `Lexer.kt` to recognize new symbols like `&&`, `||`  
2. **Add new keywords**: Add to the `keywords` map in `Lexer.kt`
3. **Improve error messages**: Make `Parser.kt` give better error descriptions
4. **Add examples**: Create more `.coal` files showing features

### Medium Contributions  
1. **Add new data types**: Extend the type system (arrays, structs)
2. **Add control flow**: Implement `if`, `while`, `for` statements
3. **Add functions with parameters**: Extend function definitions
4. **Improve standard library**: Add more built-in functions

### Advanced Contributions
1. **Add a type checker**: Catch type errors before code generation
2. **Optimize generated code**: Make the LLVM IR more efficient  
3. **Add debugging support**: Generate debug information
4. **Create language server**: IDE support with syntax highlighting

### Getting Started Contributing

1. **Pick a small feature**: Start with adding a new operator like `**` for exponentiation
2. **Understand the pipeline**: 
   - Add the token in `Lexer.kt`
   - Add the AST node in `AST.kt`  
   - Add parsing logic in `Parser.kt`
   - Add code generation in `LLVMEmitter.kt`
3. **Test your changes**: Write test `.coal` files
4. **Ask questions**: Use GitHub issues to discuss ideas

### Example: Adding the `**` Power Operator

Here's how you'd add exponentiation:

1. **Lexer**: Add `**` token recognition
2. **AST**: Add `Pow` to the `BinOp` enum  
3. **Parser**: Add precedence rules for `**`
4. **Codegen**: Generate LLVM IR for power operation

This touches every part of the compiler!

## Next Steps

### Learning More About Compilers
- **Books**: "Crafting Interpreters" by Robert Nystrom
- **Courses**: Compiler courses from universities (many free online)  
- **Practice**: Try building a simple calculator first

### Understanding LLVM
- **LLVM Tutorial**: The official LLVM tutorial
- **IR Reference**: Learn more LLVM IR instructions
- **Tools**: Explore `llc`, `lli`, and other LLVM tools

### Advanced Topics
- **Optimization**: How to make code run faster
- **Garbage Collection**: Automatic memory management
- **JIT Compilation**: Compiling code at runtime
- **Domain Specific Languages**: Languages for specific problem areas

### Building Your Own Language

Start simple:
1. **Calculator**: Just numbers and math operators
2. **Variables**: Add variable declarations and assignments  
3. **Functions**: Function definitions and calls
4. **Control Flow**: If statements and loops
5. **Data Structures**: Arrays, structs, objects

Remember: Every complex language started as something simple!

---

## Conclusion

Building programming languages is like building a bridge between human thoughts and machine execution. Coal shows all the essential pieces:

- **Lexer**: Breaks text into meaningful pieces
- **Parser**: Understands the structure and grammar
- **AST**: Represents the program's meaning
- **Code Generator**: Translates to machine instructions

Each piece is simpler than it seems, but powerful when combined. Start small, experiment, and don't be afraid to break things - that's how you learn!

The Coal codebase is well-organized and perfect for learning. Every contribution, no matter how small, helps you understand compilers better and helps the project grow.

Happy language building! 🚀

---

*This guide assumes no prior programming experience. If you have questions about any part, please ask in the GitHub issues - learning is a collaborative process!*