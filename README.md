# JavaFX 2048 Game

A modern, feature-rich implementation of the classic **2048** puzzle game, built with JavaFX. This project features a clean UI, persistent high scores, undo functionality, and multiple visual themes including a Synthwave-inspired dark mode.


## Features



* **Classic Gameplay**: Merge tiles to reach 2048 and beyond.
* **Undo Move**: Made a mistake? Step back one move.
* **High Scores**: Tracks your top 10 best scores with player names and dates.
* **Save & Resume**: Game state is automatically saved on exit and can be resumed later.
* **Themes**: Toggle between a classic **Light Theme** (Beige/Brown) and a **Synthwave Dark Theme** (Charcoal/Neon).
* **Keyboard Support**: Full keyboard navigation for menus and gameplay.
* **Responsive UI**: Animations for tile merging and movement.


## Controls


### Main Menu



* **[ N ]** : Start New Game
* **[ C ] / [ Enter ]** : Continue Game (if save exists)
* **[ H ]** : High Scores
* **[ I ]** : Information / Help
* **[ T ]** : Toggle Theme
* **[ Q ]** : Quit


### In-Game



* **Arrow Keys / WASD** : Move Tiles
* **[ U ]** : Undo last move
* **[ Esc ]** : Back to Menu (Auto-saves)


## How to Build & Run


### Prerequisites



* **Java JDK 17+**
* **Maven**


### Running from Source



1. Clone the repository.
2. Open a terminal in the project root.
3. Run using the Maven JavaFX plugin: \
```mvn javafx:run```



### Building a Standalone JAR

To create a single, runnable JAR file (including all dependencies):



1. Run the Maven package command: \
```mvn clean package```

2. Navigate to the target/ directory.
3. Run the shaded JAR: \
```java -jar game2048-1.0-SNAPSHOT.jar```
 



## Project Structure



* ```src/main/java/com/pateda/game2048```: Java source code.
    * ```Game2048.java```: Main application class.
    * ```GameController.java```: Core game logic and state management.
    * ```SceneController.java```: UI interaction and scene switching.
    * ```Launcher.java```: Wrapper for standalone JAR execution.
* ```src/main/resources/com/pateda/game2048```: FXML layouts, CSS styles, and assets.


## Acknowledgments



* Original game concept by Gabriele Cirulli.
* Built with [JavaFX](https://openjfx.io/) and [Maven](https://maven.apache.org/).
* JSON persistence powered by [Jackson]
