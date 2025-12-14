package com.pateda.game2048;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class GameController {

    private static final int BOARD_SIZE = 4;
    private static final int HISTORY_LIMIT = 1; // Depth of undo history
    private static final int WINNING_TILE_VALUE = 2048;

    // Define save file location in user's home directory
    private static final String SAVE_FILE;
    static {
        SAVE_FILE = System.getProperty("user.home") + File.separator + "2048_save.json";
    }

    //Game State Fields
    private int[][] gameBoard;
    private long score;
    private boolean isGameOver;
    private boolean hasWon;
    private boolean continuePlaying;
    private List<HighScore> highScores;

    @JsonIgnore
    private final Random random;

    //History for Undo
    private List<int[][]> boardHistory;
    private List<Long> scoreHistory;

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    // Constructor initializes a fresh game state
    public GameController() {
        this.random = new Random();
        this.gameBoard = new int[BOARD_SIZE][BOARD_SIZE];
        this.score = 0;
        this.isGameOver = false;
        this.hasWon = false;
        this.continuePlaying = false;
        this.boardHistory = new ArrayList<>();
        this.scoreHistory = new ArrayList<>();
        this.highScores = new ArrayList<>();

        initializeBoard();
    }

    //Serialization Accessors

    @JsonProperty("gameBoard")
    public int[][] getBoard() { return gameBoard; }

    @JsonAlias("board")
    public void setGameBoard(int[][] gameBoard) { this.gameBoard = gameBoard; }

    public long getScore() { return score; }
    public void setScore(long score) { this.score = score; }

    public boolean isGameOver() { return isGameOver; }
    public void setGameOver(boolean isGameOver) { this.isGameOver = isGameOver; }

    public boolean hasWon() { return hasWon; }
    public void setHasWon(boolean hasWon) { this.hasWon = hasWon; }

    public boolean isContinuePlaying() { return continuePlaying; }
    public void setContinuePlaying(boolean continuePlaying) { this.continuePlaying = continuePlaying; }

    public List<int[][]> getBoardHistory() { return boardHistory; }
    public void setBoardHistory(List<int[][]> boardHistory) { this.boardHistory = boardHistory; }

    public List<Long> getScoreHistory() { return scoreHistory; }
    public void setScoreHistory(List<Long> scoreHistory) { this.scoreHistory = scoreHistory; }

    public List<HighScore> getHighScores() {
        if (highScores == null) highScores = new ArrayList<>();
        return highScores;
    }
    public void setHighScores(List<HighScore> highScores) { this.highScores = highScores; }

    // Clears board and spawns initial tiles
    private void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                gameBoard[i][j] = 0;
            }
        }
        spawnNewTile();
        spawnNewTile();
    }

    // Adds a new score, sorts list, and keeps top 10
    public void addHighScore(String name, long score) {
        highScores.add(new HighScore(name, score));
        Collections.sort(highScores);
        if (highScores.size() > 10) {
            highScores = highScores.subList(0, 10);
        }
    }

    // Checks if a score qualifies for the top 10
    public boolean isHighScore(long currentScore) {
        if (highScores.size() < 10) return true;
        return currentScore > highScores.get(highScores.size() - 1).getScore();
    }

    // Spawns a 2 (90%) or 4 (10%) in a random empty cell
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
            gameBoard[cell[0]][cell[1]] = random.nextDouble() < 0.9 ? 2 : 4;
        } else if (!isGameOver) {
            checkGameOver();
        }
    }

    // Main entry for player moves; returns true if board changed
    public boolean handleMove(Direction direction) {
        if (isGameOver || (hasWon && !continuePlaying)) return false;

        saveHistoryState(); // Snapshot for undo

        int[][] originalBoard = deepCopy(gameBoard);
        boolean moved = applyMoveLogic(direction);

        if (moved) {
            spawnNewTile();
            checkGameOver();
        } else {
            discardLastHistoryState(); // No move happened, don't save undo state
        }
        return !boardsEqual(originalBoard, gameBoard);
    }

    // Applies transformation logic based on direction
    private boolean applyMoveLogic(Direction direction) {
        boolean moved = false;
        switch (direction) {
            case UP -> {
                transposeBoard();
                moved = slideAndMergeLeft();
                transposeBoard();
            }
            case DOWN -> {
                transposeBoard();
                reverseRows();
                moved = slideAndMergeLeft();
                reverseRows();
                transposeBoard();
            }
            case LEFT -> moved = slideAndMergeLeft();
            case RIGHT -> {
                reverseRows();
                moved = slideAndMergeLeft();
                reverseRows();
            }
        }
        return moved;
    }

    //Method that slides tiles to left and merges duplicates
    private boolean slideAndMergeLeft() {
        boolean changed = false;

        for (int r = 0; r < BOARD_SIZE; r++) {
            int[] row = gameBoard[r];
            int[] newRow = new int[BOARD_SIZE];
            int current = 0;

            // Slide non-zero values to the left
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (row[c] != 0) newRow[current++] = row[c];
            }

            if (!rowEquals(row, newRow)) changed = true;

            // Merge adjacent equal values
            for (int i = 0; i < BOARD_SIZE - 1; i++) {
                if (newRow[i] != 0 && newRow[i] == newRow[i + 1]) {
                    newRow[i] *= 2;
                    score += newRow[i];
                    newRow[i + 1] = 0;
                    changed = true;
                    if (newRow[i] == WINNING_TILE_VALUE) hasWon = true;
                }
            }

            // Slide again after merge to fill gaps
            int[] finalRow = new int[BOARD_SIZE];
            current = 0;
            for (int i = 0; i < BOARD_SIZE; i++) {
                if (newRow[i] != 0) finalRow[current++] = newRow[i];
            }

            gameBoard[r] = finalRow;
        }
        return changed;
    }

    // Board manipulation helpers for directional logic
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

    // Checks for empty spots or possible merges
    public void checkGameOver() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (gameBoard[r][c] == 0) {
                    isGameOver = false;
                    return;
                }
            }
        }
        // Check horizontal merges
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE - 1; c++) {
                if (gameBoard[r][c] == gameBoard[r][c + 1]) {
                    isGameOver = false;
                    return;
                }
            }
        }
        // Check vertical merges
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

    //Undo Logic

    private void saveHistoryState() {
        if (boardHistory.size() >= HISTORY_LIMIT) {
            boardHistory.remove(0);
            scoreHistory.remove(0);
        }
        boardHistory.add(deepCopy(gameBoard));
        scoreHistory.add(score);
    }

    private void discardLastHistoryState() {
        if (!boardHistory.isEmpty()) {
            boardHistory.remove(boardHistory.size() - 1);
            scoreHistory.remove(scoreHistory.size() - 1);
        }
    }

    public boolean undo() {
        if (canUndo()) {
            int lastIndex = boardHistory.size() - 1;
            gameBoard = boardHistory.remove(lastIndex);
            score = scoreHistory.remove(lastIndex);
            checkGameOver();
            return true;
        }
        return false;
    }

    public boolean canUndo() {
        return !boardHistory.isEmpty();
    }

    //Persistence

    public void saveGame(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        try {
            writer.writeValue(new File(filePath), this);
            System.out.println("Game saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving game: " + e.getMessage());
        }
    }

    public static GameController loadGame(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            GameController loadedGame = mapper.readValue(new File(filePath), GameController.class);
            loadedGame.checkGameOver();
            System.out.println("Game loaded from " + filePath);
            return loadedGame;
        } catch (FileNotFoundException e) {
            System.out.println("No save found. Starting new game.");
        } catch (IOException e) {
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

    //Utility Methods

    private int[][] deepCopy(int[][] source) {
        int[][] destination = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(source[i], 0, destination[i], 0, BOARD_SIZE);
        }
        return destination;
    }

    private boolean boardsEqual(int[][] b1, int[][] b2) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (b1[r][c] != b2[r][c]) return false;
            }
        }
        return true;
    }

    private boolean rowEquals(int[] r1, int[] r2) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (r1[i] != r2[i]) return false;
        }
        return true;
    }
}