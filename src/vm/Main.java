package vm;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

public class Main extends Application {
    // Синглтоны для гондонов
    public static Main instance;
    
    // Статики в гуихе тоже
    public GridPane windowGridPane;
    public ToggleButton powerButton;
    public Pane screensPane;
    public TextField EEPROMPathTextField, HDDPathTextField;

    // Ну, а жсон - всему голова
    private static final File configFile = new File(System.getProperty("user.home"), "OpenComputersVM.json");

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
        JSONObject loadedConfig = null;
        try {
            if (configFile.exists()) {
                System.out.println("Loading config from: " + configFile.getAbsolutePath());
                loadedConfig = new JSONObject(IO.loadFileAsString(configFile.toURI()));
            }
            else {
                System.out.println("Config doesn't exists, loading a blank one");
                loadedConfig = new JSONObject(IO.loadResourceAsString("defaults/Config.json"));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        // Пидорасим главное йоба-окошечко так, как надо
        windowGridPane.setPrefSize(loadedConfig.getDouble("width"), loadedConfig.getDouble("height"));

        // Ебурим наши машинки из конфига
        JSONArray configMachines = loadedConfig.getJSONArray("machines");
        for (int i = 0; i < configMachines.length(); i++) {
            Machine.add(configMachines.getJSONObject(i));
        }
        
        // Сейвим конфиг при выходе из прожки
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Generating config JSON...");

            JSONArray JSONMachines = new JSONArray();
            for (Machine machine : Machine.list) {
                JSONMachines.put(machine.toJSONObject());
            }
            
            try {
                System.out.println("Saving config file...");

                Files.write(
                    Paths.get(configFile.toURI()),
                    new JSONObject()
                        .put("width", windowGridPane.getWidth())
                        .put("height", windowGridPane.getHeight())
                        .put("machines", JSONMachines)
                    .toString(2).getBytes(StandardCharsets.UTF_8)
                );
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    public void onGenerateButtonTouch() throws IOException, URISyntaxException {
        try {
            System.out.println("Generating new machine...");

            // Грузим дефолтный конфиг машины и создаем жсон на его основе
            JSONObject generatedMachine = new JSONObject(IO.loadResourceAsString("defaults/Machine.json"));

            // Продрачиваем дефолтные компоненты и рандомим им ууидшники
            // Запоминаем адрес компонента файлосистемы, чтоб потом его в биос дату вхуячить
            String filesystemAddress = null, address;
            JSONObject component;
            JSONArray components = generatedMachine.getJSONArray("components");
            for (int i = 0; i < components.length(); i++) {
                component = components.getJSONObject(i);

                address = UUID.randomUUID().toString();
                component.put("address", address);

                // Попутно создаем директории харда
                if (component.getString("type").equals("filesystem")) {
                    filesystemAddress = address;
                    component.put("path", HDDPathTextField.getText());
                }
            }

            // А терь ищем еепром и сеттим ему полученный адрес харда
            for (int i = 0; i < components.length(); i++) {
                component = components.getJSONObject(i);

                if (component.getString("type").equals("eeprom")) {
                    component.put("data", filesystemAddress);
                    component.put("path", EEPROMPathTextField.getText());
                    break;
                }
            }

            // Усе, уася, машинка готова
            Machine.add(generatedMachine);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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
}
