package WatermelonLang;

import java.util.List;

public class Transpiler implements Expr.Visitor<String>, Stmt.Visitor<String> {

    // --- MEMORY FOR OOP ---
    private final java.util.Map<String, String> varTypes = new java.util.HashMap<>();
    private String currentClass = null;

    // Main entry point. Assembles all C code into one large string.
    public String transpile(List<Stmt> statements) {
        StringBuilder cCode = new StringBuilder();
        StringBuilder globals = new StringBuilder(); // For structures and functions
        StringBuilder mainBody = new StringBuilder(); // For executable code
        
        // 1. Inject WatermelonLang Standard Library
        cCode.append("// --- WatermelonLang Standard Library ---\n");
        cCode.append("#include <stdio.h>\n");
        cCode.append("#include <stdbool.h>\n");
        cCode.append("#include <math.h>\n");
        cCode.append("#include <stdlib.h>\n");
        cCode.append("#include <string.h>\n");
        cCode.append("#include <time.h>\n\n");

        // Polymorphic print implementation via C macros
        cCode.append("#define print(x) _Generic((x), \\\n" +
                     "    int: printf(\"%d\", x), \\\n" +
                     "    double: printf(\"%g\", x), \\\n" +
                     "    char*: printf(\"%s\", x), \\\n" +
                     "    default: printf(\"unknown\") \\\n" +
                     ")\n");
        
        cCode.append("#define println(x) { print(x); printf(\"\\n\"); }\n\n");

        // Math and Input
        cCode.append("int read_int() { int x; scanf(\"%d\", &x); return x; }\n");
        cCode.append("double read_float() { double x; scanf(\"%lf\", &x); return x; }\n");
        cCode.append("char* read_str() { char* s = malloc(256); scanf(\"%s\", s); return s; }\n");
        cCode.append("char* to_str(int n) { char* s = malloc(20); sprintf(s, \"%d\", n); return s; }\n");
        cCode.append("int to_int(char* s) { return atoi(s); }\n");
        cCode.append("int _abs(int n) { return abs(n); }\n");
        cCode.append("int _max(int a, int b) { return a > b ? a : b; }\n");
        cCode.append("int _min(int a, int b) { return a < b ? a : b; }\n\n");

        // --- ADDING FUNCTION FOR STRING CONCATENATION ---
        cCode.append("char* str_concat(char* s1, char* s2) {\n");
        cCode.append("    char* res = malloc(strlen(s1) + strlen(s2) + 1);\n");
        cCode.append("    strcpy(res, s1); strcat(res, s2); return res;\n");
        cCode.append("}\n\n");

        // 2. Separate classes/functions and main code
        for (Stmt statement : statements) {
            if (statement instanceof Stmt.Class || statement instanceof Stmt.Function) {
                globals.append(transpile(statement)).append("\n");
            } else {
                String code = transpile(statement);
                mainBody.append("    ").append(code).append("\n");
            }
        }
        
        // 3. Assemble everything
        cCode.append(globals.toString());
        cCode.append("\nint main() {\n");
        cCode.append(mainBody.toString());
        cCode.append("    return 0;\n}\n");

        return cCode.toString();
    }

    private String transpile(Stmt stmt) {
        return stmt.accept(this);
    }

    private String transpile(Expr expr) {
        return expr.accept(this);
    }

    // --- HELPER FUNCTION FOR TYPES ---
    // Translates WatermelonLang types into C language types
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

    // --- STATEMENT GENERATION (STMT) ---

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
        
        // Function parameters
        for (int i = 0; i < stmt.params.size(); i++) {
            builder.append(toCType(stmt.paramTypes.get(i).type)).append(" ").append(stmt.params.get(i).value);
            if (i < stmt.params.size() - 1) builder.append(", ");
        }
        builder.append(") ");
        
        // Function body (this is a Block, so braces {} will be added automatically)
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
        // Determine the type (built-in int/str or custom class)
        String type = stmt.type.type == TokenType.IDENT ? stmt.type.value : toCType(stmt.type.type);
        
        // Store the variable type for correct method calls later
        varTypes.put(stmt.name.value, type);

