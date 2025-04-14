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
import java.util.Objects;

public class Main extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		// Correctif étrange des bugs visuels sur les systèmes de type UNIX
		System.setProperty("prism.order", "sw");

		// Chargement de la police Minecraft
		System.out.println("Chargement de la police " + Font.loadFont(Objects.requireNonNull(Main.class.getResource("resources/Minecraft.ttf")).toString(), 10));

		// Analyse des glyphes de caractères et des codes de touches OC
		Glyph.initialize();
		KeyMap.initialize();

		// Vérifie si la configuration existe et la charge, sinon en crée une nouvelle à partir des ressources
		try {
			if (IO.configFile.exists()) {
				System.out.println("Chargement de la configuration depuis " + IO.configFile.getPath());

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
				System.out.println("La configuration n'existe pas, création d'une nouvelle configuration vide");

				Machine.generate();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		// Enregistre la configuration lors de la fermeture de l'application
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
