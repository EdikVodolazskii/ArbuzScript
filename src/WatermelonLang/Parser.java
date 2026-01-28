package WatermelonLang;

import java.util.List;

// ВАЖНО: Импортируем типы токенов статически, чтобы писать PLUS вместо TokenType.PLUS
import static WatermelonLang.TokenType.*;

public class Parser {
    // Внутренний класс ошибки (чтобы мы могли прерывать парсинг)
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // --- ГЛАВНЫЙ МЕТОД ---
    public Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    // --- ГРАММАТИКА (LOGIC) ---

    // 1. Expression
    private Expr expression() {
        return equality();
    }

    // 2. Equality (==, !=)
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // 3. Comparison (>, <, >=, <=)
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // 4. Term (+, -)
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // 5. Factor (*, /)
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // 6. Unary (!, -)
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // 7. Primary (Числа, Строки, Группировка)
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);

        if (match(INT_LITERAL, FLOAT_LITERAL, STRING_LITERAL)) {
            return new Expr.Literal(previous().literal);
        }

        // Группировка ( expression )
        if (match(LPAREN)) {
            Expr expr = expression();
            consume(RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        // Идентификаторы (пока как литералы, позже изменим)
        if (match(IDENT)) {
            return new Expr.Literal(previous().value);
        }

        throw error(peek(), "Expect expression.");
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (HELPERS) ---

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        WatermelonLang.error(token.line, message);
        return new ParseError();
    }
}