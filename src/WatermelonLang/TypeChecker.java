package WatermelonLang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TypeChecker implements Expr.Visitor<TokenType>, Stmt.Visitor<Void> {

    private final Stack<Map<String, TokenType>> scopes = new Stack<>();
    private final Map<String, Stmt.Function> functions = new HashMap<>();
    // --- НОВЫЕ ПОЛЯ ДЛЯ КЛАССОВ ---
    private final Map<String, Stmt.Class> classes = new HashMap<>();
    private String currentClass = null; // Хранит имя класса, внутри которого мы сейчас находимся

    // Инициализация глобальной области видимости
    public TypeChecker() {
        scopes.push(new HashMap<>());
    }

    // Главная точка входа для проверки всего дерева
    public void check(List<Stmt> statements) {
        for (Stmt statement : statements) {
            check(statement);
        }
    }

    // Проверка отдельной инструкции (Statement)
    private void check(Stmt stmt) {
        stmt.accept(this);
    }

    // Проверка отдельного выражения (Expression) и возврат его типа
    private TokenType check(Expr expr) {
        return expr.accept(this);
    }

    // Открытие новой области видимости (например, при входе в { })
    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    // Закрытие области видимости (удаление локальных переменных)
    private void endScope() {
        scopes.pop();
    }

    // Регистрация новой переменной в текущей области видимости
    private void declare(Token name, TokenType type) {
        if (scopes.peek().containsKey(name.value)) {
            WatermelonLang.error(name.line, "Variable already declared in this scope.");
        }
        scopes.peek().put(name.value, type);
    }

    // Поиск типа переменной (начиная с локальной области и вверх до глобальной)
    private TokenType getVarType(Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.value)) {
                return scopes.get(i).get(name.value);
            }
        }
        WatermelonLang.error(name.line, "Undefined variable '" + name.value + "'.");
        return null;
    }

    // --- РЕАЛИЗАЦИЯ ПРОВЕРОК ДЛЯ ИНСТРУКЦИЙ (STMT) ---

    // Проверка блока кода
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        for (Stmt s : stmt.statements) check(s);
        endScope();
        return null;
    }

    // Проверка объявления переменной и совпадения типов
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        TokenType declaredType = stmt.type.type;
        if (stmt.initializer != null) {
            TokenType initType = check(stmt.initializer);
            // Игнорируем строгую проверку, если значение пришло из класса/объекта (IDENT)
            if (initType != declaredType && initType != null && initType != TokenType.IDENT && declaredType != TokenType.IDENT) {
                WatermelonLang.error(stmt.name.line, "Type mismatch. Cannot assign " + initType + " to " + declaredType);
            }
        }
        declare(stmt.name, declaredType);
        return null;
    }

    // Проверка и сохранение сигнатуры функции
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        functions.put(stmt.name.value, stmt);
        beginScope();
        for (int i = 0; i < stmt.params.size(); i++) {
            declare(stmt.params.get(i), stmt.paramTypes.get(i).type);
        }
        for (Stmt s : stmt.body) check(s);
        endScope();
        return null;
    }

    // Проверка обычного выражения-инструкции
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        check(stmt.expression);
        return null;
    }

    // Проверка условия IF (должно быть boolean)
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        TokenType condType = check(stmt.condition);
        if (condType != TokenType.BOOL_TYPE && condType != null) {
            WatermelonLang.error(stmt.condition instanceof Expr.Variable ? 0 : 0, "Condition in 'if' must be a boolean.");
        }
        check(stmt.thenBranch);
        if (stmt.elseBranch != null) check(stmt.elseBranch);
        return null;
    }

    // Проверка условия WHILE (должно быть boolean)
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        TokenType condType = check(stmt.condition);
        if (condType != TokenType.BOOL_TYPE && condType != null) {
            WatermelonLang.error(0, "Condition in 'while' must be a boolean.");
        }
        check(stmt.body);
        return null;
    }

    // Проверка возвращаемого значения
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value != null) check(stmt.value);
        return null;
    }

    // --- РЕАЛИЗАЦИЯ ПРОВЕРОК ДЛЯ ВЫРАЖЕНИЙ (EXPR) ---

    // Определение типа примитивного значения (число, строка, bool)
    @Override
    public TokenType visitLiteralExpr(Expr.Literal expr) {
        if (expr.value instanceof Integer) return TokenType.INT_TYPE;
        if (expr.value instanceof Double) return TokenType.FLOAT_TYPE;
        if (expr.value instanceof String) return TokenType.STR_TYPE;
        if (expr.value instanceof Boolean) return TokenType.BOOL_TYPE;
        return null;
    }

    // Получение типа используемой переменной
    @Override
    public TokenType visitVariableExpr(Expr.Variable expr) {
        return getVarType(expr.name);
    }

    // Проверка присваивания
    @Override
    public TokenType visitAssignExpr(Expr.Assign expr) {
        TokenType varType = getVarType(expr.name);
        TokenType valType = check(expr.value);
        // Игнорируем строгую проверку, если значение пришло из класса/объекта (IDENT)
        if (varType != null && valType != null && varType != valType && valType != TokenType.IDENT && varType != TokenType.IDENT) {
            WatermelonLang.error(expr.name.line, "Type mismatch in assignment. Expected " + varType);
        }
        return varType;
    }

    // Вычисление типа математической или логической операции
    @Override
    public TokenType visitBinaryExpr(Expr.Binary expr) {
        TokenType left = check(expr.left);
        TokenType right = check(expr.right);

        switch (expr.operator.type) {
            case PLUS: case MINUS: case STAR: case SLASH:
                if (left == TokenType.FLOAT_TYPE || right == TokenType.FLOAT_TYPE) return TokenType.FLOAT_TYPE;
                return TokenType.INT_TYPE;
            case GREATER: case GREATER_EQUAL: case LESS: case LESS_EQUAL:
            case EQUAL_EQUAL: case BANG_EQUAL:
                return TokenType.BOOL_TYPE;
        }
        return left;
    }

    // Проверка логических AND / OR
    @Override
    public TokenType visitLogicalExpr(Expr.Logical expr) {
        check(expr.left);
        check(expr.right);
        return TokenType.BOOL_TYPE;
    }

    // Проверка унарных операций (!, -)
    @Override
    public TokenType visitUnaryExpr(Expr.Unary expr) {
        return check(expr.right);
    }

    // Проверка типа, который возвращает функция или конструктор класса
    @Override
    public TokenType visitCallExpr(Expr.Call expr) {
        if (expr.callee instanceof Expr.Variable) {
            String name = ((Expr.Variable) expr.callee).name.value;
            
            // 1. Это вызов обычной функции?
            if (functions.containsKey(name)) {
                for (Expr arg : expr.arguments) check(arg);
                return functions.get(name).returnType.type;
            }
            
            // 2. Это вызов конструктора класса (создание объекта)?
            if (classes.containsKey(name)) {
                for (Expr arg : expr.arguments) check(arg); // На будущее, если у конструктора будут аргументы
                return TokenType.IDENT; // Возвращаем тип пользовательского объекта
            }
            
            WatermelonLang.error(expr.paren.line, "Undefined function or class '" + name + "'.");
            return null;
        }
        
        // Для сложных вызовов (например, методов объекта player.heal()) 
        // пока используем заглушку, так как финальную проверку сделает С-компилятор.
        return TokenType.IDENT;
    }

    // Проверка выражений в скобках (возвращает тип того, что внутри)
    @Override
    public TokenType visitGroupingExpr(Expr.Grouping expr) {
        return check(expr.expression);
    }

    // --- РЕАЛИЗАЦИЯ ПРОВЕРОК ДЛЯ КЛАССОВ ---

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        classes.put(stmt.name.value, stmt);
        String previousClass = currentClass;
        currentClass = stmt.name.value; // Запоминаем, что мы зашли внутрь этого класса

        beginScope(); // Открываем область видимости для методов
        
        // Регистрируем методы, чтобы они видели друг друга
        for (Stmt.Function method : stmt.methods) {
            functions.put(method.name.value, method);
        }

        // Проверяем тело каждого метода (как обычную функцию)
        for (Stmt.Function method : stmt.methods) {
            beginScope();
            for (int i = 0; i < method.params.size(); i++) {
                declare(method.params.get(i), method.paramTypes.get(i).type);
            }
            for (Stmt s : method.body) check(s);
            endScope();
        }

        endScope();
        currentClass = previousClass; // Вышли из класса — забыли его
        return null;
    }

    @Override
    public TokenType visitGetExpr(Expr.Get expr) {
        check(expr.object); // Убеждаемся, что сам объект (например, player) существует
        
        // В идеальной строгой системе мы бы здесь лезли в карту classes, 
        // искали этот класс и проверяли тип конкретного поля. 
        // Пока мы используем базовую систему типов (enum TokenType), мы просто возвращаем абстрактный IDENT.
        // Cишный компилятор на бэкенде сделает финальную строгую проверку полей.
        return TokenType.IDENT; 
    }

    @Override
    public TokenType visitSetExpr(Expr.Set expr) {
        check(expr.object); // Объект должен существовать
        return check(expr.value); // Возвращаем тип присвоенного значения
    }

    @Override
    public TokenType visitThisExpr(Expr.This expr) {
        // Главная проверка: this нельзя вызывать вне класса!
        if (currentClass == null) {
            WatermelonLang.error(expr.keyword.line, "Can't use 'this' outside of a class.");
        }
        return TokenType.IDENT;
    }
}