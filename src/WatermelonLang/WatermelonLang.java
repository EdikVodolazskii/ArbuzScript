package WatermelonLang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class WatermelonLang {

    // Flag: was there a compilation error?
    // If true, we won't attempt to run the code.
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: arbuzc [script]");
            System.exit(64);
        } else if (args.length == 1) {
            // File mode: read and compile the file
            runFile(args[0]);
        } else {
            // Console mode: input code line by line (for testing)
            runPrompt();
        }
    }

    // Run from a file
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // If there was an error, exit with an error code
        if (hadError) System.exit(65);
    }

    // Launch interactive console (REPL)
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false; // Reset the flag to not interrupt the session
        }
    }

    // The core: starting code processing

    private static void run(String source) {
        // 1. Lexical analysis
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        // --- DEBUG: Output all tokens ---
        // This helps to understand if the lexer sees semicolons and newlines
         System.out.println("--- TOKENS START ---");
        for (Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("--- TOKENS END ---");
        // -----------------------------------

        // 2. Parsing (Syntax analysis)
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        // 3. Semantic analysis (Type checking)
        TypeChecker typeChecker = new TypeChecker();
        typeChecker.check(statements);

        if (hadError) return;

        // 4. Translation to C (Code generation)
        Transpiler transpiler = new Transpiler();
        String cCode = transpiler.transpile(statements);

        // Save the result to the output.c file
        try (PrintWriter out = new PrintWriter("output.c")) {
            out.println(cCode);
        } catch (Exception e) {
            System.err.println("Failed to write output file: " + e.getMessage());
        }

        // 5. Result
        System.out.println("Success! Code is semantically correct. Parsed " + statements.size() + " statements.");
        System.out.println("Generated 'output.c' successfully! 🎉");
    }

    // --- ERROR HANDLING ---

    // The method that the IDE was complaining about
    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}