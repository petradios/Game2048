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
import java.util.List;
import java.util.ResourceBundle;

/**
 * UI Controller for all scenes (Menu, Game, High Score, Info).
 * Handles user input, animations, and scene switching.
 */
public class SceneController implements Initializable {

    // --- Global Reference ---
    private static GameController activeGameInstance = null;

    // --- Menu UI Elements ---
    @FXML private Button quitIcon;
    @FXML private Button infoIcon;
    @FXML private Button newGameIcon;
    @FXML private Button themeToggleIcon;
    @FXML private Button playButton;
    @FXML private Button scoreIcon;
    @FXML private VBox highScoreList;
    @FXML private javafx.scene.layout.Region themeRegion;
    @FXML private VBox newGameConfirmationOverlay;

    // --- Game UI Elements ---
    @FXML private GridPane gameGrid;
    @FXML private Label scoreLabel;
    @FXML private Button undoButton;
    @FXML private Label tile00, tile10, tile20, tile30;
    @FXML private Label tile01, tile11, tile21, tile31;
    @FXML private Label tile02, tile12, tile22, tile32;
    @FXML private Label tile03, tile13, tile23, tile33;

    // --- Overlays ---
    @FXML private VBox winMessageOverlay;
    @FXML private VBox highScoreOverlay;
    @FXML private VBox gameOverOverlay;
    @FXML private Label gameOverMessage;
    @FXML private Button replayButton;
    @FXML private TextField nameInput;

    // --- Internal State ---
    private GameController gameLogic;
    private Label[][] tileLabels;
    private Stage stage;
    private int[][] oldBoardState; // For animation comparison
    private static final int BOARD_SIZE = 4;

    // --- Setters/Getters ---
    public void setStage(Stage stage) { this.stage = stage; }
    public static GameController getActiveGameInstance() { return activeGameInstance; }
    public GameController getGameLogic() { return gameLogic; }

