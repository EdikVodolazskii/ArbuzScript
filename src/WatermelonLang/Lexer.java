package WatermelonLang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Lexer {
    private final String source;            // Исходный код всей программы
    private final List<Token> tokens = new ArrayList<>(); // Сюда складываем готовые токены
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();

        // Основные
        keywords.put("let",    TokenType.LET);
        keywords.put("var",    TokenType.VAR);
        keywords.put("func",   TokenType.FUNC);
        keywords.put("struct", TokenType.STRUCT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("pub",    TokenType.PUB);
        keywords.put("import", TokenType.IMPORT);

        // Управление потоком
        keywords.put("if",       TokenType.IF);
        keywords.put("else",     TokenType.ELSE);
        keywords.put("while",    TokenType.WHILE);
        keywords.put("for",      TokenType.FOR);
        keywords.put("in",       TokenType.IN);
        keywords.put("break",    TokenType.BREAK);
        keywords.put("continue", TokenType.CONTINUE);

        // Логика и Биты (СЛОВА)
        keywords.put("true",    TokenType.TRUE);
        keywords.put("false",   TokenType.FALSE);
        keywords.put("and",     TokenType.AND_KW);
        keywords.put("or",      TokenType.OR_KW);
        keywords.put("not",     TokenType.NOT_KW);
        keywords.put("is",      TokenType.IS);
        keywords.put("xor",     TokenType.XOR);
        keywords.put("bit_and", TokenType.BIT_AND);
        keywords.put("bit_or",  TokenType.BIT_OR);
        keywords.put("bit_not", TokenType.BIT_NOT);
        keywords.put("shl",     TokenType.SHL);
        keywords.put("shr",     TokenType.SHR);

        // Типы данных
        keywords.put("int",   TokenType.INT_TYPE);
        keywords.put("float", TokenType.FLOAT_TYPE);
        keywords.put("bool",  TokenType.BOOL_TYPE);
        keywords.put("str",   TokenType.STR_TYPE);
        keywords.put("char",  TokenType.CHAR_TYPE);
        keywords.put("byte",  TokenType.BYTE_TYPE);
    }

    // Курсоры для навигации по тексту
    private int start = 0;   // Указывает на первый символ текущего токена
    private int current = 0; // Указывает на символ, который мы сейчас рассматриваем
    private int line = 1;    // Номер строки (для ошибок: "Ошибка на строке 5")

    public Lexer(String source) {
        this.source = source;
    }

    // Главный метод: запускает процесс и возвращает список токенов
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // Мы в начале новой лексемы.
            start = current;
            scanToken();
        }

        // В конце всегда добавляем токен EOF (End Of File), чтобы парсер знал, что код кончился
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    // Проверка: не дошли ли мы до конца текста?
    private boolean isAtEnd() {
        return current >= source.length();
    }

    // ---------------------------------------------
    // Сюда мы будем добавлять логику распознавания
    // ---------------------------------------------
    private void scanToken() {
        char c = advance();

        switch (c) {
            // Односимвольные токены
            case '(': addToken(TokenType.LPAREN); break;
            case ')': addToken(TokenType.RPAREN); break;
            case '{': addToken(TokenType.LBRACE); break;
            case '}': addToken(TokenType.RBRACE); break;
            case '[': addToken(TokenType.LBRACKET); break; // Массивы
            case ']': addToken(TokenType.RBRACKET); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case '+': addToken(TokenType.PLUS); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case '^': addToken(TokenType.CARET); break;
            case ':': addToken(TokenType.COLON); break;

            // Операторы, которые могут быть двойными ( ! и != )
            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.ASSIGN);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;

            // Стрелка (->) или Минус (-)
            case '-':
                addToken(match('>') ? TokenType.ARROW : TokenType.MINUS);
                break; // <-- ВАЖНО: не забывайте break!

            // Деление или Комментарий
            case '/':
                if (match('/')) {
                    // Комментарий идет до конца строки
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            // Пробелы
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            // Строковые литералы (начинаются с кавычки)
            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    WatermelonLang.error(line, "Unexpected character: " + c);
                }
                break;
        }
    }

    // Возвращает текущий символ и двигает курсор (current) вперед.
    private char advance() {
        return source.charAt(current++);
    }
    private char peek()
    {
        if(isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean match(char expected)
    {
        if(isAtEnd()) return false;
        if(source.charAt(current)!=expected) return false;
        current++;
        return true;
    }

    // Метод 2: Добавить простой токен (без значения, например "+") в список.
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // Метод 3: Добавить токен со значением (для чисел, строк, идентификаторов).
    private void addToken(TokenType type, Object literal) {
        // Вырезаем текст из исходника от start до current
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    // --- ОБРАБОТКА СТРОК ---
    private void string() {
        // Идем пока не встретим закрывающую кавычку или конец файла
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++; // Разрешаем многострочные строки
            advance();
        }

        if (isAtEnd()) {
            WatermelonLang.error(line, "Unterminated string.");
            return;
        }

        // Закрывающая кавычка
        advance();

        // Обрезаем кавычки, чтобы получить чистое значение
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING_LITERAL, value);
    }

    // --- ОБРАБОТКА ЧИСЕЛ ---
    private void number() {
        while (isDigit(peek())) advance(); // Съедаем цифры

        // Проверка на дробную часть (например 12.34)
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // Съедаем точку

            while (isDigit(peek())) advance(); // Съедаем цифры после точки

            // Парсим как Float (Double), так как есть точка
            addToken(TokenType.FLOAT_LITERAL, Double.parseDouble(source.substring(start, current)));
        } else {
            // Парсим как Int
            addToken(TokenType.INT_LITERAL, Integer.parseInt(source.substring(start, current)));
        }
    }

    // --- ОБРАБОТКА ИДЕНТИФИКАТОРОВ И КЛЮЧЕВЫХ СЛОВ ---
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);

        // Проверяем, является ли это слово зарезервированным (var, func, if...)
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENT; // Если нет, то это имя переменной

        addToken(type);
    }
    // Является ли символ цифрой
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // Является ли символ буквой или _
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    // Буква или цифра?
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    // Заглянуть на 2 символа вперед (нужно для чисел типа 12.34)
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }
}