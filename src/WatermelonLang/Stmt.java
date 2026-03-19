package WatermelonLang;
import java.util.List;

abstract class Stmt {
    // Интерфейс для обхода дерева инструкций
    interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitReturnStmt(Return stmt);
        R visitVarStmt(Var stmt);
        R visitWhileStmt(While stmt);
    }

    static class Block extends Stmt {
        final List<Stmt> statements;
        Block(List<Stmt> statements) { this.statements = statements; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitBlockStmt(this); }
    }

    static class Expression extends Stmt {
        final Expr expression;
        Expression(Expr expression) { this.expression = expression; }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitExpressionStmt(this); }
    }

    static class Function extends Stmt {
        final Token name;
        final List<Token> params;
        final List<Token> paramTypes;
        final Token returnType;
        final List<Stmt> body;
        Function(Token name, List<Token> params, List<Token> paramTypes, Token returnType, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.paramTypes = paramTypes;
            this.returnType = returnType;
            this.body = body;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitFunctionStmt(this); }
    }

    static class If extends Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitIfStmt(this); }
    }

    static class Return extends Stmt {
        final Token keyword;
        final Expr value;
        Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitReturnStmt(this); }
    }

    static class Var extends Stmt {
        final Token name;
        final Token type;
        final Expr initializer;
        Var(Token name, Token type, Expr initializer) {
            this.name = name;
            this.type = type;
            this.initializer = initializer;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitVarStmt(this); }
    }

    static class While extends Stmt {
        final Expr condition;
        final Stmt body;
        While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }
        @Override <R> R accept(Visitor<R> visitor) { return visitor.visitWhileStmt(this); }
    }

    // Базовый метод принятия посетителя
    abstract <R> R accept(Visitor<R> visitor);
}