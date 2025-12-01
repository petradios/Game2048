package com.pateda.game2048;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.pateda.game2048.GameController.Direction;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the game board and scene flow. Handles UI input, display, and communicates
 * with the decoupled GameController for game logic. This controller manages both the
 * homepage and the game scene by checking which FXML initialized it.
 */
public class SceneController implements Initializable {

    // --- GLOBAL STATE TRACKING ---
    private static GameController activeGameInstance = null;

    // --- FXML Injections for HOMEPAGE SCENE ---
    @FXML private Button continueButton;
    @FXML private Button themeToggle;

    // --- FXML Injections for GAME SCENE ---
    @FXML private GridPane gameGrid;
    @FXML private Label scoreLabel;
    @FXML private Label gameOverMessage;
    @FXML private Button replayButton;
    @FXML private Button undoButton;
    @FXML private VBox winMessageOverlay;

    // Inject all 16 individual tile Labels from the FXML (r,c order)
    @FXML private Label tile00, tile10, tile20, tile30;
    @FXML private Label tile01, tile11, tile21, tile31;
    @FXML private Label tile02, tile12, tile22, tile32;
    @FXML private Label tile03, tile13, tile23, tile33;

    // --- Internal State ---
    private GameController gameLogic;
    private Label[][] tileLabels;
    private Stage stage;
    private int[][] oldBoardState; // Stores the board state BEFORE the last move
    private static final int BOARD_SIZE = 4;

    // NOTE: Animation tracking fields have been removed as they are no longer needed for scale transitions.

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public static GameController getActiveGameInstance() {
        return activeGameInstance;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (gameGrid != null) {
            tileLabels = new Label[][] {
                    {tile00, tile10, tile20, tile30}, {tile01, tile11, tile21, tile31},
                    {tile02, tile12, tile22, tile32}, {tile03, tile13, tile23, tile33}
            };

            gameGrid.setFocusTraversable(true);
            gameGrid.setOnKeyPressed(this::handleKeyPress);
            updateUndoButtonState();
        } else if (continueButton != null) {
            boolean saveExists = GameController.saveFileExists(GameController.getSaveFile());
            continueButton.setDisable(!saveExists);
            if (themeToggle != null) {
                updateThemeButtonText();
            }
        }
    }

    private void updateThemeButtonText() {
        if (themeToggle != null) {
            themeToggle.setText(Game2048.isGrayscale() ? "Theme: Grayscale" : "Theme: Classic");
        }
    }

    private void updateUndoButtonState() {
        if (undoButton != null && gameLogic != null) {
            undoButton.setDisable(!gameLogic.canUndo());
        }
    }

    @FXML
    private void onNewGameButtonClick(ActionEvent event) {
        GameController newGame = new GameController();
        loadGameScene(newGame);
    }

    @FXML
    private void onContinueButtonClick(ActionEvent event) {
        GameController loadedGame = GameController.loadGame(GameController.getSaveFile());
        loadGameScene(loadedGame);
    }

    @FXML
    private void onThemeToggle(ActionEvent event) {
        Game2048.toggleTheme(stage.getScene());
        updateThemeButtonText();
    }

    @FXML
    private void onBackButtonClick(ActionEvent event) {
        if (gameLogic != null) {
            gameLogic.saveGame(GameController.getSaveFile());
        }
        loadMainMenuScene();
    }

    @FXML
    private void onUndoButtonClick(ActionEvent event) {
        if (gameLogic.undo()) {
            this.oldBoardState = deepCopy(gameLogic.getBoard());
            updateBoardUI();
            requestGridFocus(); // <-- FIX: Return focus to the grid after undo
        }
    }

    @FXML
    private void onContinuePlaying(ActionEvent event) {
        gameLogic.setContinuePlaying(true);
        winMessageOverlay.setVisible(false);
        requestGridFocus();
    }

    @FXML
    private void onReplayButtonClick(ActionEvent event) {
        gameLogic = new GameController();
        activeGameInstance = gameLogic;
        this.oldBoardState = deepCopy(gameLogic.getBoard());
        updateBoardUI();
        requestGridFocus();
    }

    private void loadGameScene(GameController controllerInstance) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pateda/game2048/game-scene.fxml"));
            Parent gameRoot = loader.load();
            SceneController gameController = loader.getController();

            gameController.setStage(stage);
            gameController.gameLogic = controllerInstance;
            activeGameInstance = controllerInstance;

            Scene gameScene = new Scene(gameRoot, 800, 800);
            Game2048.applyTheme(gameScene);

            gameController.oldBoardState = gameController.deepCopy(gameController.gameLogic.getBoard());
            gameController.updateBoardUI();

