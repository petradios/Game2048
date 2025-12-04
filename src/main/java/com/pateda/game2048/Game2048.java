package com.pateda.game2048;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Game2048 extends Application {

    private static final String DEFAULT_CSS = "/com/pateda/game2048/styles.css";
    private static final String GRAYSCALE_CSS = "/com/pateda/game2048/dark-styles.css";

    // Static state to track the current theme
    private static boolean isDarkTheme = false;

    @Override
    public void start(Stage stage) throws IOException {
        // Load the Main Menu FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/pateda/game2048/main-menu.fxml"));
        Parent root = loader.load();



        SceneController sceneController = loader.getController();
        sceneController.setStage(stage);

        Image iconImage = new Image(getClass().getResourceAsStream("/com/pateda/game2048/logo.png"));
        stage.getIcons().add(iconImage);

        Scene scene = new Scene(root, 800, 800);
        // Load the current stylesheet based on the static state
        applyTheme(scene);

        // CRITICAL FIX: Use the static accessor to retrieve the active game instance for saving
        stage.setOnCloseRequest(event -> {
            GameController gameToSave = SceneController.getActiveGameInstance();
            if (gameToSave != null) {
                gameToSave.saveGame(GameController.getSaveFile());
                System.out.println("Game state saved on application exit.");
            } else {
                System.out.println("No active game to save.");
            }
        });

        stage.setTitle("2048");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Toggles the grayscale state and updates the current scene's stylesheet.
     */
    public static void toggleTheme(Scene scene) {
        isDarkTheme = !isDarkTheme;
        applyTheme(scene);
    }

    /**
     * Applies the correct stylesheet based on the current static theme state.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        scene.getStylesheets().clear();
        if (isDarkTheme) {
            scene.getStylesheets().add(Game2048.class.getResource(GRAYSCALE_CSS).toExternalForm());
        } else {
            scene.getStylesheets().add(Game2048.class.getResource(DEFAULT_CSS).toExternalForm());
        }
    }

    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    static void main(String[] args) {
        launch();
    }
}