package WatermelonLang;
import java.util.ArrayList;
import java.util.List;
import static WatermelonLang.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    // Конструктор парсера, принимает список токенов от лексера.
    public Parser(List<Token> tokens) { this.tokens = tokens; }

    // Главная функция парсера: собирает все токены в список готовых инструкций.
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // Определяет тип текущей конструкции: класс, объявление или инструкция.
    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();
            if (match(FUNC)) return function();
            if (match(VAR, LET)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    // Парсит объявление класса, собирая его поля (var) и методы (func).
    private Stmt classDeclaration() {
        Token name = consume(IDENT, "Expect class name.");
        consume(LBRACE, "Expect '{' before class body.");

        List<Stmt.Var> fields = new ArrayList<>();
        List<Stmt.Function> methods = new ArrayList<>();

        while (!check(RBRACE) && !isAtEnd()) {
            if (match(VAR, LET)) {
                fields.add((Stmt.Var) varDeclaration());
            } else if (match(FUNC)) {
                methods.add((Stmt.Function) function());
            } else {
                throw error(peek(), "Expect 'var' or 'func' inside class.");
            }
        }

        consume(RBRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, fields, methods);
    }

    // Разбирает синтаксис объявления функции: имя, параметры, тип возврата и тело.
    private Stmt function() {
        Token name = consume(IDENT, "Expect function name.");
        consume(LPAREN, "Expect '(' after function name.");
        List<Token> params = new ArrayList<>();
        List<Token> paramTypes = new ArrayList<>();

        if (!check(RPAREN)) {
            do {
                params.add(consume(IDENT, "Expect parameter name."));
                consume(COLON, "Expect ':' after parameter name.");
                if (match(INT_TYPE, FLOAT_TYPE, BOOL_TYPE, STR_TYPE, CHAR_TYPE, BYTE_TYPE, IDENT)) {
                    paramTypes.add(previous());
                } else {
                    throw error(peek(), "Expect parameter type.");
                }
            } while (match(COMMA));
        }
        consume(RPAREN, "Expect ')' after parameters.");

        consume(ARROW, "Expect '->' before return type.");
        Token returnType;
        if (match(INT_TYPE, FLOAT_TYPE, BOOL_TYPE, STR_TYPE, CHAR_TYPE, BYTE_TYPE, IDENT)) {
            returnType = previous();
        } else {
            throw error(peek(), "Expect return type.");
        }

        consume(LBRACE, "Expect '{' before function body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, params, paramTypes, returnType, body);
    }

    // Разбирает синтаксис объявления переменной с обязательным указанием типа.
    private Stmt varDeclaration() {
        Token name = consume(IDENT, "Expect variable name.");
        consume(COLON, "Expect ':' after variable name.");
        Token type;
        if (match(INT_TYPE, FLOAT_TYPE, BOOL_TYPE, STR_TYPE, CHAR_TYPE, BYTE_TYPE, IDENT)) {
            type = previous();
        } else {
            throw error(peek(), "Expect variable type.");
        }
        Expr initializer = null;
        if (match(ASSIGN)) {
            initializer = expression();
        }
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, type, initializer);
    }

    // Распределяет поток по типам инструкций: циклы, условия, возвраты или блоки кода.
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LBRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    // Разбирает цикл for и "распаковывает" его в эквивалентный цикл while для упрощения AST.
    private Stmt forStatement() {
        consume(LPAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR, LET)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RPAREN)) {
            increment = expression();
        }
        consume(RPAREN, "Expect ')' after for clauses.");
        Stmt body = statement();

        if (increment != null) {
            List<Stmt> stmts = new ArrayList<>();
            stmts.add(body);
            stmts.add(new Stmt.Expression(increment));
            body = new Stmt.Block(stmts);
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            List<Stmt> stmts = new ArrayList<>();
            stmts.add(initializer);
            stmts.add(body);
            body = new Stmt.Block(stmts);
        }

        return body;
    }

    // Разбирает классическое условие if с опциональной веткой else.
    private Stmt ifStatement() {
        consume(LPAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RPAREN, "Expect ')' after if condition.");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    // Разбирает команду возврата значения из функции.
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    // Разбирает базовый цикл while с условием.
    private Stmt whileStatement() {
        consume(LPAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RPAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    // Считывает список инструкций, заключенных в фигурные скобки.
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RBRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RBRACE, "Expect '}' after block.");
        return statements;
    }

    // Оборачивает обычное выражение (например, вызов функции или присваивание) в формат инструкции.
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // Точка входа для парсинга любых выражений.
    private Expr expression() {
        return assignment();
    }

    // Разбирает операцию присваивания переменной или свойству объекта.
    private Expr assignment() {
        Expr expr = logicalOr();
        if (match(ASSIGN)) {
            Token equals = previous();
            Expr value = assignment();
            
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }
            
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    // Разбирает логическое ИЛИ.
    private Expr logicalOr() {
        Expr expr = logicalAnd();
        while (match(OR_KW, OR_OR)) {
            Token operator = previous();
            Expr right = logicalAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // Разбирает логическое И.
    private Expr logicalAnd() {
        Expr expr = equality();
        while (match(AND_KW, AND_AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // Разбирает проверки на равенство и неравенство.
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Разбирает операторы сравнения больше/меньше.
    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Разбирает математические операции сложения и вычитания.
    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Разбирает математические операции умножения и деления.
    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Разбирает унарные операции (отрицание числа или логическое НЕ).
    private Expr unary() {
        if (match(BANG, MINUS, NOT_KW)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    // Распознает вызов функции или обращение к свойству объекта через точку.
    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LPAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENT, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    // Считывает список аргументов, переданных в функцию при ее вызове.
    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RPAREN)) {
            do {
                arguments.add(expression());
            } while (match(COMMA));
        }
        Token paren = consume(RPAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    // Парсит базовые "атомарные" элементы, включая указатель this.
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(INT_LITERAL, FLOAT_LITERAL, STRING_LITERAL)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(THIS)) return new Expr.This(previous());
        if (match(IDENT)) return new Expr.Variable(previous());
        if (match(LPAREN)) {
            Expr expr = expression();
            consume(RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    // Проверяет текущий токен на совпадение с одним из переданных типов и "поглощает" его в случае успеха.
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Требует наличия токена определенного типа, иначе выбрасывает синтаксическую ошибку.
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    // Проверяет тип текущего токена, не сдвигая указатель парсера.
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // Сдвигает указатель на следующий токен и возвращает предыдущий.
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // Возвращает true, если достигнут конец файла (токен EOF).
    private boolean isAtEnd() { return peek().type == EOF; }

    // Возвращает токен, на который сейчас указывает парсер.
    private Token peek() { return tokens.get(current); }

    // Возвращает последний успешно обработанный токен.
    private Token previous() { return tokens.get(current - 1); }

    // Формирует и возвращает объект ошибки с указанием строки и сообщения.
    private ParseError error(Token token, String message) {
        WatermelonLang.error(token.line, message);
        return new ParseError();
    }

    // Пропускает токены до начала следующей инструкции, чтобы продолжить работу после ошибки.
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case FUNC: case VAR: case FOR: case IF: case WHILE: case RETURN: return;
            }
            advance();
        }
    }
}