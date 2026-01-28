package WatermelonLang;


public class Token {
    public final TokenType type;
    public final String value;
    public final Object literal;
    public final int line;
    public Token(TokenType type, String value, Object literal, int line)
    {
        this.type = type;
        this.value = value;
        this.literal = literal;
        this.line = line;
    }


    @Override
    public String toString() {
        return type + " " + value + (literal != null ? " " + literal : "");
    }
}
