package com.pateda.game2048;

import javafx.animation.Animation;
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
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.pateda.game2048.GameController.Direction;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the game board and scene flow. Handles UI input, display, and communicates
 * with the decoupled GameController for game logic.
 */
public class SceneController implements Initializable {

    // --- GLOBAL STATE TRACKING ---
    private static GameController activeGameInstance = null;

    // --- FXML Injections for HOMEPAGE SCENE ---
    @FXML private Button continueButton;
    @FXML private Button themeToggle;
    @FXML private VBox highScoreList;
    @FXML private javafx.scene.layout.Region themeRegion;

    private void updateThemeButtonText() {
        if (themeRegion != null) {
            themeRegion.getStyleClass().remove("icon-sun");
            themeRegion.getStyleClass().remove("icon-moon");

            if (Game2048.isGrayscale()) {
                themeRegion.getStyleClass().add("icon-moon");
            } else {
                themeRegion.getStyleClass().add("icon-sun");
            }
        }
    }

    // MENU ICON BUTTONS
    @FXML private Button quitIcon;
    @FXML private Button infoIcon;
    @FXML private Button newGameIcon;
    @FXML private Button themeToggleIcon;
    @FXML private Button playButton;
    @FXML private Button saveStatusIcon;
    @FXML private Button continueIcon;
    @FXML private Button settingsIcon;
    @FXML private Button scoreIcon;


    // --- FXML Injections for GAME SCENE ---
    @FXML private GridPane gameGrid;
    @FXML private Label scoreLabel;
    @FXML private Button undoButton;

    // OVERLAYS
    @FXML private VBox winMessageOverlay;
    @FXML private VBox highScoreOverlay;
    @FXML private VBox gameOverOverlay; // NEW: The container for Game Over elements

    // Elements inside overlays (kept for reference or if text needs changing)
    @FXML private Label gameOverMessage;
    @FXML private Button replayButton;
    @FXML private TextField nameInput;

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

    // Animation-related
    private final Map<Label, Animation> pendingAnimations = new HashMap<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public static GameController getActiveGameInstance() {
        return activeGameInstance;
    }

    public GameController getGameLogic() {
        return gameLogic;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (gameGrid != null) {
            // ... (Game Scene logic remains the same) ...
            tileLabels = new Label[][]{
                    {tile00, tile10, tile20, tile30}, {tile01, tile11, tile21, tile31},
                    {tile02, tile12, tile22, tile32}, {tile03, tile13, tile23, tile33}
            };
            gameGrid.setFocusTraversable(true);
            gameGrid.setOnKeyPressed(this::handleKeyPress);
            updateUndoButtonState();

            if (nameInput != null) {
                nameInput.setOnAction(this::onSubmitHighScore);
            }
        }
        else if (playButton != null) {
            // --- Main Menu Initialization ---
            boolean saveExists = GameController.saveFileExists(GameController.getSaveFile());
            boolean isSavePlayable = false;

            if (saveExists) {
                // Peek at the file to see if it's playable
                GameController tempLoad = GameController.loadGame(GameController.getSaveFile());
                if (tempLoad != null && !tempLoad.isGameOver()) {
                    isSavePlayable = true;
                }
            }
        }
    }

    private void updateUndoButtonState() {
        if (undoButton != null && gameLogic != null) {
            undoButton.setDisable(!gameLogic.canUndo());
        }
    }

    // --- Menu Handlers ---

    @FXML private void onInfoClick(ActionEvent event) { System.out.println("Info Clicked."); }
    @FXML private void onSettingsClick(ActionEvent event) { System.out.println("Settings Clicked."); }

    @FXML
    private void onScoreClick(ActionEvent event) {
        loadHighScoresScene();
    }

    @FXML
    private void onBackToMenuClick(ActionEvent event) {
        loadMainMenuScene();
    }

    // --- HIGH SCORE LOGIC ---

    private void loadHighScoresScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pateda/game2048/highscores.fxml"));
            Parent root = loader.load();
            SceneController controller = loader.getController();
            controller.setStage(stage);

            // Populate the list
            controller.populateHighScores();

            Scene scene = new Scene(root, 800, 800);
            Game2048.applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle("2048 - High Scores");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void populateHighScores() {
        if (highScoreList == null) return;

        // Load game data to get scores
        GameController data = GameController.loadGame(GameController.getSaveFile());
        List<HighScore> scores = data.getHighScores();

        highScoreList.getChildren().clear();

        int rank = 1;
        for (HighScore hs : scores) {
            HBox row = new HBox();
            row.getStyleClass().add("highscore-row");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setSpacing(20);
            row.setPadding(new javafx.geometry.Insets(10, 20, 10, 20));

            Label rankLbl = new Label("#" + rank++);
            rankLbl.setPrefWidth(60);
            rankLbl.getStyleClass().add("highscore-text");

            // NEW: Name Column
            Label nameLbl = new Label(hs.getName());
            nameLbl.setPrefWidth(150);
            nameLbl.getStyleClass().add("highscore-text");

            Label dateLbl = new Label(hs.getDate());
            dateLbl.setPrefWidth(130);
            dateLbl.getStyleClass().add("highscore-text");

            Label scoreLbl = new Label(String.valueOf(hs.getScore()));
            scoreLbl.setPrefWidth(120);
            scoreLbl.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            scoreLbl.getStyleClass().add("highscore-score");

            row.getChildren().addAll(rankLbl, nameLbl, dateLbl, scoreLbl);
            highScoreList.getChildren().add(row);
        }
    }

