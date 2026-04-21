package WatermelonLang;
import java.util.ArrayList;
import java.util.List;
import static WatermelonLang.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    // Parser constructor, accepts a list of tokens from the lexer.
    public Parser(List<Token> tokens) { this.tokens = tokens; }

    // Main parser function: collects all tokens into a list of completed statements.
    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // Determines the type of the current construct: class, declaration, or statement.
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

    // Parses a class declaration, gathering its fields (var) and methods (func).
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

    // Parses function declaration syntax: name, parameters, return type, and body.
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

    // Parses variable declaration syntax with a mandatory type specification.
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

    // Distributes the flow by statement types: loops, conditions, returns, or code blocks.
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LBRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    // Parses a for loop and "unpacks" it into an equivalent while loop to simplify the AST.
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

    // Parses a classic if condition with an optional else branch.
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

    // Parses the return command for a function value.
    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    // Parses a basic while loop with a condition.
    private Stmt whileStatement() {
        consume(LPAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RPAREN, "Expect ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    // Reads a list of statements enclosed in curly braces.
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RBRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RBRACE, "Expect '}' after block.");
        return statements;
    }

    // Wraps a regular expression (e.g., function call or assignment) into a statement format.
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    // Entry point for parsing any expressions.
    private Expr expression() {
        return assignment();
    }

    // Parses an assignment operation to a variable or an object property.
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

    // Parses logical OR.
    private Expr logicalOr() {
        Expr expr = logicalAnd();
        while (match(OR_KW, OR_OR)) {
            Token operator = previous();
            Expr right = logicalAnd();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // Parses logical AND.
    private Expr logicalAnd() {
        Expr expr = equality();
        while (match(AND_KW, AND_AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    // Parses equality and inequality checks.
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Parses comparison operators (greater than/less than).
    private Expr comparison() {
        Expr expr = bitwise();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = bitwise();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // NEW METHOD: Parses bitwise operations
    private Expr bitwise() {
        Expr expr = term();
        while (match(BIT_AND, BIT_OR, XOR, SHL, SHR)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Parses mathematical addition and subtraction operations.
    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Parses mathematical multiplication and division operations.
    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    // Parses unary operations (numerical negation or logical NOT).
    private Expr unary() {
        if (match(BANG, MINUS, NOT_KW)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    // Recognizes a function call or access to an object property via a dot.
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

    // Reads the list of arguments passed to a function during its call.
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

    // Parses basic "atomic" elements, including the 'this' pointer.
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(INT_LITERAL, FLOAT_LITERAL, STRING_LITERAL)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(THIS)) return new Expr.This(previous());
        
        // --- UPDATED: Built-in types are now considered valid expressions (for sizeof) ---
        if (match(IDENT, INT_TYPE, FLOAT_TYPE, BOOL_TYPE, STR_TYPE, CHAR_TYPE, BYTE_TYPE)) {
            return new Expr.Variable(previous());
        }
        
        if (match(LPAREN)) {
            Expr expr = expression();
            consume(RPAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    // Checks the current token for a match with one of the passed types and "consumes" it if successful.
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    // Requires a token of a specific type, otherwise throws a syntax error.
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    // Checks the type of the current token without moving the parser pointer.
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    // Advances the pointer to the next token and returns the previous one.
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    // Returns true if the end of the file (EOF token) has been reached.
    private boolean isAtEnd() { return peek().type == EOF; }

    // Returns the token the parser is currently pointing to.
    private Token peek() { return tokens.get(current); }

    // Returns the last successfully processed token.
    private Token previous() { return tokens.get(current - 1); }

    // Creates and returns an error object with the line number and message.
    private ParseError error(Token token, String message) {
        WatermelonLang.error(token.line, message);
        return new ParseError();
    }

    // Skips tokens until the start of the next statement to continue processing after an error.
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