        if (stmt.initializer != null) {
            return type + " " + stmt.name.value + " = " + transpile(stmt.initializer) + ";";
        }
        return type + " " + stmt.name.value + " = {0};"; // In C, it is better to initialize empty structures with zeros
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return "while (" + transpile(stmt.condition) + ") \n" + transpile(stmt.body);
    }

    // --- EXPRESSION GENERATION (EXPR) ---

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return expr.name.value + " = " + transpile(expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        String op = expr.operator.value;
        // Translate text-based bitwise operators to C symbols
        switch (expr.operator.type) {
            case BIT_AND: op = "&"; break;
            case BIT_OR:  op = "|"; break;
            case XOR:     op = "^"; break;
            case SHL:     op = "<<"; break;
            case SHR:     op = ">>"; break;
        }

        // MAGIC: If it is a plus, and we are concatenating strings -> call C function!
        if (expr.operator.type == TokenType.PLUS) {
            if (isString(expr.left) || isString(expr.right)) {
                return "str_concat(" + transpile(expr.left) + ", " + transpile(expr.right) + ")";
            }
        }

        return transpile(expr.left) + " " + op + " " + transpile(expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        StringBuilder builder = new StringBuilder();

        // Handle built-in and special functions first
        if (expr.callee instanceof Expr.Variable) {
            String name = ((Expr.Variable) expr.callee).name.value;
            
            // Special handling for built-in functions
            if (name.equals("abs")) name = "_abs";
            else if (name.equals("min")) name = "_min";
            else if (name.equals("max")) name = "_max";
            else if (name.equals("time")) return "(int)time(NULL)";
            else if (name.equals("sizeof")) return "sizeof(" + transpile(expr.arguments.get(0)) + ")";

            // MAGIC 2: Constructor call (e.g., Hero() )
            if (Character.isUpperCase(name.charAt(0))) {
                return "(" + name + "){0}"; // C structure initializer
            }
            
            builder.append(name).append("(");
        } 
        // MAGIC 1: Object method call (e.g., warrior.takeDamage())
        else if (expr.callee instanceof Expr.Get) {
            Expr.Get get = (Expr.Get) expr.callee;
            String objName = transpile(get.object); // "warrior" or "this"
            String methodName = get.name.value;     // "takeDamage"
            
            String className = objName.equals("this") ? currentClass : varTypes.get(objName);
            String pointerStr = objName.equals("this") ? "this" : "&" + objName; // Pass the address!

            builder.append(className).append("_").append(methodName).append("(").append(pointerStr);
            if (!expr.arguments.isEmpty()) builder.append(", ");
        } 
        // Regular function call (computed)
        else {
            builder.append(transpile(expr.callee)).append("(");
        }

        // Arguments
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
        // Translate WatermelonLang 'and'/'or' to C '&&'/'||'
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
        
        // 1. Generate the structure
        builder.append("typedef struct {\n");
        for (Stmt.Var field : stmt.fields) {
            String type = field.type.type == TokenType.IDENT ? field.type.value : toCType(field.type.type);
            builder.append("    ").append(type).append(" ").append(field.name.value).append(";\n");
        }
        builder.append("} ").append(stmt.name.value).append(";\n\n");

        // 2. Generate methods (as global C functions with a 'this' pointer)
        currentClass = stmt.name.value;
        for (Stmt.Function method : stmt.methods) {
            String returnType = method.returnType.type == TokenType.IDENT ? method.returnType.value : toCType(method.returnType.type);
            builder.append(returnType).append(" ").append(currentClass).append("_").append(method.name.value).append("(");
            
            // FIRST ARGUMENT: 'this' pointer
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
        currentClass = null; // Exited the class
        
        return builder.toString();
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        String obj = transpile(expr.object);
        // If it is a 'this' pointer, '->' is used in C, otherwise a regular dot '.'
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

    // --- STRING HEURISTICS ---
    private boolean isString(Expr expr) {
        if (expr instanceof Expr.Literal) return ((Expr.Literal) expr).value instanceof String;
        if (expr instanceof Expr.Variable) {
            String type = varTypes.get(((Expr.Variable) expr).name.value);
            return "str".equals(type) || "char*".equals(type);
        }
        if (expr instanceof Expr.Call) {
            if (((Expr.Call) expr).callee instanceof Expr.Variable) {
                String name = ((Expr.Variable) ((Expr.Call) expr).callee).name.value;
                return name.equals("to_str") || name.equals("read_str"); // These functions return strings
            }
        }
        if (expr instanceof Expr.Get) {
            // Since we don't have strict field typing in the Transpiler,
            // we use a hack: if the field is named name or status, it's a string
            String prop = ((Expr.Get) expr).name.value;
            return prop.equals("name") || prop.equals("status");
        }
        if (expr instanceof Expr.Binary) {
            Expr.Binary bin = (Expr.Binary) expr;
            if (bin.operator.type == TokenType.PLUS) return isString(bin.left) || isString(bin.right);
        }
        return false;
    }
}