            stage.setScene(gameScene);
            stage.setTitle("2048 Game");
            gameController.requestGridFocus();

        } catch (IOException e) {
            System.err.println("Error loading game-scene.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMainMenuScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pateda/game2048/main-menu.fxml"));
            Parent menuRoot = loader.load();
            SceneController menuController = loader.getController();

            menuController.setStage(stage);
            activeGameInstance = null;

            Scene menuScene = new Scene(menuRoot, 800, 800);
            Game2048.applyTheme(menuScene);

            stage.setScene(menuScene);
            stage.setTitle("2048");

        } catch (IOException e) {
            System.err.println("Error loading main-menu.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onQuitButtonClick(ActionEvent event) {
        Platform.exit();
    }

    public void requestGridFocus() {
        if (gameGrid != null) {
            Platform.runLater(() -> gameGrid.requestFocus());
        }
    }

    @FXML
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            onBackButtonClick(null);
            event.consume();
            return;
        }

        if (winMessageOverlay != null && winMessageOverlay.isVisible()) {
            event.consume();
            return;
        }

        Direction direction = null;

        if (gameLogic.isGameOver()) {
            return;
        }

        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W) {
            direction = Direction.UP;
        } else if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.S) {
            direction = Direction.DOWN;
        } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) {
            direction = Direction.LEFT;
        } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) {
            direction = Direction.RIGHT;
        }

        if (direction != null) {
            boolean boardChanged = gameLogic.handleMove(direction);

            if (boardChanged) {
                // If the board changed, update the UI and trigger scale animations
                updateBoardUI();
            }
        }
        event.consume();
    }

    /**
     * Synchronizes the JavaFX UI (Labels) with the internal GameController state,
     * adding animations for newly spawned tiles and merging tiles.
     */
    public void updateBoardUI() {
        int[][] currentBoard = gameLogic.getBoard();

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                int value = currentBoard[r][c];
                int oldValue = oldBoardState[r][c];
                Label currentLabel = tileLabels[r][c];

                // Clear any previous translation/scale that might be lingering
                currentLabel.setTranslateX(0);
                currentLabel.setTranslateY(0);
                currentLabel.setScaleX(1.0);
                currentLabel.setScaleY(1.0);

                // 1. Update style and text instantly
                currentLabel.getStyleClass().clear();
                currentLabel.getStyleClass().add("tile");

                if (value > 0) {
                    currentLabel.setText(String.valueOf(value));
                    String className = value >= 4096 ? "tile-max" : "tile-" + value;
                    currentLabel.getStyleClass().add(className);

                    // A) Apply Spawn Animation (New tile from zero)
                    if (oldValue == 0) {
                        currentLabel.setScaleX(0.1);
                        currentLabel.setScaleY(0.1);

                        ScaleTransition st = new ScaleTransition(Duration.millis(150), currentLabel);
                        st.setToX(1.0);
                        st.setToY(1.0);
                        st.play();
                    }
                    // B) Apply Pulse Animation (Merged tile: value changed from non-zero)
                    else if (oldValue != value && oldValue != 0) {
                        ScaleTransition st = new ScaleTransition(Duration.millis(100), currentLabel);
                        st.setFromX(1.15); // Exaggerated pulse
                        st.setFromY(1.15);
                        st.setToX(1.0);
                        st.setToY(1.0);
                        st.play();
                    }

                } else {
                    currentLabel.setText("");
                    currentLabel.getStyleClass().add("tile-empty");
                }
            }
        }

        // Update the previous board state for the next move comparison
        this.oldBoardState = deepCopy(currentBoard);

        scoreLabel.setText("SCORE: " + gameLogic.getScore());
        updateUndoButtonState();

        // Check Win/Game Over state

        if (gameLogic.hasWon() && !gameLogic.isContinuePlaying()) {
            if (winMessageOverlay != null) {
                winMessageOverlay.setVisible(true);
            }
            gameOverMessage.setVisible(false);
            if (replayButton != null) replayButton.setVisible(false);

        } else if (gameLogic.isGameOver()) {
            gameOverMessage.setText("Game Over!");
            gameOverMessage.setVisible(true);

            if (replayButton != null) {
                replayButton.setVisible(true);
            }
        } else {
            gameOverMessage.setVisible(false);
            if (replayButton != null) replayButton.setVisible(false);
            if (winMessageOverlay != null) winMessageOverlay.setVisible(false);
        }
    }

    /** Deep copies the 2D board array. */
    private int[][] deepCopy(int[][] source) {
        int[][] destination = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(source[i], 0, destination[i], 0, BOARD_SIZE);
        }
        return destination;
    }

    /**
     * Public getter used by Game2048 to save state on app close.
     */
    public GameController getGameLogic() {
        return gameLogic;
    }
}