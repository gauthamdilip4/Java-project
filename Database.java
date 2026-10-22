import java.sql.*;

/** Handles all SQL: the word list and the history of played games.
 *  Works with either SQLite or MySQL - change USE_MYSQL below to switch. */
public class Database {
    // ---- Settings ----
    private static final boolean USE_MYSQL = false;   // false = SQLite, true = MySQL

    private static final String SQLITE_URL = "jdbc:sqlite:neonword.db";

    private static final String MYSQL_URL =
            "jdbc:mysql://localhost:3306/neonword?createDatabaseIfNotExist=true";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "your_password_here";
    // ------------------

    // Words are stored in the database. Add more here (5 letters each, separated by spaces).
    private static final String[] STARTER_WORDS = loadWords();

private static String[] loadWords() {
    try {
        return java.nio.file.Files.readAllLines(java.nio.file.Paths.get("wordle_words.txt"))
                .stream().map(String::trim).filter(s -> s.length() == 5)
                .toArray(String[]::new);
    } catch (java.io.IOException e) {
        throw new RuntimeException("Could not read wordle_words.txt: " + e.getMessage(), e);
    }
}

    private Connection conn;

    public Database() {
        try {
            if (USE_MYSQL) {
                conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
            } else {
                conn = DriverManager.getConnection(SQLITE_URL);
            }
            createTables();
            seedWords();
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    private void createTables() throws SQLException {
        // Only the auto-increment id column differs between the two databases.
        String idColumn = USE_MYSQL
                ? "id INT AUTO_INCREMENT PRIMARY KEY"
                : "id INTEGER PRIMARY KEY AUTOINCREMENT";
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS words (word VARCHAR(5) PRIMARY KEY)");
            st.execute("CREATE TABLE IF NOT EXISTS games ("
                    + idColumn + ", "
                    + "word VARCHAR(5), won INT, guesses INT, "
                    + "played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    }

    /** Adds any missing starter words on every start (words already there are skipped),
     *  so editing the list above takes effect without deleting the database. */
    private void seedWords() throws SQLException {
        String insert = USE_MYSQL ? "INSERT IGNORE INTO words(word) VALUES (?)"
                                  : "INSERT OR IGNORE INTO words(word) VALUES (?)";
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            for (String w : STARTER_WORDS) {
                ps.setString(1, w);
                ps.executeUpdate();
            }
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public String getRandomWord() {
        String random = USE_MYSQL ? "RAND()" : "RANDOM()";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT word FROM words ORDER BY " + random + " LIMIT 1")) {
            return rs.next() ? rs.getString(1) : "APPLE";
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isValidWord(String word) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM words WHERE word = ?")) {
            ps.setString(1, word);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveGame(String word, boolean won, int guesses) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO games(word, won, guesses) VALUES (?, ?, ?)")) {
            ps.setString(1, word);
            ps.setInt(2, won ? 1 : 0);
            ps.setInt(3, guesses);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns {gamesPlayed, gamesWon}. */
    public int[] getStats() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*), COALESCE(SUM(won), 0) FROM games")) {
            rs.next();
            return new int[]{rs.getInt(1), rs.getInt(2)};
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
