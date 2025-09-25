# Coal Language Examples

This directory contains example programs that demonstrate various features of the Coal programming language. These examples are designed to help beginners understand how Coal works.

## Running the Examples

To run any example, use:
```bash
./gradlew run --args="--input examples/filename.coal"
```

For example:
```bash
./gradlew run --args="--input examples/01_simple_math.coal"
```

## Examples Overview

### 01_simple_math.coal
Demonstrates:
- Variable declarations with type annotations
- Basic arithmetic operations (+, -, *, /, %)
- Converting numbers to strings with `.toString()`
- Printing output with `println()`

### 02_string_operations.coal  
Demonstrates:
- String variable declarations
- String concatenation with `+` operator
- Building complex messages from multiple strings

### 03_type_conversions.coal
Demonstrates:
- Different data types (int, float, bool, char)
- Converting values to strings with `.toString()`
- Converting between numeric types with `.toInt()` and `.toFloat()`

## Exploring the Compiler

You can also explore how the compiler processes these examples:

### See the tokens:
```bash
./gradlew run --args="--input examples/01_simple_math.coal --emit-tokens"
```

### See the Abstract Syntax Tree (AST):
```bash
./gradlew run --args="--input examples/01_simple_math.coal --emit-ast"
```

### See the generated LLVM IR:
```bash
./gradlew run --args="--input examples/01_simple_math.coal --emit-ir"
```

## Next Steps

After understanding these examples:
1. Try modifying them to see what happens
2. Create your own simple Coal programs
3. Read the main tutorial in `LEARN_LANGUAGE_BUILDING.md`
4. Explore the compiler source code in `src/main/kotlin/`

Remember: The best way to learn is by experimenting!