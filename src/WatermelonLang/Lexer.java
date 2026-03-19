package WatermelonLang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {

    private enum State {
        START, IN_INT, IN_FLOAT, IN_ID, IN_STR, SEEN_SLASH, IN_COMMENT,
        SEEN_BANG, SEEN_EQ, SEEN_LESS, SEEN_GREATER, SEEN_MINUS, ERROR
    }

    private enum CharType {
        DIGIT, LETTER, DOT, QUOTE, SLASH, EQUALS, BANG, LESS, GREATER, MINUS, SPACE, NEWLINE, OTHER, EOF
    }

    private static final State[][] transitionMatrix = new State[State.values().length][CharType.values().length];
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        // --- Keywords ---
        keywords.put("let", TokenType.LET);
        keywords.put("var", TokenType.VAR);
        keywords.put("func", TokenType.FUNC);
        keywords.put("struct", TokenType.STRUCT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("pub", TokenType.PUB);
        keywords.put("import", TokenType.IMPORT);
        keywords.put("if", TokenType.IF);
        keywords.put("else", TokenType.ELSE);
        keywords.put("while", TokenType.WHILE);
        keywords.put("for", TokenType.FOR);
        keywords.put("in", TokenType.IN);
        keywords.put("break", TokenType.BREAK);
        keywords.put("continue", TokenType.CONTINUE);
        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("and", TokenType.AND_KW);
        keywords.put("or", TokenType.OR_KW);
        keywords.put("not", TokenType.NOT_KW);
        keywords.put("is", TokenType.IS);
        keywords.put("xor", TokenType.XOR);
        keywords.put("bit_and", TokenType.BIT_AND);
        keywords.put("bit_or", TokenType.BIT_OR);
        keywords.put("bit_not", TokenType.BIT_NOT);
        keywords.put("shl", TokenType.SHL);
        keywords.put("shr", TokenType.SHR);
        keywords.put("int", TokenType.INT_TYPE);
        keywords.put("float", TokenType.FLOAT_TYPE);
        keywords.put("bool", TokenType.BOOL_TYPE);
        keywords.put("str", TokenType.STR_TYPE);
        keywords.put("char", TokenType.CHAR_TYPE);
        keywords.put("byte", TokenType.BYTE_TYPE);

        // Fill ERROR by default
        for (State s : State.values()) {
            for (CharType c : CharType.values()) {
                transitionMatrix[s.ordinal()][c.ordinal()] = State.ERROR;
            }
        }

        // --- TRANSITION RULES ---
        setTrans(State.START, CharType.DIGIT, State.IN_INT);
        setTrans(State.START, CharType.LETTER, State.IN_ID);
        setTrans(State.START, CharType.QUOTE, State.IN_STR); // Start string
        setTrans(State.START, CharType.SLASH, State.SEEN_SLASH);
        setTrans(State.START, CharType.BANG, State.SEEN_BANG);
        setTrans(State.START, CharType.EQUALS, State.SEEN_EQ);
        setTrans(State.START, CharType.LESS, State.SEEN_LESS);
        setTrans(State.START, CharType.GREATER, State.SEEN_GREATER);
        setTrans(State.START, CharType.MINUS, State.SEEN_MINUS);
        setTrans(State.START, CharType.SPACE, State.START);
        setTrans(State.START, CharType.NEWLINE, State.START);

        setTrans(State.IN_INT, CharType.DIGIT, State.IN_INT);
        setTrans(State.IN_INT, CharType.DOT, State.IN_FLOAT);
        setTrans(State.IN_FLOAT, CharType.DIGIT, State.IN_FLOAT);
        setTrans(State.IN_ID, CharType.LETTER, State.IN_ID);
        setTrans(State.IN_ID, CharType.DIGIT, State.IN_ID);

        // Strings: Accept everything except Quote
        for (CharType c : CharType.values()) {
            if (c != CharType.QUOTE && c != CharType.EOF) {
                setTrans(State.IN_STR, c, State.IN_STR);
            }
        }
        // IMPORTANT: Quote transitions back to START (Ending the string)
        setTrans(State.IN_STR, CharType.QUOTE, State.START);

        // Comments
        setTrans(State.SEEN_SLASH, CharType.SLASH, State.IN_COMMENT);
        for (CharType c : CharType.values()) {
            if (c != CharType.NEWLINE && c != CharType.EOF) {
                setTrans(State.IN_COMMENT, c, State.IN_COMMENT);
            }
        }
        setTrans(State.IN_COMMENT, CharType.NEWLINE, State.START);

        // Operators
        setTrans(State.SEEN_BANG, CharType.EQUALS, State.START);
        setTrans(State.SEEN_EQ, CharType.EQUALS, State.START);
        setTrans(State.SEEN_LESS, CharType.EQUALS, State.START);
        setTrans(State.SEEN_GREATER, CharType.EQUALS, State.START);
        setTrans(State.SEEN_MINUS, CharType.GREATER, State.START);
    }

    private static void setTrans(State from, CharType input, State to) {
        transitionMatrix[from.ordinal()][input.ordinal()] = to;
    }

    public Lexer(String source) {
        this.source = source;
    }

    private int pos = 0;
    private int line = 1;
    private StringBuilder buffer = new StringBuilder();

    public List<Token> scanTokens() {
        State currentState = State.START;

        while (pos <= source.length()) {
            char c = (pos < source.length()) ? source.charAt(pos) : '\0';
            CharType type = getCharType(c);

            State nextState = State.ERROR;
            if (type != CharType.EOF) {
                nextState = transitionMatrix[currentState.ordinal()][type.ordinal()];
            }

            // 1. END OF TOKEN (ERROR transition)
            if (nextState == State.ERROR && currentState != State.START) {
                finalizeToken(currentState);
                currentState = State.START;
                buffer.setLength(0);
                continue; // Re-process character in START
            }

            // 2. SINGLE CHARACTERS in START
            if (currentState == State.START && nextState == State.ERROR) {
                if (type != CharType.EOF && type != CharType.SPACE && type != CharType.NEWLINE) {
                    addSingleCharToken(c);
                }
                if (c == '\n') line++;
                pos++;
                continue;
            }

            // 3. SPECIAL TRANSITIONS (String end, Operators)
            // We check this BEFORE updating currentState to catch transitions like IN_STR -> START
            handleSpecialTransitions(currentState, nextState, type);

            // 4. UPDATE STATE
            currentState = nextState;

            // 5. BUFFERING
            // We add to buffer if we are in a data state.
            // EXCEPTION: If we just entered IN_STR (Opening quote), don't add the quote.
            if (shouldAddToBuffer(currentState)) {
                if (currentState == State.IN_STR && type == CharType.QUOTE) {
                    // Do not buffer the opening quote
                } else if (c != '\0') {
                    buffer.append(c);
                }
            }

            if (c == '\n') line++;
            pos++;
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void finalizeToken(State finalState) {
        String text = buffer.toString();
        if (text.isEmpty() && !isSingleCharState(finalState)) return;

        if (finalState == State.IN_INT) {
            tokens.add(new Token(TokenType.INT_LITERAL, text, Integer.parseInt(text), line));
        } else if (finalState == State.IN_FLOAT) {
            tokens.add(new Token(TokenType.FLOAT_LITERAL, text, Double.parseDouble(text), line));
        } else if (finalState == State.IN_ID) {
            TokenType type = keywords.getOrDefault(text, TokenType.IDENT);
            tokens.add(new Token(type, text, null, line));
        } else if (finalState == State.SEEN_SLASH) {
            tokens.add(new Token(TokenType.SLASH, "/", null, line));
        } else if (finalState == State.SEEN_BANG) {
            tokens.add(new Token(TokenType.BANG, "!", null, line));
        } else if (finalState == State.SEEN_EQ) {
            tokens.add(new Token(TokenType.ASSIGN, "=", null, line));
        } else if (finalState == State.SEEN_LESS) {
            tokens.add(new Token(TokenType.LESS, "<", null, line));
        } else if (finalState == State.SEEN_GREATER) {
            tokens.add(new Token(TokenType.GREATER, ">", null, line));
        } else if (finalState == State.SEEN_MINUS) {
            tokens.add(new Token(TokenType.MINUS, "-", null, line));
        }
    }

    private void handleSpecialTransitions(State prevState, State nextState, CharType type) {
        // If we transition to START, it might be a completed token (String or Operator)
        if (nextState == State.START) {
            if (prevState == State.IN_STR && type == CharType.QUOTE) {
                // Closing string!
                tokens.add(new Token(TokenType.STRING_LITERAL, buffer.toString(), buffer.toString(), line));
                buffer.setLength(0);
            } else if (prevState == State.SEEN_EQ && type == CharType.EQUALS) {
                tokens.add(new Token(TokenType.EQUAL_EQUAL, "==", null, line));
            } else if (prevState == State.SEEN_BANG && type == CharType.EQUALS) {
                tokens.add(new Token(TokenType.BANG_EQUAL, "!=", null, line));
            } else if (prevState == State.SEEN_LESS && type == CharType.EQUALS) {
                tokens.add(new Token(TokenType.LESS_EQUAL, "<=", null, line));
            } else if (prevState == State.SEEN_GREATER && type == CharType.EQUALS) {
                tokens.add(new Token(TokenType.GREATER_EQUAL, ">=", null, line));
            } else if (prevState == State.SEEN_MINUS && type == CharType.GREATER) {
                tokens.add(new Token(TokenType.ARROW, "->", null, line));
            }
        }
    }

    private boolean shouldAddToBuffer(State s) {
        return s == State.IN_INT || s == State.IN_FLOAT || s == State.IN_ID || s == State.IN_STR;
    }

    private boolean isSingleCharState(State s) {
        return s == State.SEEN_EQ || s == State.SEEN_BANG || s == State.SEEN_LESS ||
                s == State.SEEN_GREATER || s == State.SEEN_MINUS || s == State.SEEN_SLASH;
    }

    private void addSingleCharToken(char c) {
        TokenType type = null;
        if (c == '(') type = TokenType.LPAREN;
        else if (c == ')') type = TokenType.RPAREN;
        else if (c == '{') type = TokenType.LBRACE;
        else if (c == '}') type = TokenType.RBRACE;
        else if (c == '[') type = TokenType.LBRACKET;
        else if (c == ']') type = TokenType.RBRACKET;
        else if (c == ';') type = TokenType.SEMICOLON;
        else if (c == ',') type = TokenType.COMMA;
        else if (c == '.') type = TokenType.DOT;
        else if (c == '+') type = TokenType.PLUS;
        else if (c == '*') type = TokenType.STAR;
        else if (c == ':') type = TokenType.COLON;
        else if (c == '^') type = TokenType.CARET;

        if (type != null) {
            tokens.add(new Token(type, String.valueOf(c), null, line));
        } else {
            WatermelonLang.error(line, "Unexpected character: " + c);
        }
    }

    private CharType getCharType(char c) {
        if (c >= '0' && c <= '9') return CharType.DIGIT;
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') return CharType.LETTER;
        if (c == '.') return CharType.DOT;
        if (c == '"') return CharType.QUOTE;
        if (c == '/') return CharType.SLASH;
        if (c == '=') return CharType.EQUALS;
        if (c == '!') return CharType.BANG;
        if (c == '<') return CharType.LESS;
        if (c == '>') return CharType.GREATER;
        if (c == '-') return CharType.MINUS;
        if (c == ' ' || c == '\t' || c == '\r') return CharType.SPACE;
        if (c == '\n') return CharType.NEWLINE;
        if (c == '\0') return CharType.EOF;
        return CharType.OTHER;
    }
}