package vm.computer;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaStateFiveThree;
import li.cil.repack.com.naef.jnlua.NativeSupport;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import vm.IO;
import vm.Main;
import vm.computer.api.APIBase;
import vm.computer.api.Component;
import vm.computer.api.Computer;
import vm.computer.api.Unicode;
import vm.computer.components.*;
import vm.computer.components.base.ComponentBase;
import vm.computer.components.base.ComponentWindowed;
import vm.computer.components.base.UnknownComponent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class Machine {
	public static final ArrayList<Machine> list = new ArrayList<>();

	// Objets de l'interface graphique
	public AnchorPane sceneAnchorPane;
	public VBox propertiesVBox;
	public Slider RAMSlider, volumeSlider;
	public ImageView boardImageView;
	public ToggleButton powerButton;
	public TextField EEPROMPathTextField, HDDPathTextField, playerTextField;

	public int xOffset = 0, yOffset = 0;

	// Fonctionnalités de la machine
	public ArrayList<APIBase> APIList = new ArrayList<>();

	public boolean started = false;
	public long startTime;
	public LuaState lua;
	public LuaThread luaThread;
	public Component componentAPI;
	public Computer computerAPI;
	public Unicode unicodeAPI;

	public HashMap<String, HashMap<String, ComponentBase>> listComponents = new HashMap<>();
	public String temporaryFilesystemComponent = null;

	public HashMap<String, UnknownComponent> unknownComponents = new HashMap<>();
	public HashSet<String> users = new HashSet<>();
	public Player player = new Player();

	public Stage stage;

	// Le constructeur vide est nécessaire pour l'initialisation par FXML
	public Machine() {

	}

	public ComponentBase getFirstComponent(String type) {
		if (!this.listComponents.containsKey(type))
			return null;
		return this.listComponents.get(type).get(this.listComponents.get(type).keySet().toArray()[0]);
	}

	public static void fromJSONObject(JSONObject machineConfig) {
		try {
			// Crée une fenêtre, charge le fichier FXML et définit la scène pour la fenêtre
			Stage stage = new Stage();
			FXMLLoader fxmlLoader = new FXMLLoader(Machine.class.getResource("Window.fxml"));
			stage.setScene(new Scene(fxmlLoader.load()));

			// Récupère la machine à partir du contrôleur FXML et mémorise cette instance de Stage
			Machine machine = fxmlLoader.getController();
			machine.stage = stage;

			// Crée les APIs
			machine.computerAPI = new Computer(machine);
			machine.componentAPI = new Component(machine);
			machine.unicodeAPI = new Unicode(machine);

			// Initialise les composants à partir de la configuration de la machine
			JSONArray components = machineConfig.getJSONArray("components");
			JSONObject component;
			String address;
			String type;
			for (int i = 0; i < components.length(); i++) {
				component = components.getJSONObject(i);
				address = component.optString("address",UUID.randomUUID().toString());
				type = component.optString("type","type_name").toLowerCase();

				Class<?> clazz = null;

				try {
					if (type.isEmpty() || type.contains("."))
						throw new ClassNotFoundException(type + " is not a valid ComponentBase subclass.");

					// Dynamically load the class by name
					clazz = Class.forName(
							"vm.computer.components." + type.substring(0, 1).toUpperCase() + type.substring(1));

					// Verify it is a subclass of ComponentBase
					if (!ComponentBase.class.isAssignableFrom(clazz)) {
						throw new ClassCastException(type + " is not a valid ComponentBase subclass.");
					}
				} catch (ClassNotFoundException | ClassCastException e) {
					// Fallback to UnknownComponent if type is invalid or not found
					clazz = UnknownComponent.class;
					System.out.println("Falling back to UnknownComponent for type: " + type);
				}

				try {
					// Ensure the machine's component map is initialized
					machine.listComponents.putIfAbsent(type, new HashMap<>());

					// Retrieve the constructor and create an instance
					Constructor<?> constructor = clazz.getConstructor(Machine.class, String.class, JSONObject.class);
					ComponentBase instance = (ComponentBase) constructor.newInstance(machine, address, component);

					// Add the component to the machine's list
					machine.listComponents.get(type).put(address, instance);

					System.out.println("Loaded component: " + type + " at address: " + address);
				} catch (Exception e) {
					System.out.println("Failed to instantiate component: " + type + " at address: " + address);
					e.printStackTrace();
				}
			}

			if (!machine.listComponents.containsKey("computer")) {
				machine.listComponents.put("computer", new HashMap<>());
				address = UUID.randomUUID().toString();
				machine.listComponents.get("computer").put(address, new vm.computer.components.Computer(machine,
						address, new JSONObject().put("type", "computer")));
			}

			if(machine.listComponents.containsKey("screen")){
				machine.listComponents.get("screen").forEach((addr,comp)->{
					Screen screen = (Screen)comp;
					if(screen.keyboard != null){
						machine.listComponents.putIfAbsent("keyboard", new HashMap<>());
						machine.listComponents.get("keyboard").put(screen.keyboard, new Keyboard(machine, screen.keyboard));
					}
				});
			}

			// Configure la mémoire vive
			machine.RAMSlider.setValue(machineConfig.getDouble("totalMemory"));

			// Charge les utilisateurs
			JSONArray configUsers = machineConfig.getJSONArray("users");
			for (int i = 0; i < configUsers.length(); i++) {
				machine.users.add(configUsers.getString(i));
			}

			// Configure la fenêtre principale
			machine.stage.setX(machineConfig.getDouble("x"));
			machine.stage.setY(machineConfig.getDouble("y"));
			machine.stage.setWidth(320);
			machine.stage.setHeight(512);
			machine.stage.setResizable(false);
			machine.stage.setTitle("Computer@" + machine.listComponents.get("computer").keySet().toArray()[0]);
			machine.stage.getIcons().add(new Image(Objects.requireNonNull(Main.class.getResource("resources/images/Computer.png")).toString()));

			// Met à jour les contrôles
			machine.playerTextField.setText(machineConfig.getString("player"));
			//machine.HDDPathTextField.setText(machine.filesystemComponent.realPath);
			//machine.EEPROMPathTextField.setText(machine.eepromComponent.realPath);

			machine.volumeSlider.setValue(machineConfig.getDouble("volume"));
			machine.onVolumeSliderPressed();

			// Gère le clic sur le bouton d'alimentation
			machine.powerButton.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				event.consume();

				// machine.player.play(machine.player.powerButtonClicked);

				if (machine.started)
					machine.shutdown();
				else
					machine.boot();
			});

			// Gère la fermeture de la fenêtre
			stage.setOnCloseRequest(event -> machine.onWindowClosed());

			// Ajoute la machine à la liste des machines
			list.add(machine);

			// Affiche la fenêtre
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void generate() {
		try {
			System.out.println("Génération d'une nouvelle machine...");

			// Charge la configuration par défaut de la machine et crée un JSON basé dessus
			JSONObject machineConfig = new JSONObject(IO.loadResourceAsString("resources/defaults/Machine.json"));

			// Ajoute le nom de l'utilisateur à la configuration
			machineConfig.put("player", System.getProperty("user.name"));

			// Crée le chemin principal de la machine virtuelle
			File machineFile;
			int counter = 0;
			do {
				machineFile = new File(IO.machinesFile, "Machine" + counter++);
			} while (machineFile.exists());

			// Initialise les composants par défaut
			String address, type, filesystemAddress = null;
			JSONObject component;
			JSONArray components = machineConfig.getJSONArray("components");
			for (int i = 0; i < components.length(); i++) {
				component = components.getJSONObject(i);
				type = component.getString("type");

				// Génère une adresse aléatoire
				address = UUID.randomUUID().toString();
				component.put("address", address);

				// Traitement spécifique
				if (type.equals("filesystem")) {
					// Si c'est un système de fichiers temporaire, définit le chemin réel correspondant
					if (component.getBoolean("temporary")) {
						File temporaryFile = new File(machineFile, "Temporary");
						temporaryFile.mkdirs();

						component.put("path", temporaryFile.getPath());
					}
					// Sinon, mémorise son adresse pour l'utiliser ultérieurement dans les données BIOS
					else {
						filesystemAddress = address;

						// Crée l'espace de stockage pour le disque dur
						File HDDFile = new File(machineFile, "HDD");
						HDDFile.mkdirs();

						// Copie les sources de OpenOS
						System.out.println("Copie des sources de OpenOS...");
						IO.unzipResource("resources/defaults/OpenOS.zip", HDDFile);

						component.put("path", HDDFile.getPath());
					}
				}
				// Génère un canal aléatoire pour le composant de tunnel
				else if (type.equals("tunnel")) {
					component.put("channel", UUID.randomUUID().toString());
				}
			}

			// Associe le disque dur à l'EEPROM
			for (int i = 0; i < components.length(); i++) {
				component = components.getJSONObject(i);

				if (component.getString("type").equals("eeprom")) {
					File EEPROMFile = new File(machineFile, "EEPROM.lua");

					// Enregistre l'adresse du système de fichiers dans les données de l'EEPROM
					component.put("data", filesystemAddress);
					component.put("path", EEPROMFile.getPath());

					// Copie le fichier EEPROM.lua depuis les ressources
					IO.copyResourceToFile("resources/defaults/EEPROM.lua", EEPROMFile);
					break;
				}
			}

			// La machine est prête
			Machine.fromJSONObject(machineConfig);

			// Enregistre au cas où
			IO.saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public JSONObject toJSONObject() {
		JSONArray components = new JSONArray();
		this.listComponents.forEach((type, list) -> list.forEach((addr, component) -> {
			JSONObject obj = component.toJSONObject();
			if (obj != null) {
				components.put(obj);
			}
		}));

		JSONArray configUsers = new JSONArray();
		for (String user : users)
			configUsers.put(user);

		return new JSONObject()
				.put("x", stage.getX())
				.put("y", stage.getY())
				.put("width", stage.getWidth())
				.put("height", stage.getHeight())
				.put("components", components)
				.put("totalMemory", RAMSlider.getValue())
				.put("player", playerTextField.getText())
				.put("volume", volumeSlider.getValue())
				.put("users", configUsers);
	}

	private void error(String text) {
		Gpu gpu = (Gpu) getFirstComponent("gpu");
		if (gpu != null) {
			gpu.rawError("Unrecoverable error\n\n" + text);
			gpu.updaterThread.update();
		}else{
			System.out.println("Error :" + text);
		}

		shutdown();
	}

	public void onGenerateButtonPressed() {
		generate();
	}

	private void onWindowClosed() {
		shutdown();

		this.listComponents.forEach((type, list) -> list.forEach((addr, component) -> {
			if (component instanceof Gpu) {
				((Gpu) component).updaterThread.interrupt();
			}
			if (component instanceof ComponentWindowed) {
				((ComponentWindowed) component).closeScreenWindows();
			}
		}));
	}

	public void onVolumeSliderPressed() {
		player.setVolume(volumeSlider.getValue());
	}

	public static class LuaStateFactory {
		private static class Architecture {
			private static final String OS_ARCH = System.getProperty("os.arch");

			private static boolean isOSArchMatch(String archPrefix) {
				return OS_ARCH != null && OS_ARCH.startsWith(archPrefix);
			}

			static boolean IS_OS_ARM = isOSArchMatch("arm"),
					IS_OS_X86 = isOSArchMatch("x86") || isOSArchMatch("i386"),
					IS_OS_X64 = isOSArchMatch("x86_64") || isOSArchMatch("amd64");
		}

		private static void prepareLoad(boolean use53) {
			NativeSupport.getInstance().setLoader(() -> {
				String architecture = "64", extension = "dll";

				if (SystemUtils.IS_OS_FREE_BSD)
					extension = "bsd.so";
				else if (SystemUtils.IS_OS_LINUX)
					extension = "so";
				else if (SystemUtils.IS_OS_MAC)
					extension = "dylib";

				if (Architecture.IS_OS_X64)
					architecture = "64";
				else if (Architecture.IS_OS_X86)
					architecture = "32";
				else if (Architecture.IS_OS_ARM)
					architecture = "32.arm";

				// Le nom final de la bibliothèque
				String libraryPath = "lua" + (use53 ? "53" : "52") + "/native." + architecture + "." + extension;

				// Copie la bibliothèque depuis les ressources si elle n'existe pas encore
				File libraryFile = new File(IO.librariesFile, libraryPath);
				if (!libraryFile.exists()) {
					try {
						System.out.println("Décompression de la bibliothèque : " + libraryPath);

						libraryFile.mkdirs();
						IO.copyResourceToFile("libraries/" + libraryPath, libraryFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				// Charge la bibliothèque
				System.out.println("Chargement de la bibliothèque : " + libraryFile.getPath());
				System.load(libraryFile.getPath());
			});
		}

		public static LuaState load52() {
			prepareLoad(false);

			LuaState lua = new LuaState(4 * 1024 * 1024);

			lua.openLib(LuaState.Library.BASE);
			lua.openLib(LuaState.Library.BIT32);
			lua.openLib(LuaState.Library.COROUTINE);
			lua.openLib(LuaState.Library.DEBUG);
			lua.openLib(LuaState.Library.ERIS);
			lua.openLib(LuaState.Library.MATH);
			lua.openLib(LuaState.Library.STRING);
			lua.openLib(LuaState.Library.TABLE);
			lua.openLib(LuaState.Library.OS);
			lua.pop(9);

			return lua;
		}

		public static LuaState load53() {
			prepareLoad(true);

			LuaState lua = new LuaStateFiveThree(4 * 1024 * 1024);

			lua.openLibs();
			lua.openLib(LuaState.Library.BASE);
			lua.openLib(LuaState.Library.COROUTINE);
			lua.openLib(LuaState.Library.DEBUG);
			lua.openLib(LuaState.Library.ERIS);
			lua.openLib(LuaState.Library.MATH);
			lua.openLib(LuaState.Library.STRING);
			lua.openLib(LuaState.Library.TABLE);
			lua.openLib(LuaState.Library.UTF8);
			lua.openLib(LuaState.Library.OS);
			lua.pop(9);

			return lua;
		}
	}

	public class LuaThread extends Thread {
		public boolean shuttingDown = false;

		private final LuaState[] signalStack = new LuaState[256];

		public Debugger debug = null;

		@Override
		public void run() {
			// Initialise la machine Lua correctement
			lua = LuaStateFactory.load52();

			// Ajoute un journal pour faciliter le débogage
			lua.pushJavaFunction(args -> {
				String separator = "   ";
				StringBuilder result = new StringBuilder();

				for (int i = 1; i <= args.getTop(); i++) {
					switch (args.type(i)) {
						case NIL:
							result.append("nil");
							result.append(separator);
							break;
						case BOOLEAN:
							result.append(args.toBoolean(i));
							result.append(separator);
							break;
						case NUMBER:
							result.append(args.toNumber(i));
							result.append(separator);
							break;
						case STRING:
							result.append(args.toString(i));
							result.append(separator);
							break;
						case TABLE:
							result.append("table");
							result.append(separator);
							break;
						case FUNCTION:
							result.append("function");
							result.append(separator);
							break;
						case THREAD:
							result.append("thread");
							result.append(separator);
							break;
						case LIGHTUSERDATA:
						case USERDATA:
							result.append("userdata");
							result.append(separator);
							break;
					}
				}
				System.out.println(result);

				return 0;
			});
			lua.setGlobal("LOG");

			// Ajoute tous les API
			for (APIBase api : APIList) {
				api.pushTable();
			}

			// Ajoute tous les composants
			listComponents.forEach((type, list) -> list.forEach((addr, component) -> {
				component.pushProxy();
			}));

			Platform.runLater(() -> {

			});

			try {
				// Charge le code de la machine Lua
				lua.setTotalMemory((int) (RAMSlider.getValue() * 1024 * 1024));
				lua.load(IO.loadResourceAsString("resources/Machine.lua"), "=machine");
				debug = new Debugger(lua);
				lua.call(0, 0);

				error("ordinateur arrêté");
			} catch (Exception | Error e) {
				if (shuttingDown) {
					System.out.println("Arrêt normal");
					// Efface l'écran uniquement après la fin du processus Lua
					// Sinon, il peut y avoir un cas où l'écran est déjà effacé pour l'arrêt,
					// mais le processus Lua continue de dessiner sur l'écran
					if(listComponents.containsKey("gpu")){
						listComponents.get("gpu").forEach((addr,comp)->{
							Gpu gpu = (Gpu)comp;
							gpu.flush();
							gpu.updaterThread.update();
						});
					}
					
				} else {
					error(e.getMessage());
				}
			}
		}

		// Annule la répétition multiple des touches

		public void pushSignal(LuaState signal) {
			int nullIndex = -1;

			for (int i = 0; i < signalStack.length; i++) {
				if (signalStack[i] == null) {
					nullIndex = i;
					break;
				}
			}

			if (nullIndex >= 0)
				signalStack[nullIndex] = signal;

			synchronized (this) {
				notify();
			}
		}

		public LuaState pullSignal(double timeout) {
			synchronized (this) {
				long deadline = timeout == Double.POSITIVE_INFINITY ? Long.MAX_VALUE
						: System.currentTimeMillis() + (long) (timeout * 1000),
						howMuchToWait;

				// System.out.println("Pulling signal infinite: " + (timeout ==
				// Double.POSITIVE_INFINITY) + ", timeout:" + timeout + ", deadline: " +
				// deadline + ", delta: " + (deadline - System.currentTimeMillis()));

				while (System.currentTimeMillis() <= deadline) {
					if (shuttingDown) {
						lua.setTotalMemory(1);
						break;
					} else {
						if (signalStack[0] != null) {
							LuaState result = signalStack[0];

							boolean needClearEnd = signalStack[signalStack.length - 1] != null;

							for (int i = 1; i < signalStack.length; i++)
								signalStack[i - 1] = signalStack[i];

							if (needClearEnd)
								signalStack[signalStack.length - 1] = null;
							//System.out.println("event :" +result.toString(1));
							return result;
						}

						try {
							// Ждем cкока нужна)00
							howMuchToWait = deadline - System.currentTimeMillis();
							if (howMuchToWait > 0)
								wait(howMuchToWait);
						} catch (ThreadDeath | InterruptedException e) {
							System.out.println("Le thread a été interrompu chez l'ordinateur.");
						}
					}
				}

				return new LuaState();
			}
		}
	}

	public void shutdown() {
		if (started) {
			started = false;

			player.stop(player.computerRunning);
			powerButton.setSelected(false);
			propertiesVBox.setDisable(false);

			luaThread.shuttingDown = true;
			luaThread.interrupt();
		}
	}

	public void boot() {
		if (!started) {
			started = true;
			startTime = System.currentTimeMillis();
			Eeprom eepromComponent = (Eeprom) getFirstComponent("eeprom");
			if (eepromComponent == null) {
				error("please insert an eeprom");
			} else {
				File EEPROMFile = new File(eepromComponent.realPath);
				if (EEPROMFile.exists()) {
					try {
						System.out.println("Loading EEPROM from " + eepromComponent.realPath);
						eepromComponent.code = IO.loadFileAsByteArray(EEPROMFile.toURI());

						// Il faut nettoyer l'écran, au cas où un écran bleu de la mort se serait
						// glissé.
						Gpu gpu = (Gpu) getFirstComponent("gpu");
						if(gpu != null){
							gpu.flush();
							gpu.updaterThread.update();
						}

						propertiesVBox.setDisable(true);
						powerButton.setSelected(true);
						player.play(player.computerRunning);

						luaThread = new LuaThread();
						luaThread.start();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					error("EEPROM.lua file not exists");
				}
			}
		}
	}

	private interface OnFileChosen {
		void run(File file);
	}

	public void chooseFile(String title, OnFileChosen onFileChosen) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		fileChooser.setInitialDirectory(IO.machinesFile);

		File file = fileChooser.showOpenDialog(stage);
		if (file != null) {
			onFileChosen.run(file);
		}
	}

	public void chooseDirectory(String title, OnFileChosen onFileChosen) {
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setTitle(title);
		fileChooser.setInitialDirectory(IO.machinesFile);

		File file = fileChooser.showDialog(stage);
		if (file != null) {
			onFileChosen.run(file);
		}
	}

	public void onEEPROMChooseClicked() {
		if(this.listComponents.containsKey("eeprom")) chooseFile("Choose EEPROM.lua file", (file) -> {
			
			EEPROMPathTextField.setText(file.getPath());
			//Eeprom eeprom = (Eeprom) getFirstComponent("eeprom");
			//eepromComponent.realPath = file.getPath();
		});
	}

	public void onHDDChooseClicked() {
		chooseDirectory("Choose HDD directory", (file) -> {
			HDDPathTextField.setText(file.getPath());
			//filesystemComponent.realPath = file.getPath();
		});
	}
}
