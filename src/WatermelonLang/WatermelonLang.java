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

    // Флаг: была ли ошибка при компиляции?
    // Если true, мы не будем пытаться запускать код.
    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: arbuzc [script]");
            System.exit(64);
        } else if (args.length == 1) {
            // Режим файла: читаем и компилируем файл
            runFile(args[0]);
        } else {
            // Режим консоли: вводим код строка за строкой (для тестов)
            runPrompt();
        }
    }

    // Запуск из файла
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Если была ошибка, выходим с кодом ошибки
        if (hadError) System.exit(65);
    }

    // Запуск интерактивной консоли (REPL)
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false; // Сбрасываем флаг, чтобы не прерывать сессию
        }
    }

    // Самое сердце: запуск обработки кода

    private static void run(String source) {
        // 1. Лексический анализ
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        // --- ОТЛАДКА: Выводим все токены ---
        // Это поможет понять, видит ли лексер точки с запятой и переводы строк
         System.out.println("--- TOKENS START ---");
        for (Token token : tokens) {
            System.out.println(token);
        }
        System.out.println("--- TOKENS END ---");
        // -----------------------------------

        // 2. Парсинг (Синтаксический анализ)
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        if (hadError) return;

        // 3. Семантический анализ (Проверка типов)
        TypeChecker typeChecker = new TypeChecker();
        typeChecker.check(statements);

        if (hadError) return;

        // 4. Трансляция в C (Кодогенерация)
        Transpiler transpiler = new Transpiler();
        String cCode = transpiler.transpile(statements);

        // Сохраняем результат в файл output.c
        try (PrintWriter out = new PrintWriter("output.c")) {
            out.println(cCode);
        } catch (Exception e) {
            System.err.println("Failed to write output file: " + e.getMessage());
        }

        // 5. Результат
        System.out.println("Success! Code is semantically correct. Parsed " + statements.size() + " statements.");
        System.out.println("Generated 'output.c' successfully! 🎉");
    }

    // --- ОБРАБОТКА ОШИБОК ---

    // Тот самый метод, на который ругалась IDE
    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}