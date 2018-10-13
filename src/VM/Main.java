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
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {
    public GridPane windowGridPane;
    public ToggleButton powerButton;
    public Pane screensPane;
    public TextField EEPROMPathTextField, HDDPathTextField;
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("VM.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.setTitle("OpenComputers VM");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    public void initialize() {
        Glyph.initialize();
        KeyMap.initialize();
        
        StaticControls.powerButton = powerButton;
        StaticControls.screensPane = screensPane;
        StaticControls.EEPROMPathTextField = EEPROMPathTextField;
        StaticControls.HDDPathTextField = HDDPathTextField;
        StaticControls.windowGridPane = windowGridPane;
    }
    
    public static void addStyleSheet(Region control, String styleName) {
        control.getStylesheets().add(Main.class.getResource("../styles/" + styleName).toString());
    }
    
    public void onGenerateButtonTouch() {
       new Machine();
    }
    
    public void onPowerButtonTouch() {
        new Player("click.mp3").play();
        
        if (Machine.current == null) {
            powerButton.setSelected(false);
        }
        else {
            if (powerButton.isSelected()) {
                Machine.current.shutdown();
            } else {
                Machine.current.boot();
            }
        }
    }
}
