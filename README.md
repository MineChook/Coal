# Coal

## About
**Coal is a programming language designed for simplicity and ease of use. It is a compiled language, meaning that it runs faster than python. Coal also has the low-level accessibility like C.**

> Bullet is currently in development and is not ready for production use

### Features
- Simple syntax
- Fast execution
- Low-level accessibility
- Easy to learn

## Syntax
### Variables

**Variables are declared by using the var keyword like in kotlin:**
```coal
var x: int = 5
var y: string = "Hello, World!"
```

*At the moment you cannot reassign a variable. This will be added soon*

When creating a variable, you must specify the type of the variable. Coal is a statically typed language, meaning that the type of a variable cannot be changed after it is declared. 

**See the [Data Types](#data-types) section for more information.**

### Data Types
**Coal supports the following data types:**
- int
- float
- string
- bool
- char

**You can also Convert between Data Types:**

```coal
var int: int = 1    
var stringOfInt: string = int.toString
var floatOfInt: float = int.toFloat()
```

We hope to have more in the future!

### Functions

**Functions are declared using the fn keyword:**
```coal
fn main() {
    // code here
}
```

At the moment, you cannot make any other functions other than the main function. (We plan to add this soon.)

**Important:** All coal files always start with a main function, which is the entry point of the program.

### System Out and In
**To print to the console, use the println function:**
```coal
println("Hello, World!")
```

**To take input from the user, use the input function:**
```coal
    
```

Coming soon!

## Math
**Coal supports the following mathematical operations:**
- Addition: +
- Subtraction: -
- Multiplication: *
- Division: /
- Modulus: %
- Power ^

More coming soon!

## How to Compile and Run
**To compile and run a coal program, use the following command:**
```bash
    ./gradlew run --args="--input path/to/your/file.coal"
    
```

This will compile a coal program that will appear in the same directory. It is important to note that when building if there is an error in your code, the compiler will not tell you what is wrong. (We hope to add this feature however we don't know.)

## More to come!