import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Database db = new Database();
        SwingUtilities.invokeLater(() -> new WordleFrame(db).setVisible(true));
    }
}
