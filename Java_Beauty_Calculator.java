import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import javax.swing.*;

public class Java_Beauty_Calculator extends JFrame {
    private final JTextField display = new JTextField("0");

    // Calculator state
    private BigDecimal current = BigDecimal.ZERO;   // accumulated value
    private BigDecimal entry = BigDecimal.ZERO;     // currently typed number
    private String pendingOp = null;                // "+", "-", "×", "÷"
    private boolean entering = false;               // true if user is typing digits
    private boolean justEvaluated = false;          // for chaining logic
    private final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);

    public Calculator() {
        super("Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(320, 460);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Display
        display.setEditable(false);
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(new Font("SF Pro Display", Font.PLAIN, 32));
        display.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(display, BorderLayout.NORTH);

        // Buttons
        String[] keys = {
                "C","⌫","%","÷",
                "7","8","9","×",
                "4","5","6","−",
                "1","2","3","+",
                "±","0",".","="
        };

        JPanel grid = new JPanel(new GridLayout(5,4,8,8));
        for (String k : keys) {
            JButton b = new JButton(k);
            b.setFont(new Font("Inter", Font.PLAIN, 22));
            b.setFocusPainted(false);
            if ("÷×−+".contains(k)) b.setBackground(new Color(245,245,255));
            if ("=".equals(k)) { b.setBackground(new Color(65, 105, 225)); b.setForeground(Color.WHITE); }
            b.addActionListener(e -> onKey(k));
            grid.add(b);
        }
        grid.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        add(grid, BorderLayout.CENTER);

        // Keyboard support
        setupKeyBindings(grid);

        setVisible(true);
        updateDisplay(entry);
    }

    private void setupKeyBindings(JPanel grid) {
        JComponent root = (JComponent) getContentPane();
        int WHEN = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

        bind(root, WHEN, "0", KeyStroke.getKeyStroke('0'), () -> onKey("0"));
        for (char c='1'; c<='9'; c++) bind(root, WHEN, String.valueOf(c), KeyStroke.getKeyStroke(c), () -> onKey(String.valueOf(c)));
        bind(root, WHEN, "dot", KeyStroke.getKeyStroke('.'), () -> onKey("."));

        bind(root, WHEN, "plus",  KeyStroke.getKeyStroke('+'), () -> onKey("+"));
        bind(root, WHEN, "minus", KeyStroke.getKeyStroke('-'), () -> onKey("−"));
        bind(root, WHEN, "mul*",  KeyStroke.getKeyStroke('*'), () -> onKey("×"));
        bind(root, WHEN, "div/",  KeyStroke.getKeyStroke('/'), () -> onKey("÷"));

        bind(root, WHEN, "enter", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), () -> onKey("="));
        bind(root, WHEN, "eq",    KeyStroke.getKeyStroke('='), () -> onKey("="));

        bind(root, WHEN, "esc",   KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), () -> onKey("C"));
        bind(root, WHEN, "back",  KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), () -> onKey("⌫"));
    }

    private void bind(JComponent c, int when, String name, KeyStroke ks, Runnable action) {
        c.getInputMap(when).put(ks, name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void onKey(String k) {
        switch (k) {
            case "0","1","2","3","4","5","6","7","8","9" -> typeDigit(k.charAt(0));
            case "." -> typeDot();
            case "+" , "−" , "×" , "÷" -> chooseOperator(k);
            case "=" -> evaluate();
            case "C" -> clearAll();
            case "⌫" -> backspace();
            case "±" -> toggleSign();
            case "%" -> percent();
        }
    }

    private void typeDigit(char d) {
        if (!entering || justEvaluated) {
            entry = BigDecimal.ZERO;
            entering = true;
            justEvaluated = false;
        }
        String s = entry.stripTrailingZeros().toPlainString();
        if ("0".equals(s)) s = "";
        s += d;
        entry = new BigDecimal(s);
        updateDisplay(entry);
    }

    private void typeDot() {
        String s = entering ? entry.stripTrailingZeros().toPlainString() : "0";
        if (!s.contains(".")) {
            if (!entering || justEvaluated) { s = "0"; entering = true; justEvaluated = false; }
            s += ".";
            entry = new BigDecimal(s.equals(".") ? "0." : s);
            display.setText(s);
        }
    }

    private void chooseOperator(String op) {
        if (pendingOp == null) {
            current = entering ? entry : current;
        } else {
            // chain: compute previous pending op
            if (entering) current = compute(current, entry, pendingOp);
        }
        pendingOp = op;
        entering = false;
        justEvaluated = false;
        updateDisplay(current);
    }

    private void evaluate() {
        if (pendingOp != null) {
            current = compute(current, entering ? entry : current, pendingOp);
            pendingOp = null;
            entering = false;
            justEvaluated = true;
            updateDisplay(current);
        } else {
            // pressing = twice repeats the last entry
            updateDisplay(entering ? entry : current);
        }
    }

    private BigDecimal compute(BigDecimal a, BigDecimal b, String op) {
        try {
            return switch (op) {
                case "+" -> a.add(b, MC);
                case "−" -> a.subtract(b, MC);
                case "×" -> a.multiply(b, MC);
                case "÷" -> {
                    if (b.compareTo(BigDecimal.ZERO) == 0) { showError("Cannot divide by zero"); yield a; }
                    yield a.divide(b, MC);
                }
                default -> b;
            };
        } catch (ArithmeticException ex) {
            showError(ex.getMessage());
            return a;
        }
    }

    private void backspace() {
        if (!entering) return;
        String s = entry.stripTrailingZeros().toPlainString();
        if (s.endsWith(".0")) s = s.substring(0, s.length()-2);
        if (s.length() > 0) s = s.substring(0, s.length()-1);
        if (s.isEmpty() || s.equals("-")) { entry = BigDecimal.ZERO; entering = false; }
        else entry = new BigDecimal(s);
        updateDisplay(entry);
    }

    private void toggleSign() {
        if (entering) entry = entry.negate(MC);
        else current = current.negate(MC);
        updateDisplay(entering ? entry : current);
    }

    private void percent() {
        // percent of current (like many handheld calculators): entry = current * entry / 100
        if (pendingOp != null && entering) {
            entry = current.multiply(entry, MC).divide(new BigDecimal("100"), MC);
            updateDisplay(entry);
        } else {
            entry = entry.divide(new BigDecimal("100"), MC);
            entering = true;
            updateDisplay(entry);
        }
    }

    private void clearAll() {
        current = BigDecimal.ZERO;
        entry = BigDecimal.ZERO;
        pendingOp = null;
        entering = false;
        justEvaluated = false;
        updateDisplay(entry);
    }

    private void updateDisplay(BigDecimal v) {
        String s = v.stripTrailingZeros().toPlainString();
        if (s.equals("-0")) s = "0";
        display.setText(s);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Calculator::new);
    }
}
