package vm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import vm.computer.Glyph;
import vm.computer.KeyMap;
import vm.computer.Machine;
import vm.computer.Player;

public class Main extends Application {
    // Синглтоны для гондонов
    public static Main instance;
    
    public GridPane windowGridPane;
    public ToggleButton powerButton;
    public Pane screensPane;
    public TextField EEPROMPathTextField, HDDPathTextField;
    
    @Override
    public void start(Stage primaryStage) throws Exception{
        instance = this;
        
        System.out.println("Loading font: " + Font.loadFont(getClass().getResource("resources/Minecraft.ttf").toString(), 10));

        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("VM.fxml")));
        primaryStage.setScene(scene);
        primaryStage.setTitle("OpenComputers VM");
        primaryStage.show();
    }
    
    public void initialize() {
        Glyph.initialize();
        KeyMap.initialize();
        
        Main.instance.powerButton = powerButton;
        Main.instance.screensPane = screensPane;
        Main.instance.EEPROMPathTextField = EEPROMPathTextField;
        Main.instance.HDDPathTextField = HDDPathTextField;
        Main.instance.windowGridPane = windowGridPane;
    }

    public static void main(String[] args) {
        launch(args);
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
                Machine.current.shutdown(true);
            } else {
                Machine.current.boot();
            }
        }
    }

    public static void addStyleSheet(Region control, String styleName) {
        control.getStylesheets().add(Main.class.getResource("styles/" + styleName).toString());
    }
}
