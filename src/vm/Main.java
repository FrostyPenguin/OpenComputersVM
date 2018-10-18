package vm;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import vm.computer.Glyph;
import vm.computer.KeyMap;
import vm.computer.Machine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends Application {
    // Жсон - всему голова
    private static final File dataFile = new File(System.getProperty("user.home"), "OpenComputersVM");
    private static final File configFile = new File(dataFile, "Config.json");
    private static final File librariesFile = new File(dataFile, "Libraries");

    @Override
    public void start(Stage primaryStage) {
        System.out.println("Loading font: " + Font.loadFont(Main.class.getResource("resources/Minecraft.ttf").toString(), 10));

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

        // Ебурим наши машинки из конфига
        JSONArray configMachines = loadedConfig.getJSONArray("machines");
        for (int i = 0; i < configMachines.length(); i++) {
            Machine.fromJSONObject(configMachines.getJSONObject(i));
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
}
