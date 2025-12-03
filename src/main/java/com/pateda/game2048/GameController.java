package com.pateda.game2048;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// --- JACKSON IMPORTS ---
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Manages all 2048 game logic, state, persistence (save/load via JSON/Jackson),
 * and history tracking for the undo feature.
 */
public class GameController {

    private static final int BOARD_SIZE = 4;
    private static final int HISTORY_LIMIT = 1; // Maximum number of past moves to store
    private static final int WINNING_TILE_VALUE = 2048; // The tile value required to "win"

    private static final String SAVE_FILE;
    static {
        SAVE_FILE = System.getProperty("user.home") + File.separator + "2048_save.json";
    }

    // --- Game State ---
    private int[][] gameBoard;
    private long score;
    private boolean isGameOver;
    private boolean hasWon; // Tracks if the 2048 tile has been created
    private boolean continuePlaying; // Tracks if the user chose to keep playing after winning
    private List<HighScore> highScores;

    @JsonIgnore
    private final Random random;

    // --- History Tracking ---
    private List<int[][]> boardHistory;
    private List<Long> scoreHistory;

    /**
     * Enum for handling move direction cleanly.
     */
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    /**
     * Public default constructor required for Jackson deserialization.
     */
    public GameController() {
        this.random = new Random();
        this.gameBoard = new int[BOARD_SIZE][BOARD_SIZE];
        this.score = 0;
        this.isGameOver = false;
        this.hasWon = false;
        this.continuePlaying = false;
        this.boardHistory = new ArrayList<>();
        this.scoreHistory = new ArrayList<>();
        this.highScores = new ArrayList<>(); // Initialize the list

        initializeBoard();
    }

    // --- Serialization Getters and Setters (Required by Jackson) ---

    @JsonProperty("gameBoard")
    public int[][] getBoard() {
        return gameBoard;
    }

    @JsonAlias("board")
    public void setGameBoard(int[][] gameBoard) {
        this.gameBoard = gameBoard;
    }

    public long getScore() {
        return score;
    }
    public void setScore(long score) {
        this.score = score;
    }

