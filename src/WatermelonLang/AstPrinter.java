package WatermelonLang;

// Этот класс реализует интерфейс Visitor и превращает дерево в строку
public class AstPrinter implements Expr.Visitor<String> {

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        // Превращаем в строку вида: (+ 1 2)
        return parenthesize(expr.operator.value, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        // Превращаем в строку вида: (group expression)
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.value, expr.right);
    }

    // Вспомогательный метод для красивых скобочек
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this)); // Рекурсия!
        }
        builder.append(")");

        return builder.toString();
    }

    // Мэин для теста (можно удалить потом)
    public static void main(String[] args) {
        // Пробуем создать выражение вручную: -123 * (45.67)
        // Структура:
        //      *
        //     / \
        //   (-)  (group)
        //    |      |
        //   123   45.67

        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)
                ),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)
                )
        );

        System.out.println(new AstPrinter().print(expression));
    }
}