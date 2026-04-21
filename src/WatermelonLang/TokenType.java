package WatermelonLang;

public enum TokenType {

    // --- 1. KEYWORDS (CORE) ---
    LET,        // let
    VAR,        // var
    FUNC,       // func
    CLASS,      // class
    THIS,       // this
    NEW,        // new
    STRUCT,     // struct (Legacy)
    RETURN,     // return
    PUB,        // pub    (For modules, Section 10)
    IMPORT,     // import (For modules, Section 10)

    // --- 2. DATA TYPES ---
    INT_TYPE,   // int
    FLOAT_TYPE, // float (Added: Section 3)
    BOOL_TYPE,  // bool
    STR_TYPE,   // str
    CHAR_TYPE,  // char  (Added: Section 3)
    BYTE_TYPE,  // byte  (Critical for crypto, Section 3)

    // --- 3. FLOW CONTROL ---
    IF,         // if
    ELSE,       // else
    WHILE,      // while
    FOR,        // for      (Added: Section 7)
    IN,         // in       (For for-in loop, Section 7)
    BREAK,      // break    (Added: Section 7)
    CONTINUE,   // continue (Added: Section 7)

    // --- 4. LITERALS ---
    IDENT,          // Identifiers (variable names, functions)
    INT_LITERAL,    // 123
    FLOAT_LITERAL,  // 12.34 (Added)
    STRING_LITERAL, // "text"
    CHAR_LITERAL,   // 'c'   (Added)
    TRUE,           // true
    FALSE,          // false

    // --- 5. ARITHMETIC OPERATORS ---
    PLUS,           // +
    MINUS,          // -
    STAR,           // *
    SLASH,          // /
    PERCENT,        // %
    CARET,          // ^ (Exponentiation, Section 8)
    ASSIGN,         // =

    // --- 6. LOGICAL OPERATORS AND COMPARISON ---
    EQUAL_EQUAL,    // ==
    BANG_EQUAL,     // !=
    LESS,           // <
    LESS_EQUAL,     // <=
    GREATER,        // >
    GREATER_EQUAL,  // >=

    // Symbolic (Legacy/C-style)
    AND_AND,        // &&
    OR_OR,          // ||
    BANG,           // !

    // Word-based (Python-style, from Section 8)
    AND_KW,         // and
    OR_KW,          // or
    NOT_KW,         // not
    IS,             // is (Type check, Section 8.1)

    // --- 7. BITWISE OPERATORS (WORDS) ---
    // Specification requires words for readability (Section 8)
    XOR,            // xor
    BIT_AND,        // bit_and
    BIT_OR,         // bit_or
    BIT_NOT,        // bit_not
    SHL,            // shl
    SHR,            // shr

    // --- 8. SPECIAL CHARACTERS ---
    ARROW,          // -> (For return type)
    DOT,            // .  (For field access: struct.field)
    QUESTION,       // ?  (For error propagation Result, Section 9)

    // --- 9. BRACKETS AND SEPARATORS ---
    LPAREN,         // (
    RPAREN,         // )
    LBRACE,         // {
    RBRACE,         // }
    LBRACKET,       // [  (For arrays, Section 6)
    RBRACKET,       // ]  (For arrays, Section 6)
    COLON,          // :
    SEMICOLON,      // ;
    COMMA,          // ,

    // --- 10. SYSTEM/UTILITY ---
    EOF             // End of file
}