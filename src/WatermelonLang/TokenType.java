package WatermelonLang;

public enum TokenType {

    // --- 1. КЛЮЧЕВЫЕ СЛОВА (ОСНОВНЫЕ) ---
    LET,        // let
    VAR,        // var
    FUNC,       // func
    STRUCT,     // struct (Для структур, Раздел 5)
    RETURN,     // return
    PUB,        // pub    (Для модулей, Раздел 10)
    IMPORT,     // import (Для модулей, Раздел 10)

    // --- 2. ТИПЫ ДАННЫХ ---
    INT_TYPE,   // int
    FLOAT_TYPE, // float (Добавлено: Раздел 3)
    BOOL_TYPE,  // bool
    STR_TYPE,   // str
    CHAR_TYPE,  // char  (Добавлено: Раздел 3)
    BYTE_TYPE,  // byte  (Критично для крипты, Раздел 3)

    // --- 3. УПРАВЛЕНИЕ ПОТОКОМ ---
    IF,         // if
    ELSE,       // else
    WHILE,      // while
    FOR,        // for      (Добавлено: Раздел 7)
    IN,         // in       (Для цикла for-in, Раздел 7)
    BREAK,      // break    (Добавлено: Раздел 7)
    CONTINUE,   // continue (Добавлено: Раздел 7)

    // --- 4. ЛИТЕРАЛЫ ---
    IDENT,          // Идентификаторы (имена переменных, функций)
    INT_LITERAL,    // 123
    FLOAT_LITERAL,  // 12.34 (Добавлено)
    STRING_LITERAL, // "text"
    CHAR_LITERAL,   // 'c'   (Добавлено)
    TRUE,           // true
    FALSE,          // false

    // --- 5. АРИФМЕТИЧЕСКИЕ ОПЕРАТОРЫ ---
    PLUS,           // +
    MINUS,          // -
    STAR,           // *
    SLASH,          // /
    PERCENT,        // %
    CARET,          // ^ (Возведение в степень, Раздел 8)
    ASSIGN,         // =

    // --- 6. ЛОГИЧЕСКИЕ ОПЕРАТОРЫ И СРАВНЕНИЕ ---
    EQUAL_EQUAL,    // ==
    BANG_EQUAL,     // !=
    LESS,           // <
    LESS_EQUAL,     // <=
    GREATER,        // >
    GREATER_EQUAL,  // >=

    // Символьные (Legacy/C-style)
    AND_AND,        // &&
    OR_OR,          // ||
    BANG,           // !

    // Словесные (Python-style, из Раздела 8)
    AND_KW,         // and
    OR_KW,          // or
    NOT_KW,         // not
    IS,             // is (Проверка типа, Раздел 8.1)

    // --- 7. БИТОВЫЕ ОПЕРАТОРЫ (СЛОВА) ---
    // Спецификация требует слова для читаемости (Раздел 8)
    XOR,            // xor
    BIT_AND,        // bit_and
    BIT_OR,         // bit_or
    BIT_NOT,        // bit_not
    SHL,            // shl
    SHR,            // shr

    // --- 8. СПЕЦИАЛЬНЫЕ СИМВОЛЫ ---
    ARROW,          // -> (Для возвращаемого типа)
    DOT,            // .  (Для доступа к полям: struct.field)
    QUESTION,       // ?  (Для проброса ошибок Result, Раздел 9)

    // --- 9. СКОБКИ И РАЗДЕЛИТЕЛИ ---
    LPAREN,         // (
    RPAREN,         // )
    LBRACE,         // {
    RBRACE,         // }
    LBRACKET,       // [  (Для массивов, Раздел 6)
    RBRACKET,       // ]  (Для массивов, Раздел 6)
    COLON,          // :
    SEMICOLON,      // ;
    COMMA,          // ,

    // --- 10. СЛУЖЕБНЫЕ ---
    EOF             // Конец файла
}