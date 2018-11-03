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

public class Main extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		// Странная залупа, фиксящая визуальные баги на UNIX-подобных системах
		System.setProperty("prism.order", "sw");
		
		// Грузим шрифт кубача
		System.out.println("Loading font " + Font.loadFont(Main.class.getResource("resources/Minecraft.ttf").toString(), 10));
		
		// Парсим символьные глифы и коды OC-клавиш
		Glyph.initialize();
		KeyMap.initialize();
		
		// Чекаем, имеется ли конфиг и грузим его, либо создаем новый из ресурсов
		try {
			if (IO.configFile.exists()) {
				System.out.println("Loading config from " + IO.configFile.getPath());
				
				JSONObject loadedConfig = new JSONObject(IO.loadFileAsString(IO.configFile.toURI()));
				
				JSONArray configMachines = loadedConfig.getJSONArray("machines");
				if (configMachines.length() > 0) {
					for (int i = 0; i < configMachines.length(); i++)
						Machine.fromJSONObject(configMachines.getJSONObject(i));
				}
				else {
					Machine.generate();
				}
			}
			else {
				System.out.println("Config doesn't exists, creating a blank one");
				
				Machine.generate();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		// Сейвим конфиг при выходе из прожки
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				IO.saveConfig();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}));
	}
}
