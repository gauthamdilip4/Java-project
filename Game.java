/** Pure game logic: holds the secret word and scores guesses. No UI or SQL here. */
public class Game {
    public static final int WORD_LENGTH = 5;
    public static final int MAX_GUESSES = 6;

    public static final int ABSENT = 0;
    public static final int PRESENT = 1;
    public static final int CORRECT = 2;

    private final String secret;
    private int guessCount = 0;
    private boolean won = false;

    public Game(String secret) {
        this.secret = secret.toUpperCase();
    }

    /** Scores a guess: CORRECT = right spot, PRESENT = wrong spot, ABSENT = not in word. */
    public int[] submitGuess(String guess) {
        guess = guess.toUpperCase();
        int[] result = new int[WORD_LENGTH];
        boolean[] used = new boolean[WORD_LENGTH]; // secret letters already matched

        // First pass: exact matches
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (guess.charAt(i) == secret.charAt(i)) {
                result[i] = CORRECT;
                used[i] = true;
            }
        }
        // Second pass: right letter, wrong spot (each secret letter used once)
        for (int i = 0; i < WORD_LENGTH; i++) {
            if (result[i] == CORRECT) continue;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (!used[j] && guess.charAt(i) == secret.charAt(j)) {
                    result[i] = PRESENT;
                    used[j] = true;
                    break;
                }
            }
        }

        guessCount++;
        if (guess.equals(secret)) won = true;
        return result;
    }

    public boolean isOver() { return won || guessCount >= MAX_GUESSES; }
    public boolean isWon() { return won; }
    public int getGuessCount() { return guessCount; }
    public String getSecret() { return secret; }
}
