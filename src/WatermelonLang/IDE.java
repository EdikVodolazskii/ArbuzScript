package WatermelonLang;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class IDE {
    private JFrame frame;
    private JTextArea editorPane;
    private JTextArea consolePane;

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new IDE().createAndShowGUI());
    }

    private void createAndShowGUI() {
        // Setup the main window
        frame = new JFrame("🍉 WatermelonLang Studio");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        // --- TOP PANEL (Buttons) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(new Color(45, 52, 54));

        JButton runButton = new JButton("▶ Run WatermelonLang");
        runButton.setBackground(new Color(0, 184, 148));
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(runButton);

        // --- CODE EDITOR (Top) ---
        editorPane = new JTextArea();
        editorPane.setFont(new Font("Consolas", Font.PLAIN, 18));
        editorPane.setBackground(new Color(250, 250, 250));
        editorPane.setCaretColor(Color.RED);
        editorPane.setTabSize(4);
        
        // Initial welcome code
        editorPane.setText("// Welcome to WatermelonLang IDE 🍉\n\nprintln(\"Hello, World!\");\n");
        JScrollPane editorScroll = new JScrollPane(editorPane);

        // --- OUTPUT CONSOLE (Bottom) ---
        consolePane = new JTextArea();
        consolePane.setFont(new Font("Consolas", Font.PLAIN, 14));
        consolePane.setBackground(new Color(30, 30, 30)); // Dark terminal theme
        consolePane.setForeground(new Color(0, 255, 0));  // Green hacker-style text
        consolePane.setEditable(false);
        JScrollPane consoleScroll = new JScrollPane(consolePane);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScroll, consoleScroll);
        splitPane.setDividerLocation(400); // Initial editor height
        splitPane.setDividerSize(5);

        // Add everything to the window
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(splitPane, BorderLayout.CENTER);

        // Run button action
        runButton.addActionListener(e -> executeCode());

        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);
    }

    private void executeCode() {
        // Clear console before new run
        consolePane.setText("🍉 Compiling...\n");
        String code = editorPane.getText();

        // 1. Save code from editor to a temporary file
        try (PrintWriter out = new PrintWriter("ide_temp.arb")) {
            out.print(code);
        } catch (Exception ex) {
            consolePane.append("Error saving file: " + ex.getMessage());
            return;
        }

        // 2. Console interception magic! Redirect System.out to our JTextArea
        PrintStream printStream = new PrintStream(new CustomOutputStream(consolePane));
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        System.setOut(printStream);
        System.setErr(printStream);

        // 3. Launch compiler in a separate thread (to avoid freezing the UI)
        new Thread(() -> {
            try {
                // Call your main compiler class
                WatermelonLang.main(new String[]{"ide_temp.arb"});
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                // Return output back to the original console
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
        }).start();
    }

    // Helper class: intercepts characters intended for the console and appends them to JTextArea
    class CustomOutputStream extends OutputStream {
        private JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            // Append character and scroll down
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }
}
