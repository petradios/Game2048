package com.pateda.game2048;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main JavaFX Application class.
 * Handles stage setup, scene loading, and global theme management.
 */
public class Game2048 extends Application {

    private static final String DEFAULT_CSS = "/com/pateda/game2048/styles.css";
    private static final String DARK_THEME_CSS = "/com/pateda/game2048/dark-styles.css";

    // Tracks the current visual theme application-wide
    private static boolean isDarkTheme = false;

    @Override
    public void start(Stage stage) throws IOException {
        // Load the initial view (Main Menu)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pateda/game2048/main-menu.fxml"));
        Parent root = loader.load();

        // Pass stage reference to controller for scene switching
        SceneController sceneController = loader.getController();
        sceneController.setStage(stage);

        // Set application icon
        Image iconImage = new Image(getClass().getResourceAsStream("/com/pateda/game2048/logo.png"));
        stage.getIcons().add(iconImage);

        // Initialize scene and apply default/saved theme
        Scene scene = new Scene(root, 800, 800);
        applyTheme(scene);

        // Save active game state when user closes the window
        stage.setOnCloseRequest(event -> {
            GameController gameToSave = SceneController.getActiveGameInstance();
            if (gameToSave != null) {
                gameToSave.saveGame(GameController.getSaveFile());
                System.out.println("Game state saved on application exit.");
            }
        });

        stage.setTitle("2048");
        stage.setScene(scene);
        stage.show();
    }

    // Switches between Light and Dark modes and refreshes the scene
    public static void toggleTheme(Scene scene) {
        isDarkTheme = !isDarkTheme;
        applyTheme(scene);
    }

    // Applies the appropriate CSS stylesheet based on current state
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        scene.getStylesheets().clear();
        if (isDarkTheme) {
            scene.getStylesheets().add(Game2048.class.getResource(DARK_THEME_CSS).toExternalForm());
        } else {
            scene.getStylesheets().add(Game2048.class.getResource(DEFAULT_CSS).toExternalForm());
        }
    }

    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    public static void main(String[] args) {
        launch();
    }
}