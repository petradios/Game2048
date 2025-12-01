module com.pateda.game2048 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;


    opens com.pateda.game2048 to javafx.fxml;
    exports com.pateda.game2048;
}