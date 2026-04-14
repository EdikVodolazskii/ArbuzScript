package WatermelonLang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {

    // 1. РАСШИРЯЕМ СОСТОЯНИЯ: Теперь у каждого символа/оператора есть своё состояние
    private enum State {
        START, IN_INT, IN_FLOAT, IN_ID, IN_STR, STR_CLOSED, IN_COMMENT,
        SEEN_SLASH, SEEN_BANG, IN_BANG_EQ, SEEN_EQ, IN_EQ_EQ,
        SEEN_LESS, IN_LESS_EQ, SEEN_GREATER, IN_GREATER_EQ,
        SEEN_MINUS, IN_ARROW,
        SEEN_PLUS, SEEN_STAR, SEEN_LPAREN, SEEN_RPAREN, SEEN_LBRACE, SEEN_RBRACE,
        SEEN_LBRACKET, SEEN_RBRACKET, SEEN_SEMICOLON, SEEN_COMMA, SEEN_DOT, SEEN_CARET, SEEN_COLON,
        ERROR
    }

    // 2. РАСШИРЯЕМ ТИПЫ СИМВОЛОВ
    private enum CharType {
        DIGIT, LETTER, DOT, QUOTE, SLASH, EQUALS, BANG, LESS, GREATER, MINUS, SPACE, NEWLINE,
        PLUS, STAR, LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, SEMICOLON, COMMA, CARET, COLON,
        EOF, OTHER
    }

    private static final State[][] transitionMatrix = new State[State.values().length][CharType.values().length];

    // МАППИНГ 1: Таблица поиска (O(1)) для символов (заменяет if/else в getCharType)
    private static final CharType[] charMap = new CharType[128];

    // МАППИНГ 2: Связь финального состояния с Токеном (заменяет if/else в finalizeToken)
    private static final TokenType[] acceptingStates = new TokenType[State.values().length];

    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        // --- 1. ЗАПОЛНЯЕМ КЛЮЧЕВЫЕ СЛОВА ---
        keywords.put("let", TokenType.LET); keywords.put("var", TokenType.VAR);
        keywords.put("func", TokenType.FUNC); keywords.put("struct", TokenType.STRUCT);
        keywords.put("class", TokenType.CLASS);
        keywords.put("this", TokenType.THIS);
        keywords.put("new", TokenType.NEW);
        keywords.put("return", TokenType.RETURN); keywords.put("pub", TokenType.PUB);
        keywords.put("import", TokenType.IMPORT); keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE); keywords.put("while", TokenType.WHILE);
        keywords.put("for", TokenType.FOR); keywords.put("in", TokenType.IN);
        keywords.put("break", TokenType.BREAK); keywords.put("continue", TokenType.CONTINUE);
        keywords.put("true", TokenType.TRUE); keywords.put("false", TokenType.FALSE);
        keywords.put("and", TokenType.AND_KW); keywords.put("or", TokenType.OR_KW);
        keywords.put("not", TokenType.NOT_KW); keywords.put("is", TokenType.IS);
        keywords.put("xor", TokenType.XOR); keywords.put("bit_and", TokenType.BIT_AND);
        keywords.put("bit_or", TokenType.BIT_OR); keywords.put("bit_not", TokenType.BIT_NOT);
        keywords.put("shl", TokenType.SHL); keywords.put("shr", TokenType.SHR);
        keywords.put("int", TokenType.INT_TYPE); keywords.put("float", TokenType.FLOAT_TYPE);
        keywords.put("bool", TokenType.BOOL_TYPE); keywords.put("str", TokenType.STR_TYPE);
        keywords.put("char", TokenType.CHAR_TYPE); keywords.put("byte", TokenType.BYTE_TYPE);

        // --- 2. НАСТРАИВАЕМ O(1) СЛОВАРЬ СИМВОЛОВ (Таблица ASCII) ---
        Arrays.fill(charMap, CharType.OTHER);
        for (int c = '0'; c <= '9'; c++) charMap[c] = CharType.DIGIT;
        for (int c = 'a'; c <= 'z'; c++) charMap[c] = CharType.LETTER;
        for (int c = 'A'; c <= 'Z'; c++) charMap[c] = CharType.LETTER;
        charMap['_'] = CharType.LETTER; charMap['.'] = CharType.DOT;
        charMap['"'] = CharType.QUOTE; charMap['/'] = CharType.SLASH;
        charMap['='] = CharType.EQUALS; charMap['!'] = CharType.BANG;
        charMap['<'] = CharType.LESS; charMap['>'] = CharType.GREATER;
        charMap['-'] = CharType.MINUS; charMap['+'] = CharType.PLUS;
        charMap['*'] = CharType.STAR; charMap['^'] = CharType.CARET;
        charMap['('] = CharType.LPAREN; charMap[')'] = CharType.RPAREN;
        charMap['{'] = CharType.LBRACE; charMap['}'] = CharType.RBRACE;
        charMap['['] = CharType.LBRACKET; charMap[']'] = CharType.RBRACKET;
        charMap[';'] = CharType.SEMICOLON; charMap[','] = CharType.COMMA;
        charMap[':'] = CharType.COLON;
        charMap[' '] = CharType.SPACE; charMap['\t'] = CharType.SPACE; charMap['\r'] = CharType.SPACE;
        charMap['\n'] = CharType.NEWLINE;

        // --- 3. ЗАПОЛНЯЕМ МАТРИЦУ ПЕРЕХОДОВ ---
        for (State s : State.values()) {
            Arrays.fill(transitionMatrix[s.ordinal()], State.ERROR);
        }

        // Пробелы и переводы строк игнорируются (переход в START)
        setTrans(State.START, CharType.SPACE, State.START);
        setTrans(State.START, CharType.NEWLINE, State.START);

        // Базовые переходы
        setTrans(State.START, CharType.DIGIT, State.IN_INT);
        setTrans(State.START, CharType.LETTER, State.IN_ID);
        setTrans(State.IN_INT, CharType.DIGIT, State.IN_INT);
        setTrans(State.IN_INT, CharType.DOT, State.IN_FLOAT);
        setTrans(State.IN_FLOAT, CharType.DIGIT, State.IN_FLOAT);
        setTrans(State.IN_ID, CharType.LETTER, State.IN_ID);
        setTrans(State.IN_ID, CharType.DIGIT, State.IN_ID);

        // Одиночные символы
        setTrans(State.START, CharType.PLUS, State.SEEN_PLUS);
        setTrans(State.START, CharType.STAR, State.SEEN_STAR);
        setTrans(State.START, CharType.LPAREN, State.SEEN_LPAREN);
        setTrans(State.START, CharType.RPAREN, State.SEEN_RPAREN);
        setTrans(State.START, CharType.LBRACE, State.SEEN_LBRACE);
        setTrans(State.START, CharType.RBRACE, State.SEEN_RBRACE);
        setTrans(State.START, CharType.LBRACKET, State.SEEN_LBRACKET);
        setTrans(State.START, CharType.RBRACKET, State.SEEN_RBRACKET);
        setTrans(State.START, CharType.SEMICOLON, State.SEEN_SEMICOLON);
        setTrans(State.START, CharType.COMMA, State.SEEN_COMMA);
        setTrans(State.START, CharType.DOT, State.SEEN_DOT);
        setTrans(State.START, CharType.CARET, State.SEEN_CARET);
        setTrans(State.START, CharType.COLON, State.SEEN_COLON);

        // Строки
        setTrans(State.START, CharType.QUOTE, State.IN_STR);
        for (CharType c : CharType.values()) {
            if (c != CharType.QUOTE && c != CharType.EOF) {
                setTrans(State.IN_STR, c, State.IN_STR);
            }
        }
        setTrans(State.IN_STR, CharType.QUOTE, State.STR_CLOSED);

        // Комментарии и слэш
        setTrans(State.START, CharType.SLASH, State.SEEN_SLASH);
        setTrans(State.SEEN_SLASH, CharType.SLASH, State.IN_COMMENT);
        for (CharType c : CharType.values()) {
            if (c != CharType.NEWLINE && c != CharType.EOF) {
                setTrans(State.IN_COMMENT, c, State.IN_COMMENT);
            }
        }
        setTrans(State.IN_COMMENT, CharType.NEWLINE, State.START);

        // Двойные операторы
        setTrans(State.START, CharType.BANG, State.SEEN_BANG);
        setTrans(State.SEEN_BANG, CharType.EQUALS, State.IN_BANG_EQ);
        setTrans(State.START, CharType.EQUALS, State.SEEN_EQ);
        setTrans(State.SEEN_EQ, CharType.EQUALS, State.IN_EQ_EQ);
        setTrans(State.START, CharType.LESS, State.SEEN_LESS);
        setTrans(State.SEEN_LESS, CharType.EQUALS, State.IN_LESS_EQ);
        setTrans(State.START, CharType.GREATER, State.SEEN_GREATER);
        setTrans(State.SEEN_GREATER, CharType.EQUALS, State.IN_GREATER_EQ);
        setTrans(State.START, CharType.MINUS, State.SEEN_MINUS);
        setTrans(State.SEEN_MINUS, CharType.GREATER, State.IN_ARROW);

        // --- 4. НАСТРАИВАЕМ СЛОВАРЬ ПРИНИМАЮЩИХ СОСТОЯНИЙ ---
        // Если конечного состояния тут нет (например IN_COMMENT), токен просто отбрасывается
        acceptingStates[State.IN_INT.ordinal()] = TokenType.INT_LITERAL;
        acceptingStates[State.IN_FLOAT.ordinal()] = TokenType.FLOAT_LITERAL;
        acceptingStates[State.IN_ID.ordinal()] = TokenType.IDENT;
        acceptingStates[State.STR_CLOSED.ordinal()] = TokenType.STRING_LITERAL;

        acceptingStates[State.SEEN_SLASH.ordinal()] = TokenType.SLASH;
        acceptingStates[State.SEEN_BANG.ordinal()] = TokenType.BANG;
        acceptingStates[State.IN_BANG_EQ.ordinal()] = TokenType.BANG_EQUAL;
        acceptingStates[State.SEEN_EQ.ordinal()] = TokenType.ASSIGN;
        acceptingStates[State.IN_EQ_EQ.ordinal()] = TokenType.EQUAL_EQUAL;
        acceptingStates[State.SEEN_LESS.ordinal()] = TokenType.LESS;
        acceptingStates[State.IN_LESS_EQ.ordinal()] = TokenType.LESS_EQUAL;
        acceptingStates[State.SEEN_GREATER.ordinal()] = TokenType.GREATER;
        acceptingStates[State.IN_GREATER_EQ.ordinal()] = TokenType.GREATER_EQUAL;
        acceptingStates[State.SEEN_MINUS.ordinal()] = TokenType.MINUS;
        acceptingStates[State.IN_ARROW.ordinal()] = TokenType.ARROW;

        acceptingStates[State.SEEN_PLUS.ordinal()] = TokenType.PLUS;
        acceptingStates[State.SEEN_STAR.ordinal()] = TokenType.STAR;
        acceptingStates[State.SEEN_LPAREN.ordinal()] = TokenType.LPAREN;
        acceptingStates[State.SEEN_RPAREN.ordinal()] = TokenType.RPAREN;
        acceptingStates[State.SEEN_LBRACE.ordinal()] = TokenType.LBRACE;
        acceptingStates[State.SEEN_RBRACE.ordinal()] = TokenType.RBRACE;
        acceptingStates[State.SEEN_LBRACKET.ordinal()] = TokenType.LBRACKET;
        acceptingStates[State.SEEN_RBRACKET.ordinal()] = TokenType.RBRACKET;
        acceptingStates[State.SEEN_SEMICOLON.ordinal()] = TokenType.SEMICOLON;
        acceptingStates[State.SEEN_COMMA.ordinal()] = TokenType.COMMA;
        acceptingStates[State.SEEN_DOT.ordinal()] = TokenType.DOT;
        acceptingStates[State.SEEN_CARET.ordinal()] = TokenType.CARET;
        acceptingStates[State.SEEN_COLON.ordinal()] = TokenType.COLON;
    }

    private static void setTrans(State from, CharType input, State to) {
        transitionMatrix[from.ordinal()][input.ordinal()] = to;
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int pos = 0;
    private int line = 1;
    private final StringBuilder buffer = new StringBuilder();

    public Lexer(String source) {
        this.source = source;
    }

    // --- ОСНОВНОЙ ЦИКЛ (Теперь без IF/ELSE проверок конкретных символов) ---
    public List<Token> scanTokens() {
        State currentState = State.START;

        while (pos <= source.length()) {
            char c = (pos < source.length()) ? source.charAt(pos) : '\0';
            CharType type = getCharType(c);

            State nextState = State.ERROR;
            if (type != CharType.EOF) {
                nextState = transitionMatrix[currentState.ordinal()][type.ordinal()];
            }

            // Если автомат уперся в тупик (ERROR), значит текущий токен завершен
            if (nextState == State.ERROR && currentState != State.START) {
                finalizeToken(currentState);
                currentState = State.START;
                buffer.setLength(0); // Очищаем буфер
                continue; // Отправляем ТОТ ЖЕ символ на повторную проверку с состояния START
            }

            // Обработка невалидных символов (одиночный символ, который из START ведет в ERROR)
            if (currentState == State.START && nextState == State.ERROR) {
                if (type != CharType.EOF && type != CharType.SPACE && type != CharType.NEWLINE) {
                    WatermelonLang.error(line, "Unexpected character: " + c);
                }
                if (c == '\n') line++;
                pos++;
                continue;
            }

            currentState = nextState;

            // Если мы вернулись в START (например, после пробела или \n в комментариях) - чистим буфер
            if (currentState == State.START) {
                buffer.setLength(0);
            } else if (type != CharType.EOF) {
                buffer.append(c);
            }

            if (c == '\n') line++;
            pos++;
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    // Мгновенная табличная маршрутизация для завершенного токена
    private void finalizeToken(State finalState) {
        TokenType type = acceptingStates[finalState.ordinal()];

        // Если у состояния нет токена (например, это был комментарий), мы его просто игнорируем
        if (type == null) return;

        String text = buffer.toString();
        Object literal = null;

        // Определяем финальное значение
        if (type == TokenType.IDENT) {
            type = keywords.getOrDefault(text, TokenType.IDENT);
        } else if (type == TokenType.INT_LITERAL) {
            literal = Integer.parseInt(text);
        } else if (type == TokenType.FLOAT_LITERAL) {
            literal = Double.parseDouble(text);
        } else if (type == TokenType.STRING_LITERAL) {
            literal = text.substring(1, text.length() - 1); // Убираем кавычки
        }

        tokens.add(new Token(type, text, literal, line));
    }

    // Мгновенная O(1) маршрутизация символов через ASCII-массив
    private CharType getCharType(char c) {
        if (c == '\0') return CharType.EOF;
        if (c < 128) return charMap[c];
        return CharType.OTHER;
    }
}