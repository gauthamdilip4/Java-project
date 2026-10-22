import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/** The Swing window: a 6x5 grid you type directly into. Letters appear in the tiles as you type. */
public class WordleFrame extends JFrame {
    // Color scheme: black background, teal = correct, orange = wrong spot, gray = not in word
    private static final Color BG = Color.BLACK;
    private static final Color TILE_BORDER = new Color(58, 58, 60);
    private static final Color TILE_TYPED_BORDER = new Color(170, 170, 170);
    private static final Color CORRECT = new Color(0, 194, 168);
    private static final Color PRESENT = new Color(255, 159, 28);
    private static final Color ABSENT = new Color(58, 58, 60);
    private static final Color TEXT = Color.WHITE;
    private static final Color SUBTEXT = new Color(150, 150, 150);
    private static final Color ERROR = new Color(255, 99, 99);

    private final Database db;
    private Game game;
    private final StringBuilder current = new StringBuilder(); // letters typed in the current row

    private final JLabel[][] tiles = new JLabel[Game.MAX_GUESSES][Game.WORD_LENGTH];
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel stats = new JLabel(" ", SwingConstants.CENTER);
    private final JButton newGameButton = new JButton("NEW GAME");

    public WordleFrame(Database db) {
        this.db = db;
        setTitle("Guess It");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(10, 10));

        add(buildTitle(), BorderLayout.NORTH);
        add(buildGrid(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        setupKeys();
        newGameButton.addActionListener(e -> startNewGame());

        startNewGame();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JComponent buildTitle() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JLabel title = new JLabel("GUESS IT", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        title.setForeground(TEXT);

        JLabel hint = new JLabel("Type a 5-letter word and press Enter", SwingConstants.CENTER);
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        hint.setForeground(SUBTEXT);

        panel.add(title);
        panel.add(hint);
        return panel;
    }

    private JComponent buildGrid() {
        JPanel grid = new JPanel(new GridLayout(Game.MAX_GUESSES, Game.WORD_LENGTH, 6, 6));
        grid.setBackground(BG);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
        for (int r = 0; r < Game.MAX_GUESSES; r++) {
            for (int c = 0; c < Game.WORD_LENGTH; c++) {
                JLabel tile = new JLabel("", SwingConstants.CENTER);
                tile.setOpaque(true);
                tile.setPreferredSize(new Dimension(62, 62));
                tile.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
                tiles[r][c] = tile;
                grid.add(tile);
            }
        }
        return grid;
    }

    private JComponent buildBottom() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));

        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        status.setAlignmentX(CENTER_ALIGNMENT);
        status.setPreferredSize(new Dimension(380, 24));   // fixed height so the layout never jumps

        newGameButton.setBackground(BG);
        newGameButton.setForeground(TEXT);
        newGameButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        newGameButton.setBorder(new LineBorder(TILE_TYPED_BORDER, 1));
        newGameButton.setFocusPainted(false);
        newGameButton.setFocusable(false);   // keep the keyboard focused on the grid
        newGameButton.setOpaque(true);
        Dimension buttonSize = new Dimension(130, 36);
        newGameButton.setPreferredSize(buttonSize);
        newGameButton.setMinimumSize(buttonSize);
        newGameButton.setMaximumSize(buttonSize);
        newGameButton.setAlignmentX(CENTER_ALIGNMENT);

        stats.setForeground(SUBTEXT);
        stats.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        stats.setAlignmentX(CENTER_ALIGNMENT);

        panel.add(status);
        panel.add(Box.createVerticalStrut(8));
        panel.add(newGameButton);
        panel.add(Box.createVerticalStrut(8));
        panel.add(stats);
        return panel;
    }

    /** Hooks the keyboard up: A-Z types a letter, Backspace deletes, Enter submits. */
    private void setupKeys() {
        InputMap in = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = getRootPane().getActionMap();

        for (char c = 'A'; c <= 'Z'; c++) {
            final char letter = c;
            in.put(KeyStroke.getKeyStroke(c), "letter" + c);
            in.put(KeyStroke.getKeyStroke(Character.toLowerCase(c)), "letter" + c);
            actions.put("letter" + c, action(() -> addLetter(letter)));
        }
        in.put(KeyStroke.getKeyStroke("BACK_SPACE"), "delete");
        actions.put("delete", action(this::deleteLetter));
        in.put(KeyStroke.getKeyStroke("ENTER"), "submit");
        actions.put("submit", action(this::submitGuess));
    }

    /** Wraps a key action so any error (e.g. a database problem) shows up in the window instead of silently failing. */
    private Action action(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    r.run();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    showError("Error: " + ex.getMessage());
                }
            }
        };
    }

    private void showMessage(String text) {
        status.setForeground(TEXT);
        status.setText(text);
    }

    private void showError(String text) {
        status.setForeground(ERROR);
        status.setText(text);
    }

    private void startNewGame() {
        game = new Game(db.getRandomWord());
        current.setLength(0);
        for (JLabel[] row : tiles) {
            for (JLabel t : row) {
                t.setText("");
                t.setBackground(BG);
                t.setForeground(TEXT);
                t.setBorder(new LineBorder(TILE_BORDER, 2));
            }
        }
        showMessage("Guess the word!");
        updateStats();
    }

    private void addLetter(char c) {
        if (game.isOver() || current.length() >= Game.WORD_LENGTH) return;
        JLabel tile = tiles[game.getGuessCount()][current.length()];
        tile.setText(String.valueOf(c));
        tile.setBorder(new LineBorder(TILE_TYPED_BORDER, 2));
        current.append(c);
        showMessage(" ");
    }

    private void deleteLetter() {
        if (game.isOver() || current.length() == 0) return;
        current.deleteCharAt(current.length() - 1);
        JLabel tile = tiles[game.getGuessCount()][current.length()];
        tile.setText("");
        tile.setBorder(new LineBorder(TILE_BORDER, 2));
        showMessage(" ");
    }

    private void submitGuess() {
        if (game.isOver()) return;
        if (current.length() < Game.WORD_LENGTH) {
            showError("Not enough letters.");
            return;
        }
        String guess = current.toString();
        if (!db.isValidWord(guess)) {
            showError(guess + " is not in the word list.");
            return;
        }

        int row = game.getGuessCount();
        int[] result = game.submitGuess(guess);
        for (int c = 0; c < Game.WORD_LENGTH; c++) {
            Color color = colorFor(result[c]);
            tiles[row][c].setBackground(color);
            tiles[row][c].setBorder(new LineBorder(color, 2));
            tiles[row][c].setForeground(result[c] == Game.PRESENT ? Color.BLACK : TEXT);
        }
        current.setLength(0);
        showMessage(" ");

        if (game.isOver()) endGame();
    }

    private Color colorFor(int state) {
        if (state == Game.CORRECT) return CORRECT;
        if (state == Game.PRESENT) return PRESENT;
        return ABSENT;
    }

    private void endGame() {
        db.saveGame(game.getSecret(), game.isWon(), game.getGuessCount());
        if (game.isWon()) {
            showMessage("You won in " + game.getGuessCount() + " guess(es)!");
        } else {
            showMessage("Game over! The word was " + game.getSecret());
        }
        updateStats();
    }

    private void updateStats() {
        int[] s = db.getStats();
        stats.setText("Played: " + s[0] + "   Won: " + s[1]);
    }
}
