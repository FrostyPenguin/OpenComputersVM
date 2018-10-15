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
import org.json.JSONArray;
import org.json.JSONObject;
import vm.computer.Glyph;
import vm.computer.KeyMap;
import vm.computer.Machine;
import vm.computer.Player;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends Application {
    // Синглтоны для гондонов
    public static Main instance;
    
    // Статики в гуихе тоже
    public GridPane windowGridPane;
    public ToggleButton powerButton;
    public Pane screensPane;
    public TextField EEPROMPathTextField, HDDPathTextField;

    // Ну, а жсон - всему голова
    private static final File configFile = new File(System.getProperty("user.home") + "/OpenComputersVM.json");

    @Override
    public void start(Stage primaryStage) throws Exception{
        System.out.println("Loading font: " + Font.loadFont(getClass().getResource("resources/Minecraft.ttf").toString(), 10));

        primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("VM.fxml"))));
        primaryStage.setTitle("OpenComputers VM");
        primaryStage.show();
    }
    
    public void initialize() {
        instance = this;

        // Парсим символьные глифы и коды OC-клавиш
        Glyph.initialize();
        KeyMap.initialize();
        
        // Грузим конфиг
        JSONObject config = null;
        if (configFile.exists()) {
            System.out.println("Loading config from: " + configFile.getAbsolutePath());
            
            try {
                config = new JSONObject(Main.loadFile(configFile.toURI()));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Config doesn't exists, loading a blank one");

            config = new JSONObject();
        }
        
        //Пидорасим текущее окошечко так, как надо
        windowGridPane.setPrefSize(config.getDouble("width"), config.getDouble("height"));

        // Ебурим наши машинки из конфига
        JSONArray machines = config.getJSONArray("machines");
        for (int i = 0; i < machines.length(); i++) {
            new Machine(machines.getJSONObject(i));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void saveConfig(JSONObject jsonObject) {
        System.out.println("Saving config...");

        try {
            Files.write(Paths.get(configFile.toURI()), jsonObject.toString().getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void onGenerateButtonTouch() {
        
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

    public static String loadFile(URI uri) throws IOException {
        return new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
    }

    public static String loadResource(String name) throws URISyntaxException, IOException {
        return loadFile(Main.class.getResource("resources/" + name).toURI());
    }
}
