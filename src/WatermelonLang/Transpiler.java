package WatermelonLang;

import java.util.List;

public class Transpiler implements Expr.Visitor<String>, Stmt.Visitor<String> {

    // --- ПАМЯТЬ ДЛЯ ООП ---
    private final java.util.Map<String, String> varTypes = new java.util.HashMap<>();
    private String currentClass = null;

    // Главная точка входа. Собирает весь C-код в одну большую строку.
    public String transpile(List<Stmt> statements) {
        StringBuilder cCode = new StringBuilder();
        
        // Подключаем стандартные библиотеки Си
        cCode.append("#include <stdio.h>\n");
        cCode.append("#include <stdbool.h>\n\n");

        for (Stmt statement : statements) {
            cCode.append(transpile(statement)).append("\n");
        }
        
        return cCode.toString();
    }

    private String transpile(Stmt stmt) {
        return stmt.accept(this);
    }

    private String transpile(Expr expr) {
        return expr.accept(this);
    }

    // --- ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ ДЛЯ ТИПОВ ---
    // Переводит типы ArbuzScript в типы языка C
    private String toCType(TokenType type) {
        switch (type) {
            case INT_TYPE:   return "int";
            case FLOAT_TYPE: return "double";
            case BOOL_TYPE:  return "bool";
            case STR_TYPE:   return "char*";
            case CHAR_TYPE:  return "char";
            case BYTE_TYPE:  return "unsigned char";
            default: return "void";
        }
    }

    // --- ГЕНЕРАЦИЯ ИНСТРУКЦИЙ (STMT) ---

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        for (Stmt s : stmt.statements) {
            builder.append("    ").append(transpile(s)).append("\n");
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return transpile(stmt.expression) + ";";
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append(toCType(stmt.returnType.type)).append(" ").append(stmt.name.value).append("(");
        
        // Параметры функции
        for (int i = 0; i < stmt.params.size(); i++) {
            builder.append(toCType(stmt.paramTypes.get(i).type)).append(" ").append(stmt.params.get(i).value);
            if (i < stmt.params.size() - 1) builder.append(", ");
        }
        builder.append(") ");
        
        // Тело функции (это Block, поэтому скобки {} добавятся автоматически)
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("{\n");
        for (Stmt s : stmt.body) {
            bodyBuilder.append("    ").append(transpile(s)).append("\n");
        }
        bodyBuilder.append("}\n");

        builder.append(bodyBuilder.toString());
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("if (").append(transpile(stmt.condition)).append(") \n").append(transpile(stmt.thenBranch));
        if (stmt.elseBranch != null) {
            builder.append(" else \n").append(transpile(stmt.elseBranch));
        }
        return builder.toString();
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value != null) {
            return "return " + transpile(stmt.value) + ";";
        }
        return "return;";
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        // Определяем тип (встроенный int/str или кастомный класс)
        String type = stmt.type.type == TokenType.IDENT ? stmt.type.value : toCType(stmt.type.type);
        
        // Запоминаем тип переменной, чтобы потом правильно вызывать её методы
        varTypes.put(stmt.name.value, type);

        if (stmt.initializer != null) {
            return type + " " + stmt.name.value + " = " + transpile(stmt.initializer) + ";";
        }
        return type + " " + stmt.name.value + " = {0};"; // В Си пустые структуры лучше инициализировать нулями
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return "while (" + transpile(stmt.condition) + ") \n" + transpile(stmt.body);
    }

    // --- ГЕНЕРАЦИЯ ВЫРАЖЕНИЙ (EXPR) ---

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.name.value + " = " + transpile(expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return transpile(expr.left) + " " + expr.operator.value + " " + transpile(expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        StringBuilder builder = new StringBuilder();

        // МАГИЯ 1: Вызов метода объекта (например, warrior.takeDamage())
        if (expr.callee instanceof Expr.Get) {
            Expr.Get get = (Expr.Get) expr.callee;
            String objName = transpile(get.object); // "warrior" или "this"
            String methodName = get.name.value;     // "takeDamage"
            
            String className = objName.equals("this") ? currentClass : varTypes.get(objName);
            String pointerStr = objName.equals("this") ? "this" : "&" + objName; // Передаем адрес!

            builder.append(className).append("_").append(methodName).append("(").append(pointerStr);
            if (!expr.arguments.isEmpty()) builder.append(", ");
        } 
        // МАГИЯ 2: Вызов конструктора (например, Hero() )
        else if (expr.callee instanceof Expr.Variable && Character.isUpperCase(((Expr.Variable)expr.callee).name.value.charAt(0))) {
            return "(" + ((Expr.Variable)expr.callee).name.value + "){0}"; // Инициализатор структуры Си
        }
        // Обычная функция (например, factorial(5))
        else {
            builder.append(transpile(expr.callee)).append("(");
        }

        // Аргументы
        for (int i = 0; i < expr.arguments.size(); i++) {
            builder.append(transpile(expr.arguments.get(i)));
            if (i < expr.arguments.size() - 1) builder.append(", ");
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return "(" + transpile(expr.expression) + ")";
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "NULL";
        if (expr.value instanceof String) return "\"" + expr.value + "\"";
        if (expr.value instanceof Boolean) return (boolean) expr.value ? "true" : "false";
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        // Переводим ArbuzScript 'and'/'or' в Сишные '&&'/'||'
        String op = expr.operator.type == TokenType.AND_KW ? "&&" : "||";
        return transpile(expr.left) + " " + op + " " + transpile(expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return expr.operator.value + transpile(expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.value;
    }

    @Override
    public String visitClassStmt(Stmt.Class stmt) {
        StringBuilder builder = new StringBuilder();
        
        // 1. Генерируем структуру
        builder.append("typedef struct {\n");
        for (Stmt.Var field : stmt.fields) {
            String type = field.type.type == TokenType.IDENT ? field.type.value : toCType(field.type.type);
            builder.append("    ").append(type).append(" ").append(field.name.value).append(";\n");
        }
        builder.append("} ").append(stmt.name.value).append(";\n\n");

        // 2. Генерируем методы (как глобальные функции Си с указателем this)
        currentClass = stmt.name.value;
        for (Stmt.Function method : stmt.methods) {
            String returnType = method.returnType.type == TokenType.IDENT ? method.returnType.value : toCType(method.returnType.type);
            builder.append(returnType).append(" ").append(currentClass).append("_").append(method.name.value).append("(");
            
            // ПЕРВЫЙ АРГУМЕНТ: Указатель this
            builder.append(currentClass).append("* this");
            if (!method.params.isEmpty()) builder.append(", ");
            
            for (int i = 0; i < method.params.size(); i++) {
                String paramType = method.paramTypes.get(i).type == TokenType.IDENT ? method.paramTypes.get(i).value : toCType(method.paramTypes.get(i).type);
                builder.append(paramType).append(" ").append(method.params.get(i).value);
                if (i < method.params.size() - 1) builder.append(", ");
            }
            builder.append(") {\n");
            for (Stmt s : method.body) builder.append("    ").append(transpile(s)).append("\n");
            builder.append("}\n\n");
        }
        currentClass = null; // Вышли из класса
        
        return builder.toString();
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        String obj = transpile(expr.object);
        // Если это указатель this, в Си используется '->', иначе обычная точка '.'
        return obj + (obj.equals("this") ? "->" : ".") + expr.name.value;
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        String obj = transpile(expr.object);
        return obj + (obj.equals("this") ? "->" : ".") + expr.name.value + " = " + transpile(expr.value);
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "this";
    }
}
