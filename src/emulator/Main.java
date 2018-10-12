package emulator;

import emulator.computer.Glyph;
import emulator.computer.KeyMap;
import emulator.computer.Machine;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class Main extends Application {
    public TextArea textArea;
    public GridPane windowGridPane;
    public ImageView screenImageView;
    
    private Machine machine;
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("Emulator.fxml"));
        Scene scene = new Scene(root);
        
        primaryStage.setScene(scene);
        primaryStage.setTitle("JavaFX OpenComputers Emulator");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    public void initialize() {
        Glyph.initialize();
        KeyMap.initialize();
        
        machine = new Machine();
    }

    public void onConnectButtonTouch() {
        machine.boot(screenImageView, textArea.getText());
    }
}
