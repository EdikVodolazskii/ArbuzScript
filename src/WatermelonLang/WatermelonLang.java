package WatermelonLang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
        // 1. Лексический анализ (То, что мы сейчас пишем)
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        // Пока что просто выведем токены, чтобы проверить, как работает Лексер
        //for (Token token : tokens) {
            //System.out.println(token);
        //}
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        // Если была синтаксическая ошибка, выходим
        if (hadError) return;

        // Печатаем дерево!
        System.out.println(new AstPrinter().print(expression));

        // (Позже здесь будет запуск Парсера)
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