    /**
     * Initializes the controller class.
     * Detects which scene is loaded based on injected elements and configures it.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Game Scene Initialization
        if (gameGrid != null) {
            tileLabels = new Label[][]{
                    {tile00, tile10, tile20, tile30}, {tile01, tile11, tile21, tile31},
                    {tile02, tile12, tile22, tile32}, {tile03, tile13, tile23, tile33}
            };
            gameGrid.setFocusTraversable(true);
            gameGrid.setOnKeyPressed(this::handleGameKeyPress);
            updateUndoButtonState();

            if (nameInput != null) {
                nameInput.setOnAction(this::onSubmitHighScore);
            }
        }
        // 2. Main Menu Initialization
        else if (playButton != null) {
            // Prevent buttons from stealing keyboard focus
            disableMenuFocus();

            // Attach keyboard listener once scene is ready
            Platform.runLater(() -> {
                if (playButton.getScene() != null) {
                    playButton.getScene().setOnKeyPressed(this::handleMenuKeyPress);
                    playButton.getParent().requestFocus();
                }
            });
        }
        // 3. High Scores / Info Initialization
        else if (highScoreList != null) {
            Platform.runLater(() -> {
                if (highScoreList.getScene() != null) {
                    highScoreList.getScene().setOnKeyPressed(this::handleBackKeyOnly);
                }
            });
        }
    }

    // Disables focus traversal so buttons don't capture Arrow/Space keys
    private void disableMenuFocus() {
        if (quitIcon != null) quitIcon.setFocusTraversable(false);
        if (infoIcon != null) infoIcon.setFocusTraversable(false);
        if (newGameIcon != null) newGameIcon.setFocusTraversable(false);
        if (themeToggleIcon != null) themeToggleIcon.setFocusTraversable(false);
        if (playButton != null) playButton.setFocusTraversable(false);
        if (scoreIcon != null) scoreIcon.setFocusTraversable(false);
    }

    // --- Keyboard Handlers ---

    // Handles Game Scene inputs (Arrows/WASD for movement)
    @FXML
    private void handleGameKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            onBackButtonClick(null);
            event.consume();
            return;
        }

        // Block input if overlay is active
        if (isOverlayVisible()) {
            event.consume();
            return;
        }

        Direction direction = null;

        // Game Over / High Score Check logic
        if (gameLogic.isGameOver()) {
            if (gameLogic.isHighScore(gameLogic.getScore())) {
                showHighScoreInput();
            } else {
                if (gameOverOverlay != null) gameOverOverlay.setVisible(true);
                gameLogic.saveGame(GameController.getSaveFile());
            }
            return;
        }

        // Mapping Keys to Directions
        switch (event.getCode()) {
            case UP, W -> direction = Direction.UP;
            case DOWN, S -> direction = Direction.DOWN;
            case LEFT, A -> direction = Direction.LEFT;
            case RIGHT, D -> direction = Direction.RIGHT;
        }

        if (direction != null) {
            if (gameLogic.handleMove(direction)) {
                updateBoardUI();
            }
        }
        event.consume();
    }

    // Handles Main Menu inputs (Shortcuts)
    private void handleMenuKeyPress(KeyEvent event) {
        // Handle Confirmation Overlay Inputs
        if (newGameConfirmationOverlay != null && newGameConfirmationOverlay.isVisible()) {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.N) {
                onCancelNewGame(null);
            } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.Y) {
                onConfirmNewGame(null);
            }
            return;
        }

        // Menu Shortcuts
        switch (event.getCode()) {
            case Q -> onQuitButtonClick(null);
            case H, S -> onScoreClick(null);
            case I -> onInfoClick(null);
            case N -> onNewGameButtonClick(null);
            case T -> onThemeToggle(null);
            case ENTER, SPACE -> onPlayButtonClick(null);
        }
    }

    // Handles Back/Escape for Info and Scores
    private void handleBackKeyOnly(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            onBackToMenuClick(null);
        }
    }

    // --- UI Update Logic ---

    // Syncs UI Grid with Internal Board State
    public void updateBoardUI() {
        int[][] currentBoard = gameLogic.getBoard();

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                int value = currentBoard[r][c];
                int oldValue = oldBoardState[r][c];
                Label currentLabel = tileLabels[r][c];

                currentLabel.getStyleClass().clear();
                currentLabel.getStyleClass().add("tile");

                // Determine if animation is needed
                boolean spawn = (value > 0 && oldValue == 0);
                boolean merge = (value > 0 && oldValue > 0 && value != oldValue);

                // Update text and style class based on value
                if (value > 0) {
                    currentLabel.setText(String.valueOf(value));
                    currentLabel.getStyleClass().add(value >= 4096 ? "tile-max" : "tile-" + value);
                } else {
                    currentLabel.setText("");
                    currentLabel.getStyleClass().add("tile-empty");
                }

                // Apply animations
                if (spawn) {
                    animateTile(currentLabel, 0.7, 150);
                } else if (merge) {
                    animatePulse(currentLabel);
                } else {
                    currentLabel.setScaleX(1.0);
                    currentLabel.setScaleY(1.0);
                }
            }
        }

        this.oldBoardState = deepCopy(currentBoard);
        scoreLabel.setText("SCORE: " + gameLogic.getScore());
        updateUndoButtonState();
        checkGameStatusOverlays();
    }

    private void animateTile(Label label, double startScale, int duration) {
        label.setScaleX(startScale);
        label.setScaleY(startScale);
        ScaleTransition st = new ScaleTransition(Duration.millis(duration), label);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    private void animatePulse(Label label) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), label);
        st.setFromX(1.1);
        st.setFromY(1.1);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }

    private void updateUndoButtonState() {
        if (undoButton != null && gameLogic != null) {
            undoButton.setDisable(!gameLogic.canUndo());
        }
    }

    private void updateThemeButtonText() {
        if (themeRegion != null) {
            themeRegion.getStyleClass().remove("icon-sun");
            themeRegion.getStyleClass().remove("icon-moon");
            if (Game2048.isDarkTheme()) {
                themeRegion.getStyleClass().add("icon-moon");
            } else {
                themeRegion.getStyleClass().add("icon-sun");
            }
        }
    }

    private boolean isOverlayVisible() {
        return (winMessageOverlay != null && winMessageOverlay.isVisible()) ||
                (highScoreOverlay != null && highScoreOverlay.isVisible()) ||
                (gameOverOverlay != null && gameOverOverlay.isVisible());
    }

    private void checkGameStatusOverlays() {
        if (gameLogic.hasWon() && !gameLogic.isContinuePlaying()) {
            if (winMessageOverlay != null) winMessageOverlay.setVisible(true);
            if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
        } else if (gameLogic.isGameOver()) {
            if (!gameLogic.isHighScore(gameLogic.getScore())) {
                if (gameOverOverlay != null) gameOverOverlay.setVisible(true);
            }
        } else {
            if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
            if (winMessageOverlay != null) winMessageOverlay.setVisible(false);
        }
    }

    // --- Navigation & Action Handlers ---

    @FXML private void onInfoClick(ActionEvent event) { loadInfoScene(); }
    @FXML private void onScoreClick(ActionEvent event) { loadHighScoresScene(); }
    @FXML private void onBackToMenuClick(ActionEvent event) { loadMainMenuScene(); }
    @FXML private void onThemeToggle(ActionEvent event) { Game2048.toggleTheme(stage.getScene()); updateThemeButtonText(); }
    @FXML private void onQuitButtonClick(ActionEvent event) {
        if (gameLogic != null) gameLogic.saveGame(GameController.getSaveFile());
        Platform.exit();
    }

    @FXML
    private void onNewGameButtonClick(ActionEvent event) {
        if (GameController.saveFileExists(GameController.getSaveFile())) {
            GameController saved = GameController.loadGame(GameController.getSaveFile());
            if (saved != null && !saved.isGameOver()) {
                if (newGameConfirmationOverlay != null) newGameConfirmationOverlay.setVisible(true);
                return;
            }
        }
        launchNewGame();
    }

    @FXML
    private void onPlayButtonClick(ActionEvent event) {
        if (GameController.saveFileExists(GameController.getSaveFile())) {
            GameController loaded = GameController.loadGame(GameController.getSaveFile());
            if (loaded.isGameOver()) {
                launchNewGame(); // Start fresh if saved game was over
            } else {
                loadGameScene(loaded); // Resume
            }
        } else {
            launchNewGame();
        }
    }

    private void launchNewGame() {
        GameController newGame = new GameController();
        // Preserve High Scores from save file if it exists
        if (GameController.saveFileExists(GameController.getSaveFile())) {
            GameController oldData = GameController.loadGame(GameController.getSaveFile());
            if (oldData != null) newGame.setHighScores(oldData.getHighScores());
        }
        loadGameScene(newGame);
    }

    @FXML
    private void onConfirmNewGame(ActionEvent event) {
        if (newGameConfirmationOverlay != null) newGameConfirmationOverlay.setVisible(false);
        launchNewGame();
    }

    @FXML
    private void onCancelNewGame(ActionEvent event) {
        if (newGameConfirmationOverlay != null) newGameConfirmationOverlay.setVisible(false);
    }

    @FXML private void onBackButtonClick(ActionEvent event) {
        if (gameLogic != null) gameLogic.saveGame(GameController.getSaveFile());
        loadMainMenuScene();
    }

    @FXML private void onUndoButtonClick(ActionEvent event) {
        if (gameLogic.undo()) {
            oldBoardState = deepCopy(gameLogic.getBoard());
            updateBoardUI();
        }
        requestGridFocus();
    }

    @FXML private void onContinuePlaying(ActionEvent event) {
        gameLogic.setContinuePlaying(true);
        winMessageOverlay.setVisible(false);
        requestGridFocus();
    }

    @FXML private void onReplayButtonClick(ActionEvent event) {
        launchNewGame(); // Replay logic is identical to starting new
    }

    @FXML private void onSubmitHighScore(ActionEvent event) {
        String name = nameInput.getText();
        gameLogic.addHighScore(name, gameLogic.getScore());
        gameLogic.saveGame(GameController.getSaveFile());
        highScoreOverlay.setVisible(false);
        loadHighScoresScene();
    }

    private void showHighScoreInput() {
        if (highScoreOverlay != null) {
            highScoreOverlay.setVisible(true);
            nameInput.setText("");
            nameInput.requestFocus();
        }
    }

    // --- Scene Loading Helpers ---

    private void loadInfoScene() {
        loadScene("/com/pateda/game2048/info.fxml", "2048 - Information", true);
    }

    private void loadHighScoresScene() {
        SceneController c = loadScene("/com/pateda/game2048/highscores.fxml", "2048 - High Scores", true);
        if (c != null) c.populateHighScores();
    }

    private void loadMainMenuScene() {
        loadScene("/com/pateda/game2048/main-menu.fxml", "2048", false);
        activeGameInstance = null;
    }

    private void loadGameScene(GameController controllerInstance) {
        SceneController c = loadScene("/com/pateda/game2048/game-scene.fxml", "2048 Game", false);
        if (c != null) {
            c.gameLogic = controllerInstance;
            activeGameInstance = controllerInstance;
            c.oldBoardState = c.deepCopy(controllerInstance.getBoard());
            c.updateBoardUI();
            c.requestGridFocus();
        }
    }

    // Generic Scene Loader to reduce code duplication
    private SceneController loadScene(String fxmlPath, String title, boolean attachBackListener) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            SceneController controller = loader.getController();
            controller.setStage(stage);

            Scene scene = new Scene(root, 800, 800);
            if (attachBackListener) {
                scene.setOnKeyPressed(controller::handleBackKeyOnly);
            }
            Game2048.applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle(title);
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void populateHighScores() {
        if (highScoreList == null) return;
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

            Label rankLbl = createLabel("#" + rank++, 60, "highscore-text");
            Label nameLbl = createLabel(hs.getName(), 150, "highscore-text");
            Label dateLbl = createLabel(hs.getDate(), 130, "highscore-text");
            Label scoreLbl = createLabel(String.valueOf(hs.getScore()), 120, "highscore-score");
            scoreLbl.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            row.getChildren().addAll(rankLbl, nameLbl, dateLbl, scoreLbl);
            highScoreList.getChildren().add(row);
        }
    }

    private Label createLabel(String text, double width, String styleClass) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.getStyleClass().add(styleClass);
        return lbl;
    }

    // --- Helper Methods ---

    private int[][] deepCopy(int[][] source) {
        int[][] destination = new int[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(source[i], 0, destination[i], 0, BOARD_SIZE);
        }
        return destination;
    }

    public void requestGridFocus() {
        if (gameGrid != null) {
            Platform.runLater(() -> gameGrid.requestFocus());
        }
    }
}