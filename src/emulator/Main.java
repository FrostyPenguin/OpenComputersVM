package emulator;

import emulator.computer.Machine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
    public Pane screensPane;
    public TextArea textArea;
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("JavaFX OpenComputers Emulator");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void initialize() {
        Glyph.loadMap();
    }

    public void onConnectButtonTouch() {
        new Machine(screensPane, textArea.getText()).start();
    }
}
