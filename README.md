# ArbuzScript Programming Language

## Project Overview
ArbuzScript is a strictly typed, object-oriented programming language designed for systems programming and the advanced study of compiler architecture. This project represents a full-scale implementation of the language "from scratch": from lexical analysis to machine code generation via transpilation to the C programming language.

The core distinguishing feature of the project is the manual implementation of all compilation stages without the use of third-party parser generators. This approach guarantees complete transparency of the internal algorithmic processes.

## System Architecture
The compiler is developed in Java and utilizes a pipelined data processing architecture:

1. **Lexical Analysis (Lexer):** Transformation of source text into a stream of tokens using a Deterministic Finite Automaton (DFA). The use of a transition matrix ensures O(n) linear time complexity.
2. **Syntax Analysis (Parser):** Construction of an Abstract Syntax Tree (AST) using the Recursive Descent method with full support for LL(1) grammar.
3. **Semantic Analysis (TypeChecker):** Static type checking, variable scope control, and strict protection of constants from modification during the compilation stage.
4. **Transpilation (Transpiler):** Generation of equivalent source code in C using the Visitor design pattern. Object-oriented language constructs are lowered to flat structures (`struct`) and global functions with implicit `this` pointer passing.
5. **Compilation (GCC):** Automatic invocation of the system C compiler to create a platform-dependent binary executable file (.exe).

## Language Specification

### Data Types
The language provides strict static typing:
* `int` — 32-bit signed integer.
* `float` — 64-bit floating-point number.
* `bool` — logical type (true / false).
* `str` — string type.
* `byte` — 8-bit unsigned integer, optimized for binary operations.

### Memory and Data Structures
The language strictly adheres to a static memory allocation policy:
* **Arrays:** Fixed-size arrays are supported without hidden dynamic memory allocations (Zero Allocation Policy). Examples: `byte[]`, `int[][]`.
* **Classes:** Implement encapsulation of data and behavior. Internally transpiled into C language structures.

### Variable Declaration
Access to data is strictly regulated by mutability rules:
* `let` — declaration of an immutable constant. An attempt to redefine the value causes a compilation error.
* `var` — declaration of a mutable variable.

**Example:**
```text
let VERSION: int = 3;
var buffer: byte[] = [192, 168, 1, 1];
```

### Control Flow
Classic execution flow control structures are supported:
* Conditional operators: `if` / `else`.
* Pre-condition loops: `while`.
* Iterative loops: `for`.

### Operators
To improve code readability and prevent syntactic ambiguity, text operators are used instead of standard symbolic ones:
* Logical: `and`, `or`, `not`.
* Bitwise: `xor`, `shl` (shift left), `shr` (shift right), `bit_and`, `bit_or`.

**Example:**
```text
var flags: int = 1 shl 3;
var secret: int = 255 bit_and 15;
```

### Object-Oriented Programming
Class declaration requires explicit specification of types for both fields and method return values.

**Example:**
```text
class Entity {
    var hp: int;
    
    func init(h: int) -> void {
        this.hp = h;
    }

    func takeDamage(dmg: int) -> void {
        this.hp = this.hp - dmg;
        if (this.hp < 0) {
            this.hp = 0;
        }
    }
}
```

## Standard Library (StdLib)
The language includes a basic set of built-in functions that are directly transpiled into C standard library calls:
* **Input/Output:** `print()`, `println()`, `read_int()`, `read_str()`.
* **Math:** `abs()`, `sqrt()`, `pow()`, `min()`, `max()`.
* **System:** `time()` (current UNIX timestamp), `sizeof()` (memory size calculation).
* **Type Conversion:** `to_str()`, `to_int()`.

## Tooling, IDE, and Build Requirements
To work with the compiler and the integrated development environment, the following are required:
1. **Java Development Kit (JDK) 17+** — runtime environment for the compiler and graphical interface.
2. **GCC (MSYS2 / MinGW-w64)** — system C compiler, which must be added to the operating system's `PATH` environment variable.

### ArbuzScript Studio (IDE)
The project includes its own graphical Integrated Development Environment (IDE) — ArbuzScript Studio. This environment allows developers to seamlessly write, compile, and execute code. The IDE is equipped with a built-in simulated console panel that intercepts and displays compilation logs, the execution process, and the program's standard output in real-time directly within the user interface.

### Command Line Usage
For manual compilation of a source file (.arb), use the following command:
```text
java -jar arbuzc.jar script.arb
```
After successfully passing lexical, syntactic, and semantic checks, the compiler generates an intermediate `output.c` file and initiates the build of the `program.exe` binary executable.

## Future Development and Cryptography
The vector for further development of the ArbuzScript language is aimed at implementing functionality for cryptographic applications. Future updates plan to expand operations with the `byte` type, introduce native cryptographic primitives, and add advanced tools for low-level memory manipulation. These innovations are intended for the design and analysis of encryption algorithms and ensuring information security.
