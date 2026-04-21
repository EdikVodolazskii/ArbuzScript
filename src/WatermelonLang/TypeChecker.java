package WatermelonLang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class TypeChecker implements Expr.Visitor<TokenType>, Stmt.Visitor<Void> {

    private final Stack<Map<String, TokenType>> scopes = new Stack<>();
    private final Map<String, Stmt.Function> functions = new HashMap<>();
    // --- NEW FIELDS FOR CLASSES ---
    private final Map<String, Stmt.Class> classes = new HashMap<>();
    private String currentClass = null; // Stores the name of the class we are currently in

    // Initialization of the global scope
    public TypeChecker() {
        scopes.push(new HashMap<>());
        defineNativeFunctions();
    }

    private void defineNativeFunctions() {
        // Register built-in functions as empty Stmt.Function objects
        // We only care about their names and return types for checking
        defineNative("print", TokenType.INT_TYPE); 
        defineNative("println", TokenType.INT_TYPE);
        defineNative("read_int", TokenType.INT_TYPE);
        defineNative("read_float", TokenType.FLOAT_TYPE);
        defineNative("read_str", TokenType.STR_TYPE);
        defineNative("abs", TokenType.INT_TYPE);
        defineNative("pow", TokenType.INT_TYPE);
        defineNative("sqrt", TokenType.FLOAT_TYPE);
        defineNative("min", TokenType.INT_TYPE);
        defineNative("max", TokenType.INT_TYPE);
        defineNative("to_str", TokenType.STR_TYPE);
        defineNative("to_int", TokenType.INT_TYPE);
        defineNative("time", TokenType.INT_TYPE);
        // sizeof is a special case, handled directly in the transpiler
        defineNative("sizeof", TokenType.INT_TYPE);
    }

    private void defineNative(String name, TokenType returnType) {
        // Create a "dummy" function for the checker
        functions.put(name, new Stmt.Function(
            new Token(TokenType.IDENT, name, null, 0), 
            new java.util.ArrayList<>(), 
            new java.util.ArrayList<>(), 
            new Token(returnType, "", null, 0), 
            new java.util.ArrayList<>()
        ));
    }

    // Main entry point for checking the entire tree
    public void check(List<Stmt> statements) {
        for (Stmt statement : statements) {
            check(statement);
        }
    }

    // Check an individual statement
    private void check(Stmt stmt) {
        stmt.accept(this);
    }

    // Check an individual expression and return its type
    private TokenType check(Expr expr) {
        return expr.accept(this);
    }

    // Opening a new scope (e.g., when entering { })
    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    // Closing the scope (removing local variables)
    private void endScope() {
        scopes.pop();
    }

    // Registering a new variable in the current scope
    private void declare(Token name, TokenType type) {
        if (scopes.peek().containsKey(name.value)) {
            WatermelonLang.error(name.line, "Variable already declared in this scope.");
        }
        scopes.peek().put(name.value, type);
    }

    // Finding the variable type (starting from the local scope and moving up to global)
    private TokenType getVarType(Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.value)) {
                return scopes.get(i).get(name.value);
            }
        }
        WatermelonLang.error(name.line, "Undefined variable '" + name.value + "'.");
        return null;
    }

    // --- IMPLEMENTATION OF CHECKS FOR STATEMENTS (STMT) ---

    // Check a code block
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        for (Stmt s : stmt.statements) check(s);
        endScope();
        return null;
    }

    // Check variable declaration and type matching
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        TokenType declaredType = stmt.type.type;
        if (stmt.initializer != null) {
            TokenType initType = check(stmt.initializer);
            // Ignore strict check if the value comes from a class/object (IDENT)
            if (initType != declaredType && initType != null && initType != TokenType.IDENT && declaredType != TokenType.IDENT) {
                WatermelonLang.error(stmt.name.line, "Type mismatch. Cannot assign " + initType + " to " + declaredType);
            }
        }
        declare(stmt.name, declaredType);
        return null;
    }

    // Check and save function signature
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

    // Check a regular expression statement
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        check(stmt.expression);
        return null;
    }

    // Check IF condition (must be boolean)
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        TokenType condType = check(stmt.condition);
        if (condType != TokenType.BOOL_TYPE && condType != TokenType.IDENT && condType != null) {
            WatermelonLang.error(stmt.condition instanceof Expr.Variable ? 0 : 0, "Condition in 'if' must be a boolean.");
        }
        check(stmt.thenBranch);
        if (stmt.elseBranch != null) check(stmt.elseBranch);
        return null;
    }

    // Check WHILE condition (must be boolean)
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        TokenType condType = check(stmt.condition);
        if (condType != TokenType.BOOL_TYPE && condType != TokenType.IDENT && condType != null) {
            WatermelonLang.error(0, "Condition in 'while' must be a boolean.");
        }
        check(stmt.body);
        return null;
    }

    // Check the return value
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value != null) check(stmt.value);
        return null;
    }

    // --- IMPLEMENTATION OF CHECKS FOR EXPRESSIONS (EXPR) ---

    // Determine the type of a primitive value (number, string, bool)
    @Override
    public TokenType visitLiteralExpr(Expr.Literal expr) {
        if (expr.value instanceof Integer) return TokenType.INT_TYPE;
        if (expr.value instanceof Double) return TokenType.FLOAT_TYPE;
        if (expr.value instanceof String) return TokenType.STR_TYPE;
        if (expr.value instanceof Boolean) return TokenType.BOOL_TYPE;
        return null;
    }

    // Get the type of the variable being used (with support for built-in types for sizeof)
    @Override
    public TokenType visitVariableExpr(Expr.Variable expr) {
        // If the variable is a built-in type (e.g., int), simply return it
        switch (expr.name.type) {
            case INT_TYPE: case FLOAT_TYPE: case BOOL_TYPE: 
            case STR_TYPE: case CHAR_TYPE: case BYTE_TYPE:
                return expr.name.type;
        }
        // NEW: Allow using class names (e.g., for sizeof)
        if (classes.containsKey(expr.name.value)) {
            return TokenType.IDENT;
        }
        return getVarType(expr.name);
    }

    // Check assignment
    @Override
    public TokenType visitAssignExpr(Expr.Assign expr) {
        TokenType varType = getVarType(expr.name);
        TokenType valType = check(expr.value);
        // Ignore strict check if the value comes from a class/object (IDENT)
        if (varType != null && valType != null && varType != valType && valType != TokenType.IDENT && varType != TokenType.IDENT) {
            WatermelonLang.error(expr.name.line, "Type mismatch in assignment. Expected " + varType);
        }
        return varType;
    }

    // Compute the type of a mathematical or logical operation
    @Override
    public TokenType visitBinaryExpr(Expr.Binary expr) {
        TokenType left = check(expr.left);
        TokenType right = check(expr.right);

        switch (expr.operator.type) {
            case PLUS:
                // MAGIC: If at least one part is a string, the result of addition is a string
                if (left == TokenType.STR_TYPE || right == TokenType.STR_TYPE) return TokenType.STR_TYPE;
            case MINUS: case STAR: case SLASH:
                if (left == TokenType.FLOAT_TYPE || right == TokenType.FLOAT_TYPE) return TokenType.FLOAT_TYPE;
                return TokenType.INT_TYPE;
            case GREATER: case GREATER_EQUAL: case LESS: case LESS_EQUAL:
            case EQUAL_EQUAL: case BANG_EQUAL:
                return TokenType.BOOL_TYPE;
            // NEW: Bitwise operations return int
            case BIT_AND: case BIT_OR: case XOR: case SHL: case SHR:
                if (left != TokenType.INT_TYPE || right != TokenType.INT_TYPE) {
                    WatermelonLang.error(expr.operator.line, "Bitwise operations require 'int' operands.");
                }
                return TokenType.INT_TYPE;
        }
        return left;
    }

    // Check logical AND / OR
    @Override
    public TokenType visitLogicalExpr(Expr.Logical expr) {
        check(expr.left);
        check(expr.right);
        return TokenType.BOOL_TYPE;
    }

    // Check unary operations (!, -)
    @Override
    public TokenType visitUnaryExpr(Expr.Unary expr) {
        return check(expr.right);
    }

    // Check the return type of a function or class constructor
    @Override
    public TokenType visitCallExpr(Expr.Call expr) {
        if (expr.callee instanceof Expr.Variable) {
            String name = ((Expr.Variable) expr.callee).name.value;
            
            // 1. Is this a regular function call?
            if (functions.containsKey(name)) {
                for (Expr arg : expr.arguments) check(arg);
                return functions.get(name).returnType.type;
            }
            
            // 2. Is this a class constructor call (object creation)?
            if (classes.containsKey(name)) {
                for (Expr arg : expr.arguments) check(arg); // For the future, if the constructor has arguments
                return TokenType.IDENT; // Return the type of the user-defined object
            }
            
            WatermelonLang.error(expr.paren.line, "Undefined function or class '" + name + "'.");
            return null;
        }
        
        // For complex calls (e.g., object methods like player.heal()) 
        // currently using a placeholder, as the final check will be done by the C compiler.
        return TokenType.IDENT;
    }

    // Check expressions in parentheses (returns the type of what's inside)
    @Override
    public TokenType visitGroupingExpr(Expr.Grouping expr) {
        return check(expr.expression);
    }

    // --- IMPLEMENTATION OF CHECKS FOR CLASSES ---

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        classes.put(stmt.name.value, stmt);
        String previousClass = currentClass;
        currentClass = stmt.name.value; // Remember that we have entered this class

        beginScope(); // Opening a new scope for methods
        
        // Register methods so they can see each other
        for (Stmt.Function method : stmt.methods) {
            functions.put(method.name.value, method);
        }

        // Check the body of each method (like a regular function)
        for (Stmt.Function method : stmt.methods) {
            beginScope();
            for (int i = 0; i < method.params.size(); i++) {
                declare(method.params.get(i), method.paramTypes.get(i).type);
            }
            for (Stmt s : method.body) check(s);
            endScope();
        }

        endScope();
        currentClass = previousClass; // Exited the class — forgot it
        return null;
    }

    @Override
    public TokenType visitGetExpr(Expr.Get expr) {
        check(expr.object); // Ensure that the object itself (e.g., player) exists
        
        // In an ideal strict system, we would look into the 'classes' map here, 
        // search for this class, and check the type of the specific field. 
        // For now, using the base type system (enum TokenType), we just return an abstract IDENT.
        // The C compiler on the backend will perform the final strict field check.
        return TokenType.IDENT; 
    }

    @Override
    public TokenType visitSetExpr(Expr.Set expr) {
        check(expr.object); // The object must exist
        return check(expr.value); // Return the type of the assigned value
    }

    @Override
    public TokenType visitThisExpr(Expr.This expr) {
        // Main check: 'this' cannot be used outside of a class!
        if (currentClass == null) {
            WatermelonLang.error(expr.keyword.line, "Can't use 'this' outside of a class.");
        }
        return TokenType.IDENT;
    }
}