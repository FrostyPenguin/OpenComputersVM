package vm;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import vm.computer.Glyph;
import vm.computer.KeyMap;
import vm.computer.Machine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		// Грузим шрифт кубача
		System.out.println("Loading font " + Font.loadFont(Main.class.getResource("resources/Minecraft.ttf").toString(), 10));
		
		// Парсим символьные глифы и коды OC-клавиш
		Glyph.initialize();
		KeyMap.initialize();
		
		// Чекаем, имеется ли конфиг и грузим его, либо создаем новый из ресурсов
		JSONObject loadedConfig;
		try {
			if (IO.configFile.exists()) {
				System.out.println("Loading config from " + IO.configFile.getPath());
				
				loadedConfig = new JSONObject(IO.loadFileAsString(IO.configFile.toURI()));

				// Ебурим наши машинки из конфига
				JSONArray configMachines = loadedConfig.getJSONArray("machines");
				for (int i = 0; i < configMachines.length(); i++) {
					Machine.fromJSONObject(configMachines.getJSONObject(i));
				}
			}
			else {
				System.out.println("Config doesn't exists, creating a blank one");
				
				// Генерим шаблонную машину
				Machine.generate();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		// Сейвим конфиг при выходе из прожки
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			JSONArray JSONMachines = new JSONArray();
			for (Machine machine : Machine.list) {
				JSONMachines.put(machine.toJSONObject());
			}

			try {
				System.out.println("Saving config file...");

				Files.write(
					Paths.get(IO.configFile.toURI()),
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
}
