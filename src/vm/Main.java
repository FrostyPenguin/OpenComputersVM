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
import java.nio.file.StandardCopyOption;
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
    private static final File dataFile = new File(System.getProperty("user.home") + "/OpenComputersVM/");
    private static final File configFile = new File(dataFile, "Config.json");
    private static final File machinesFile = new File(dataFile, "Machines/");

    @Override
    public void start(Stage primaryStage) throws Exception{
        System.out.println("Loading font: " + Font.loadFont(getClass().getResource("resources/Minecraft.ttf").toString(), 10));

        primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("VM.fxml"))));
        primaryStage.setTitle("OpenComputers VM");
        primaryStage.show();
    }
    
    public void initialize() {
        instance = this;
        
        // Создадим директорию для всей залупы, а то вдрух ее нет
        System.out.println("Creating data directories: " + dataFile.mkdirs());

        // Парсим символьные глифы и коды OC-клавиш
        Glyph.initialize();
        KeyMap.initialize();
        
        // Грузим конфиг
        JSONObject loadedConfig = null;
        try {
            if (configFile.exists()) {
                System.out.println("Loading config from: " + configFile.getAbsolutePath());
                loadedConfig = new JSONObject(Main.loadFile(configFile.toURI()));
            }
            else {
                System.out.println("Config doesn't exists, loading a blank one");
                loadedConfig = new JSONObject(loadResource("defaults/Config.json"));
            }
        }
        catch (URISyntaxException | IOException e) {
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
                JSONArray JSONComponents = new JSONArray();
                for (int j = 0; j < machine.componentAPI.list.size(); j++) {
                    JSONComponents.put(machine.componentAPI.list.get(j).toJSONObject());
                }

                JSONMachines.put(
                    new JSONObject()
                        .put("name", machine.name)
                        .put("x", machine.screenWidget.getLayoutX())
                        .put("y", machine.screenWidget.getLayoutY())
                        .put("scale", machine.screenWidget.scale)
                        .put("components", JSONComponents)
                );
            }
            
            try {
                System.out.println("Saving config file...");
                
                Files.write(
                    Paths.get(configFile.toURI()),
                    new JSONObject()
                        .put("width", windowGridPane.getPrefWidth())
                        .put("height", windowGridPane.getPrefHeight())
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
    
    public void onGenerateButtonTouch() {
        try {
            System.out.println("Generating new machine...");
            
            // Грузим дефолтный конфиг машины и создаем жсон на его основе
            JSONObject generatedMachine = new JSONObject(loadResource("defaults/Machine.json"));

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

                    File HDDFile = new File(machinesFile, filesystemAddress + "/HDD/");
                    System.out.println("Creating HDD directory at " + HDDFile.toString() + ": " + HDDFile.mkdirs());

                    component.put("path", HDDFile.toString());
                }
            }

            // А терь ищем еепром и сеттим ему полученный адрес харда
            for (int i = 0; i < components.length(); i++) {
                component = components.getJSONObject(i);
                
                if (component.getString("type").equals("eeprom")) {
                    File EEPROMFile = new File(machinesFile, filesystemAddress + "/EEPROM.lua");
                    
                    System.out.println("Creating default EEPROM firmware file at " + EEPROMFile.toString());

                    Files.copy(
                        Paths.get(Main.getResource("defaults/EEPROM.lua")),
                        Paths.get(EEPROMFile.toURI()),
                        StandardCopyOption.REPLACE_EXISTING
                    );
                    
                    component.put("data", filesystemAddress);
                    component.put("path", EEPROMFile.toString());
                }
            }
            
            Machine.add(generatedMachine);
        }
        catch (URISyntaxException | IOException e) {
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

    public static void addStyleSheet(Region control, String styleName) {
        control.getStylesheets().add(Main.class.getResource("styles/" + styleName).toString());
    }

    public static String loadFile(URI uri) throws IOException {
        return new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
    }
    
    public static URI getResource(String name) throws URISyntaxException {
        return Main.class.getResource("resources/" + name).toURI();
    }

    public static String loadResource(String name) throws URISyntaxException, IOException {
        return loadFile(getResource(name));
    }
}
