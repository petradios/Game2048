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

            // Assuming standard isGrayscale check based on your Game2048 class
            // Use Game2048.isGrayscale() if isDarkTheme() is not defined in Game2048
            if (Game2048.isDarkTheme()) {
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
    @FXML private VBox gameOverOverlay;

    // Elements inside overlays
    @FXML private Label gameOverMessage;
    @FXML private Button replayButton;
    @FXML private TextField nameInput;

    // Inject all 16 individual tile Labels
    @FXML private Label tile00, tile10, tile20, tile30;
    @FXML private Label tile01, tile11, tile21, tile31;
    @FXML private Label tile02, tile12, tile22, tile32;
    @FXML private Label tile03, tile13, tile23, tile33;

    // --- Internal State ---
    private GameController gameLogic;
    private Label[][] tileLabels;
    private Stage stage;
    private int[][] oldBoardState;
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
        // 1. GAME SCENE LOGIC
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
        // 2. MAIN MENU LOGIC
        else if (playButton != null) {
            boolean saveExists = GameController.saveFileExists(GameController.getSaveFile());

            // --- FIX START: Disable Focus Traversal ---
            // This prevents buttons from "stealing" the Space/Enter keys.
            // Now, only your handleMenuKeyPress method controls the input.
            quitIcon.setFocusTraversable(false);
            infoIcon.setFocusTraversable(false);
            newGameIcon.setFocusTraversable(false);
            themeToggleIcon.setFocusTraversable(false);
            playButton.setFocusTraversable(false);
            scoreIcon.setFocusTraversable(false);
            // --- FIX END ---

            // Attach Menu Keyboard Listener
            Platform.runLater(() -> {
                if (playButton.getScene() != null) {
                    playButton.getScene().setOnKeyPressed(this::handleMenuKeyPress);

                    // Optional: Request focus on the container so no button is highlighted
                    playButton.getParent().requestFocus();
                }
            });
        }
        // 3. HIGH SCORES SCENE LOGIC
        else if (highScoreList != null) {
            Platform.runLater(() -> {
                if (highScoreList.getScene() != null) {
                    highScoreList.getScene().setOnKeyPressed(this::handleBackKeyOnly);
                }
            });
        }
    }

    // --- KEYBOARD HANDLERS (NEW) ---

    /**
     * Handles keyboard input for the Main Menu.
     */
    private void handleMenuKeyPress(KeyEvent event) {
        // If Confirmation Overlay is visible, only allow Escape (to cancel) or Enter (to confirm)
        if (newGameConfirmationOverlay != null && newGameConfirmationOverlay.isVisible()) {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.N) {
                onCancelNewGame(null);
            } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.Y) { // Y for Yes
                onConfirmNewGame(null);
            }
            return; // Block other inputs
        }

        switch (event.getCode()) {
            case Q -> onQuitButtonClick(null);
            case H, S -> onScoreClick(null);
            case I -> onInfoClick(null);
            case N -> onNewGameButtonClick(null);
            case T -> onThemeToggle(null);
            case ENTER, SPACE -> onPlayButtonClick(null);
        }
    }

    /**
     * Handles keyboard input for Info and High Score screens (Escape to go back).
     */
    private void handleBackKeyOnly(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            onBackToMenuClick(null);
        }
    }

    private void updateUndoButtonState() {
        if (undoButton != null && gameLogic != null) {
            undoButton.setDisable(!gameLogic.canUndo());
        }
    }

    // --- Menu Handlers ---

    @FXML
    private void onInfoClick(ActionEvent event) {
        loadInfoScene();
    }

    @FXML private void onSettingsClick(ActionEvent event) { System.out.println("Settings Clicked."); }

    @FXML
    private void onScoreClick(ActionEvent event) {
        loadHighScoresScene();
    }

    @FXML
    private void onBackToMenuClick(ActionEvent event) {
        loadMainMenuScene();
    }

    // --- SCENE LOADING LOGIC ---

    private void loadInfoScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pateda/game2048/info.fxml"));
            Parent root = loader.load();
            SceneController controller = loader.getController();
            controller.setStage(stage);

            Scene scene = new Scene(root, 800, 800);

            // KEYBOARD FIX: Attach Back Listener to Info Scene
            scene.setOnKeyPressed(controller::handleBackKeyOnly);

            Game2048.applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle("2048 - Information");

        } catch (IOException e) {
            System.err.println("Error loading info.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadHighScoresScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pateda/game2048/highscores.fxml"));
            Parent root = loader.load();
            SceneController controller = loader.getController();
            controller.setStage(stage);

            // Populate the list
            controller.populateHighScores();

            Scene scene = new Scene(root, 800, 800);

            // KEYBOARD FIX: Attach Back Listener to High Score Scene
            scene.setOnKeyPressed(controller::handleBackKeyOnly);

            Game2048.applyTheme(scene);
            stage.setScene(scene);
            stage.setTitle("2048 - High Scores");

        } catch (IOException e) {
            e.printStackTrace();
        }
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
    @FXML private VBox newGameConfirmationOverlay;

    // --- Updated Navigation Handlers ---

    @FXML
    private void onNewGameButtonClick(ActionEvent event) {
        boolean saveExists = GameController.saveFileExists(GameController.getSaveFile());

        if (saveExists) {
            // Check if the saved game is actually active or already game over
            GameController savedGame = GameController.loadGame(GameController.getSaveFile());
            if (savedGame != null && !savedGame.isGameOver()) {
                // Game is active -> Show Confirmation Prompt
                if (newGameConfirmationOverlay != null) {
                    newGameConfirmationOverlay.setVisible(true);
                }
                return; // Stop here, wait for user response
            }
        }

        // If no save, or save is Game Over -> Start Immediately
        launchNewGame();
    }

    @FXML
    private void onConfirmNewGame(ActionEvent event) {
        if (newGameConfirmationOverlay != null) {
            newGameConfirmationOverlay.setVisible(false);
        }
        launchNewGame();
    }

    @FXML
    private void onCancelNewGame(ActionEvent event) {
        if (newGameConfirmationOverlay != null) {
            newGameConfirmationOverlay.setVisible(false);
        }
    }

    // Helper method to actually start the game logic
    private void launchNewGame() {
        GameController newGame = new GameController();

        // Preserve High Scores
        if (GameController.saveFileExists(GameController.getSaveFile())) {
            GameController oldData = GameController.loadGame(GameController.getSaveFile());
            if (oldData != null) {
                newGame.setHighScores(oldData.getHighScores());
            }
        }
        loadGameScene(newGame);
    }
    // --- HIGH SCORE LOGIC ---

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

    public void requestGridFocus() {
        if (gameGrid != null) {
            Platform.runLater(() -> gameGrid.requestFocus());
        }
    }

    // RENAMED: This handles key press specifically for the GAME scene
    @FXML
    private void handleGameKeyPress(KeyEvent event) {
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