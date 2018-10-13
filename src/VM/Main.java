package VM;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {
    public GridPane windowGridPane;
    public ToggleButton powerButton;
    public VBox toolVBox;
    public Pane screensPane;
    public TextField EEPROMPathTextField, HDDPathTextField;
    
    public static Machine currentMachine;
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("VM.fxml"));
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
        
    }
    
    public static void addStyleSheet(Region control, String styleName) {
        control.getStylesheets().add(Main.class.getResource("../styles/" + styleName).toString());
    }
    
    public void onGenerateButtonTouch() {
        currentMachine = new Machine(EEPROMPathTextField.getText(), HDDPathTextField.getText(), screensPane, powerButton);
    }
    
    public void onPowerButtonTouch() {
        new Player("click.mp3").play();
        
        if (currentMachine == null) {
            powerButton.setSelected(false);
        }
        else {
            if (powerButton.isSelected()) {
                currentMachine.shutdown();
            } else {
                currentMachine.boot();
            }
        }
    }
}