    @FXML
    private void onSubmitHighScore(ActionEvent event) {
        String name = nameInput.getText();

        // Save the score with the name
        gameLogic.addHighScore(name, gameLogic.getScore());

        // Save to file
        gameLogic.saveGame(GameController.getSaveFile());

        // Hide overlay
        highScoreOverlay.setVisible(false);

        // Navigate to the High Score screen to show the user their ranking
        loadHighScoresScene();
    }

    private void showHighScoreInput() {
        if (highScoreOverlay != null) {
            highScoreOverlay.setVisible(true);
            nameInput.setText("");
            nameInput.requestFocus();
        }
    }

    // --- Navigation Handlers ---

    @FXML
    private void onQuitButtonClick(ActionEvent event) {
        if (gameLogic != null) {
            gameLogic.saveGame(GameController.getSaveFile());
        }
        Platform.exit();
    }

    @FXML
    private void onNewGameButtonClick(ActionEvent event) {
        GameController newGame = new GameController();

        if (GameController.saveFileExists(GameController.getSaveFile())) {
            GameController oldData = GameController.loadGame(GameController.getSaveFile());
            if (oldData != null) {
                newGame.setHighScores(oldData.getHighScores());
            }
        }
        loadGameScene(newGame);
    }

    @FXML
    private void onPlayButtonClick(ActionEvent event) {
        boolean saveExists = GameController.saveFileExists(GameController.getSaveFile());

        if (saveExists) {
            GameController loadedGame = GameController.loadGame(GameController.getSaveFile());

            if (loadedGame.isGameOver()) {
                System.out.println("Saved game was over. Starting fresh.");
                GameController newGame = new GameController();
                newGame.setHighScores(loadedGame.getHighScores());
                loadGameScene(newGame);
            } else {
                System.out.println("Resuming active game.");
                loadGameScene(loadedGame);
            }
        } else {
            GameController newGame = new GameController();
            loadGameScene(newGame);
        }
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
        }
        requestGridFocus();
    }

    @FXML
    private void onContinuePlaying(ActionEvent event) {
        gameLogic.setContinuePlaying(true);
        winMessageOverlay.setVisible(false);
        requestGridFocus();
    }

    @FXML
    private void onReplayButtonClick(ActionEvent event) {
        GameController newGame = new GameController();

        if (GameController.saveFileExists(GameController.getSaveFile())) {
            GameController oldData = GameController.loadGame(GameController.getSaveFile());
            if (oldData != null) {
                newGame.setHighScores(oldData.getHighScores());
            }
        }
        loadGameScene(newGame);
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

        // Prevent input if any overlay is visible
        if ((winMessageOverlay != null && winMessageOverlay.isVisible()) ||
                (highScoreOverlay != null && highScoreOverlay.isVisible()) ||
                (gameOverOverlay != null && gameOverOverlay.isVisible())) {
            event.consume();
            return;
        }

        Direction direction = null;

        if (gameLogic.isGameOver()) {
            if (gameLogic.isHighScore(gameLogic.getScore())) {
                showHighScoreInput();
            } else {
                // UPDATE: Show the Game Over VBox Overlay
                if (gameOverOverlay != null) {
                    gameOverOverlay.setVisible(true);
                }
                gameLogic.saveGame(GameController.getSaveFile());
            }
            return;
        }

        // --- Determine Direction and Perform Move ---
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
                updateBoardUI();
            }
        }
        event.consume();
    }

    /**
     * Synchronizes the JavaFX UI (Labels) with the internal GameController state.
     */
    public void updateBoardUI() {
        int[][] currentBoard = gameLogic.getBoard();

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                int value = currentBoard[r][c];
                int oldValue = oldBoardState[r][c];
                Label currentLabel = tileLabels[r][c];

                currentLabel.getStyleClass().clear();
                currentLabel.getStyleClass().add("tile");

                boolean needsSpawnAnimation = (value > 0 && oldValue == 0);
                boolean needsPulseAnimation = (value > 0 && oldValue > 0 && value != oldValue);

                if (value > 0) {
                    currentLabel.setText(String.valueOf(value));
                    String className = value >= 4096 ? "tile-max" : "tile-" + value;
                    currentLabel.getStyleClass().add(className);
                } else {
                    currentLabel.setText("");
                    currentLabel.getStyleClass().add("tile-empty");
                }

                if (needsSpawnAnimation) {
                    currentLabel.setScaleX(0.7);
                    currentLabel.setScaleY(0.7);
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), currentLabel);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    st.play();
                } else if (needsPulseAnimation) {
                    ScaleTransition st = new ScaleTransition(Duration.millis(100), currentLabel);
                    st.setFromX(1.1);
                    st.setFromY(1.1);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    st.play();
                }

                if (!needsSpawnAnimation && !needsPulseAnimation) {
                    currentLabel.setScaleX(1.0);
                    currentLabel.setScaleY(1.0);
                }
            }
        }

        this.oldBoardState = deepCopy(currentBoard);

        scoreLabel.setText("SCORE: " + gameLogic.getScore());
        updateUndoButtonState();

        if (gameLogic.hasWon() && !gameLogic.isContinuePlaying()) {
            if (winMessageOverlay != null) {
                winMessageOverlay.setVisible(true);
            }
            if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
        } else if (gameLogic.isGameOver()) {
            if (!gameLogic.isHighScore(gameLogic.getScore())) {
                // UPDATE: Show Overlay instead of just Label/Button
                if (gameOverOverlay != null) gameOverOverlay.setVisible(true);
            }
        } else {
            // Hide all overlays if game is active
            if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
            if (winMessageOverlay != null) winMessageOverlay.setVisible(false);
        }
    }

    private int[][] deepCopy(int[][] source) {
        int[][] destination = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(source[i], 0, destination[i], 0, BOARD_SIZE);
        }
        return destination;
    }
}