    public boolean isGameOver() {
        return isGameOver;
    }
    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }

    public boolean hasWon() {
        return hasWon;
    }
    public void setHasWon(boolean hasWon) {
        this.hasWon = hasWon;
    }

    public boolean isContinuePlaying() {
        return continuePlaying;
    }
    public void setContinuePlaying(boolean continuePlaying) {
        this.continuePlaying = continuePlaying;
    }

    // History Getters/Setters for serialization
    public List<int[][]> getBoardHistory() {
        return boardHistory;
    }
    public void setBoardHistory(List<int[][]> boardHistory) {
        this.boardHistory = boardHistory;
    }

    public List<Long> getScoreHistory() {
        return scoreHistory;
    }
    public void setScoreHistory(List<Long> scoreHistory) {
        this.scoreHistory = scoreHistory;
    }

    // --- Initialization and Spawning ---

    private void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                gameBoard[i][j] = 0;
            }
        }
        spawnNewTile();
        spawnNewTile();
    }
    // --- High Score Logic ---
    // UPDATED: Now accepts a name
    public void addHighScore(String name, long score) {
        highScores.add(new HighScore(name, score));
        Collections.sort(highScores);

        // Keep only top 10 scores
        if (highScores.size() > 10) {
            highScores = highScores.subList(0, 10);
        }
    }
    public boolean isHighScore(long currentScore) {
        if (highScores.size() < 10) {
            return true; // List isn't full, so it's a high score
        }
        // Check if current score beats the lowest score on the list
        long lowestScore = highScores.get(highScores.size() - 1).getScore();
        return currentScore > lowestScore;
    }



    public List<HighScore> getHighScores() {
        if (highScores == null) {
            highScores = new ArrayList<>();
        }
        return highScores;
    }

    // Setter for Jackson
    public void setHighScores(List<HighScore> highScores) {
        this.highScores = highScores;
    }

    /**
     * Spawns a new tile (90% chance of 2, 10% chance of 4) in a random empty spot.
     */
    public void spawnNewTile() {
        List<int[]> emptyCells = new ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (gameBoard[r][c] == 0) {
                    emptyCells.add(new int[]{r, c});
                }
            }
        }

        if (!emptyCells.isEmpty()) {
            int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
            int r = cell[0];
            int c = cell[1];

            gameBoard[r][c] = random.nextDouble() < 0.9 ? 2 : 4;
        } else if (!isGameOver) {
            checkGameOver();
        }
    }

    // --- Core Game Logic ---

    /**
     * Handles a single move based on user input.
     * @param direction The direction of the move.
     * @return true if the board state changed, false otherwise.
     */
    public boolean handleMove(Direction direction) {
        // Prevent moves if the game is over OR if the user hasn't chosen to continue after winning
        if (isGameOver || (hasWon && !continuePlaying)) {
            return false;
        }

        // 1. Save current state BEFORE the move
        saveHistoryState();

        int[][] originalBoard = deepCopy(gameBoard);
        boolean moved = applyMoveLogic(direction);

        if (moved) {
            spawnNewTile();
            checkGameOver();
        } else {
            // If no move occurred, discard the saved state
            discardLastHistoryState();
        }
        return !boardsEqual(originalBoard, gameBoard);
    }

    private boolean applyMoveLogic(Direction direction) {
        boolean moved = false;
        switch (direction) {
            case UP:
                transposeBoard();
                moved = slideAndMergeLeft();
                transposeBoard();
                break;
            case DOWN:
                transposeBoard();
                reverseRows();
                moved = slideAndMergeLeft();
                reverseRows();
                transposeBoard();
                break;
            case LEFT:
                moved = slideAndMergeLeft();
                break;
            case RIGHT:
                reverseRows();
                moved = slideAndMergeLeft();
                reverseRows();
                break;
        }
        return moved;
    }

    private boolean slideAndMergeLeft() {
        boolean changed = false;

        for (int r = 0; r < BOARD_SIZE; r++) {
            int[] row = gameBoard[r];
            int[] newRow = new int[BOARD_SIZE];
            int current = 0;

            for (int c = 0; c < BOARD_SIZE; c++) {
                if (row[c] != 0) {
                    newRow[current++] = row[c];
                }
            }

            if (!rowEquals(row, newRow)) {
                changed = true;
            }

            for (int i = 0; i < BOARD_SIZE - 1; i++) {
                if (newRow[i] != 0 && newRow[i] == newRow[i + 1]) {
                    newRow[i] *= 2;
                    score += newRow[i];
                    newRow[i + 1] = 0;
                    changed = true;

                    // Check for win condition immediately after merge
                    if (newRow[i] == WINNING_TILE_VALUE) {
                        hasWon = true;
                    }
                }
            }

            int[] finalRow = new int[BOARD_SIZE];
            current = 0;
            for (int i = 0; i < BOARD_SIZE; i++) {
                if (newRow[i] != 0) {
                    finalRow[current++] = newRow[i];
                }
            }

            gameBoard[r] = finalRow;
        }
        return changed;
    }

    private void reverseRows() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE / 2; c++) {
                int temp = gameBoard[r][c];
                gameBoard[r][c] = gameBoard[r][BOARD_SIZE - 1 - c];
                gameBoard[r][BOARD_SIZE - 1 - c] = temp;
            }
        }
    }

    private void transposeBoard() {
        int[][] newBoard = new int[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                newBoard[r][c] = gameBoard[c][r];
            }
        }
        gameBoard = newBoard;
    }

    public void checkGameOver() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (gameBoard[r][c] == 0) {
                    isGameOver = false;
                    return;
                }
            }
        }

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE - 1; c++) {
                if (gameBoard[r][c] == gameBoard[r][c + 1]) {
                    isGameOver = false;
                    return;
                }
            }
        }

        for (int r = 0; r < BOARD_SIZE - 1; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (gameBoard[r][c] == gameBoard[r + 1][c]) {
                    isGameOver = false;
                    return;
                }
            }
        }
        isGameOver = true;
    }


    // --- Undo Functionality ---

    /**
     * Saves the current game state (board and score) to the history lists.
     */
    private void saveHistoryState() {
        if (boardHistory.size() >= HISTORY_LIMIT) {
            // Remove the oldest state if history limit is reached
            boardHistory.removeFirst();
            scoreHistory.removeFirst();
        }
        boardHistory.add(deepCopy(gameBoard));
        scoreHistory.add(score);
    }

    /**
     * Discards the last saved history state (used when a move resulted in no change).
     */
    private void discardLastHistoryState() {
        if (!boardHistory.isEmpty()) {
            boardHistory.remove(boardHistory.size() - 1);
            scoreHistory.remove(scoreHistory.size() - 1);
        }
    }

    /**
     * Attempts to restore the previous game state from history.
     * @return true if an undo operation was performed, false otherwise.
     */
    public boolean undo() {
        if (canUndo()) {
            // Remove current state from history (since we are undoing)
            int lastIndex = boardHistory.size() - 1;

            gameBoard = boardHistory.remove(lastIndex);
            score = scoreHistory.remove(lastIndex);

            // Recalculate game over status since the state changed
            checkGameOver();
            return true;
        }
        return false;
    }

    /**
     * Checks if there is a previous state available for undo.
     */
    public boolean canUndo() {
        // We only allow undo if there is at least one recorded state
        return !boardHistory.isEmpty();
    }


    // --- Persistence (JSON Save/Load using Jackson) ---

    public void saveGame(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        try {
            writer.writeValue(new File(filePath), this);
            System.out.println("Game saved successfully to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving game: " + e.getMessage());
        }
    }

    public static GameController loadGame(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            GameController loadedGame = mapper.readValue(new File(filePath), GameController.class);

            loadedGame.checkGameOver();
            System.out.println("Game loaded successfully from " + filePath);
            return loadedGame;

        } catch (FileNotFoundException e) {
            System.out.println("No save game found. Starting new game.");
        } catch (IOException e) {
            System.err.println("Error loading or parsing game data: " + e.getMessage());
            e.printStackTrace();
        }
        return new GameController();
    }

    public static boolean saveFileExists(String filePath) {
        return new File(filePath).exists();
    }

    public static String getSaveFile() {
        return SAVE_FILE;
    }

    // --- Helper Methods ---

    /** Deep copies the 2D array. */
    private int[][] deepCopy(int[][] source) {
        int[][] destination = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(source[i], 0, destination[i], 0, BOARD_SIZE);
        }
        return destination;
    }

    /** Checks if two boards are structurally and element-wise equal. */
    private boolean boardsEqual(int[][] board1, int[][] board2) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board1[r][c] != board2[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Checks if two rows are element-wise equal. */
    private boolean rowEquals(int[] row1, int[] row2) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (row1[i] != row2[i]) {
                return false;
            }
        }
        return true;
    }
}