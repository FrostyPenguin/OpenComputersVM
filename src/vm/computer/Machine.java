package vm.computer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaStateFiveThree;
import li.cil.repack.com.naef.jnlua.NativeSupport;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import vm.IO;
import vm.computer.api.APIBase;
import vm.computer.api.Component;
import vm.computer.api.Computer;
import vm.computer.api.Unicode;
import vm.computer.components.*;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Machine {
	public static final ArrayList<Machine> list = new ArrayList<>();
	private static final int screenImageViewBlurSize = 82;
	
	// Жабафыховские обжекты
	public GridPane mainGridPane, screenGridPane;
	public AnchorPane sceneAnchorPane;
	public VBox propertiesVBox;
	public Slider RAMSlider, volumeSlider;
	public ImageView screenImageView, boardImageView;
	public ToggleButton powerButton;
	public TextField EEPROMPathTextField, HDDPathTextField, tunnelChannelTextField, screensHorizontallyTextField, screensVerticallyTextField, playerTextField;
	public Button toolbarButton, closeMachineButton;
	
	// Машиновская поебистика
	public ArrayList<ComponentBase> componentList = new ArrayList<>();
	public ArrayList<APIBase> APIList = new ArrayList<>();
	
	public boolean started = false;
	public long startTime;
	public LuaState lua;
	public LuaThread luaThread;
	public Component componentAPI;
	public Computer computerAPI;
	public Unicode unicodeAPI;
//    public OS osAPI;
	public GPU gpuComponent;
	public EEPROM eepromComponent;
	public Keyboard keyboardComponent;
	public Screen screenComponent;
	public vm.computer.components.Computer computerComponent;
	public Filesystem temporaryFilesystemComponent, filesystemComponent;
	public Modem modemComponent;
	public Tunnel tunnelComponent;
    public Internet internetComponent;
    public Player player = new Player();
    
	private Stage stage;
	private boolean toolbarHidden;
	
	// Пустой конструктор требуется FXML-ебале для инициализации
	public Machine() {
		
	}

	public static void fromJSONObject(JSONObject machineConfig) {
		try {
			// Ну че, создаем окошко, грузим фхмл-файл и ставим сцену окошку
			Stage stage = new Stage();
			FXMLLoader fxmlLoader = new FXMLLoader(Machine.class.getResource("Window.fxml"));
			stage.setScene(new Scene(fxmlLoader.load()));
			
			// Выдрачиваем машинку из фхмл-контроллера и запоминаем эту стейдж-залупу
			Machine machine = fxmlLoader.getController();
			machine.stage = stage;

			// Создаем АПИхи
			machine.computerAPI = new Computer(machine);
			machine.componentAPI = new Component(machine);
			machine.unicodeAPI = new Unicode(machine);
			
			// Инициализируем компоненты из конфига МОШЫНЫ
			JSONArray components = machineConfig.getJSONArray("components");
			JSONObject component;
			String address;
			for (int i = 0; i < components.length(); i++) {
				component = components.getJSONObject(i);
				address = component.getString("address");

				switch (component.getString("type")) {
					case "gpu":
						machine.gpuComponent = new GPU(machine, address);
						machine.gpuComponent.rawSetResolution(component.getInt("width"), component.getInt("height"));
						break;
					case "screen":
						machine.screenComponent = new Screen(machine, address, component.getBoolean("precise"), component.getInt("blocksHorizontally"), component.getInt("blocksVertically"));
						break;
					case "keyboard":
						machine.keyboardComponent = new Keyboard(machine, address);
						break;
					case "computer":
						machine.computerComponent = new vm.computer.components.Computer(machine, address);
						break;
					case "eeprom":
						machine.eepromComponent = new EEPROM(machine, address, component.getString("label"), component.getString("path"), component.getString("data"));
						break;
					case "filesystem":
						if (component.getBoolean("temporary"))
							machine.temporaryFilesystemComponent = new Filesystem(machine, address, component.getString("label"), component.getString("path"), true);
						else
							machine.filesystemComponent = new Filesystem(machine, address, component.getString("label"), component.getString("path"), false);
						break;
					case "modem":
						machine.modemComponent = new Modem(machine, address, component.getString("wakeMessage"), component.getBoolean("wakeMessageFuzzy"));
						break;
					case "tunnel":
						machine.tunnelComponent = new Tunnel(machine, address, component.getString("channel"), component.getString("wakeMessage"), component.getBoolean("wakeMessageFuzzy"));
						break;
					case "internet":
                        machine.internetComponent = new Internet(machine, address);
						break;
				}
			}

			// Вгондошиваем значение лимита оперативы
			machine.RAMSlider.setValue(machineConfig.getDouble("totalMemory"));
			
			// Пидорасим главное йоба-окошечко так, как надо
			machine.stage.setX(machineConfig.getDouble("x"));
			machine.stage.setY(machineConfig.getDouble("y"));
			machine.stage.setWidth(machineConfig.getDouble("width"));
			machine.stage.setHeight(machineConfig.getDouble("height"));

			// Апдейтим контролсы
			machine.playerTextField.setText(machineConfig.getString("player"));
			machine.HDDPathTextField.setText(machine.filesystemComponent.realPath);
			machine.EEPROMPathTextField.setText(machine.eepromComponent.realPath);
			machine.tunnelChannelTextField.setText(machine.tunnelComponent.channel);
			machine.screensHorizontallyTextField.setText(String.valueOf(machine.screenComponent.blocksHorizontally));
			machine.screensVerticallyTextField.setText(String.valueOf(machine.screenComponent.blocksVertically));
			
			machine.volumeSlider.setValue(machineConfig.getDouble("volume"));
			machine.onVolumeSliderPressed();
			
			machine.toolbarHidden = machineConfig.getBoolean("toolbarHidden");
			machine.updateToolbar();
			
			// Шоб не вводили хуйнину всякую
			machine.screensHorizontallyTextField.setTextFormatter(new TextFormatter<>(change -> {
				if (change.getControlNewText().matches("\\d*")) {
					String
						w = machine.screensHorizontallyTextField.getText(),
						h = machine.screensVerticallyTextField.getText();
					
					if (w.length() > 0 && h.length() > 0) {
						machine.screenComponent.blocksHorizontally = Integer.parseInt(w);
						machine.screenComponent.blocksVertically = Integer.parseInt(h);
					}
					
					return change;
				}
				else {
					return null;
				}
			}));
			machine.screensHorizontallyTextField.setTextFormatter(machine.screensHorizontallyTextField.getTextFormatter());
			
			// А это более грамотная обработка клика на толлгеКнопку ебливую, которая вообще неадекватно реагирует по дефолту
			machine.powerButton.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				event.consume();

				machine.player.play(machine.player.powerButtonClicked);
				
				if (machine.started)
					machine.shutdown();
				else
					machine.boot();
			});
			
			// Приходится ебошить тень вручную, т.к. иначе оно оперирует баундсами, которые поганят скрин-ивенты
			DropShadow effect = new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(0, 0, 0, 0.5), 0, 0, 0, 0);
			effect.setWidth(screenImageViewBlurSize + 2);
			effect.setHeight(screenImageViewBlurSize + 2);
			machine.screenImageView.setEffect(effect);

			// При закрытии окошка машину над оффнуть, а то хуй проссыт, будет ли там поток дрочиться или плеер этот асинхронники свои сувать меж булок
			stage.setOnCloseRequest(event -> machine.onWindowClosed());
			
			// Авторесайз пикчи, чтоб охуенно и пиздато все было
			machine.screenGridPane.widthProperty().addListener((observable, oldValue, newValue) -> machine.checkImageViewBingings());
			machine.screenGridPane.heightProperty().addListener((observable, oldValue, newValue) -> machine.checkImageViewBingings());
			
			// Запоминаем мошынку в гулаг-перечне мошынок
			list.add(machine);
			
			// Акошычко гатово
			stage.show();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void generate() {
		try {
			System.out.println("Generating new machine...");
			
			// Грузим дефолтный конфиг машины и создаем жсон на его основе
			JSONObject machineConfig = new JSONObject(IO.loadResourceAsString("resources/defaults/Machine.json"));
			
			// Пушим в него имечко юзверя
			machineConfig.put("player", System.getProperty("user.name"));
			
			// Создаем основной путь всей вирт. машины
			File machineFile;
			int counter = 0;
			do {
				machineFile = new File(IO.machinesFile, "Machine" + counter++);
			} while (machineFile.exists());

			// Продрачиваем дефолтные компоненты
			String address, type, filesystemAddress = null;
			JSONObject component;
			JSONArray components = machineConfig.getJSONArray("components");
			for (int i = 0; i < components.length(); i++) {
				component = components.getJSONObject(i);
				type = component.getString("type");
				
				// Генерим рандомный адрес
				address = UUID.randomUUID().toString();
				component.put("address", address);

				// Вот тута стопэ.
				if (type.equals("filesystem")) {
					// Если это временная файлосистема, то въебываем ей соответствующий реальный путь
					if (component.getBoolean("temporary")) {
						File temporaryFile = new File(machineFile, "Temporary");
						temporaryFile.mkdirs();
						
						component.put("path", temporaryFile.getPath());
					}
					// А если это обычный хард, то запоминаем его адрес, чтоб потом его в биос дату вхуячить
					else {
						filesystemAddress = address;
						
						// Создаем хуйню под хард
						File HDDFile = new File(machineFile, "HDD");
						HDDFile.mkdirs();
						
						// Анпачим опенось
						System.out.println("Copying OpenOS sources...");
						IO.unzipResource("resources/defaults/OpenOS.zip", HDDFile);
						
						component.put("path", HDDFile.getPath());
					}
				}
				// И рандомный канал компонента линкед карты
				else if (type.equals("tunnel")) {
					component.put("channel", UUID.randomUUID().toString());
				}
			}

			// А терь ищем еепром и сеттим ему полученный адрес харда
			for (int i = 0; i < components.length(); i++) {
				component = components.getJSONObject(i);

				if (component.getString("type").equals("eeprom")) {
					File EEPROMFile = new File(machineFile, "EEPROM.lua");

					// Сейвим инфу с загрузочным адресом еепрома
					component.put("data", filesystemAddress);
					component.put("path", EEPROMFile.getPath());
					
					// Копипиздим EEPROM.lua с ресурсов
					IO.copyResourceToFile("resources/defaults/EEPROM.lua", EEPROMFile);
					break;
				}
			}

			// Усе, уася, готова машинка
			Machine.fromJSONObject(machineConfig);

			// Сейвим на всякий
			IO.saveConfig();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject toJSONObject() {
		JSONArray components = new JSONArray();
		for (ComponentBase component : componentList)
			components.put(component.toJSONObject());
		
		return new JSONObject()
			.put("x", stage.getX())
			.put("y", stage.getY())
			.put("width", stage.getWidth())
			.put("height", stage.getHeight())
			.put("toolbarHidden", toolbarHidden)
			.put("components", components)
			.put("totalMemory", RAMSlider.getValue())
			.put("player", playerTextField.getText())
			.put("volume", volumeSlider.getValue());
	}

	public String getClipboard() {
		try {
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (HeadlessException | IOException | UnsupportedFlavorException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	public void checkImageViewBingings() {
		double
			width = screenGridPane.getWidth(),
			height = screenGridPane.getHeight();
		screenImageView.setFitWidth(width > gpuComponent.GlyphWIDTHMulWidth ? gpuComponent.GlyphWIDTHMulWidth : width);
		screenImageView.setFitHeight(height > gpuComponent.GlyphHEIGHTMulHeight ? gpuComponent.GlyphHEIGHTMulHeight : height);
	}
	
	private void updateToolbar() {
		ColumnConstraints columnConstraints = mainGridPane.getColumnConstraints().get(1);
		
		new Timeline(
			new KeyFrame(Duration.ZERO,
				new KeyValue(columnConstraints.prefWidthProperty(), columnConstraints.getPrefWidth()),
				new KeyValue(toolbarButton.textProperty(), toolbarButton.getText())
			),
			new KeyFrame(new Duration(100),
				new KeyValue(columnConstraints.prefWidthProperty(), toolbarHidden ? 0 : boardImageView.getFitWidth()),
				new KeyValue(toolbarButton.textProperty(), toolbarHidden ? "<" : ">")
			)
		).play();
	}

	private void error(String text) {
		gpuComponent.rawError("Unrecoverable error\n\n" + text);
		gpuComponent.updaterThread.update();

		shutdown();
	}
	
	public void onToolbarButtonPressed() {
		toolbarHidden = !toolbarHidden;
		updateToolbar();
		screenImageView.requestFocus();
	}
	
	public void onGenerateButtonPressed() {
		generate();
	}
	
	private void onWindowClosed() {
		shutdown();
		gpuComponent.updaterThread.interrupt();
	}

	public void onCloseMachineButtonPressed() {
		onWindowClosed();
		list.remove(this);
		stage.close();
	}
	
	public void onVolumeSliderPressed() {
		player.setVolume(volumeSlider.getValue());
	}

	public static class LuaStateFactory {
		private static class Architecture {
			private static String OS_ARCH = System.getProperty("os.arch");
			private static boolean isOSArchMatch(String archPrefix) {
				return OS_ARCH != null && OS_ARCH.startsWith(archPrefix);
			}

			static boolean
				IS_OS_ARM = isOSArchMatch("arm"),
				IS_OS_X86 = isOSArchMatch("x86") || isOSArchMatch("i386"),
				IS_OS_X64 = isOSArchMatch("x86_64") || isOSArchMatch("amd64");
		}

		private static void prepareLoad(boolean use53) {
			NativeSupport.getInstance().setLoader(() -> {
				String architecture = "64", extension = "dll";

				if (SystemUtils.IS_OS_FREE_BSD) extension = "bsd.so";
				else if (SystemUtils.IS_OS_LINUX) extension = "so";
				else if (SystemUtils.IS_OS_MAC) extension = "dylib";
				
				if (Architecture.IS_OS_X64) architecture = "64";
				else if (Architecture.IS_OS_X86) architecture = "32";
				else if (Architecture.IS_OS_ARM) architecture = "32.arm";

				// Финальное, так сказать, название либсы-хуибсы
				String libraryPath = "lua" + (use53 ? "53" : "52") + "/native." + architecture + "." + extension;
				
				// Копипиздим либу из ресурсов, если ее еще нет
				File libraryFile = new File(IO.librariesFile, libraryPath);
				if (!libraryFile.exists()) {
					try {
						System.out.println("Unpacking library: " + libraryPath);
						
						libraryFile.mkdirs();
						IO.copyResourceToFile("libraries/" + libraryPath, libraryFile);
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				// Грузим ее, НАКАНЕЦТА
				System.out.println("Loading library: " + libraryFile.getPath());
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
		
		private LuaState[] signalStack = new LuaState[256];
		private int lastOCPixelClickX, lastOCPixelClickY;

		private HashMap<KeyCode, String> codes = new HashMap<>();
		private KeyCode lastCode;
		
		@Override
		public void run() {
			// Инициализируем корректную Lua-машину
			lua = LuaStateFactory.load52();

			// Добавим логгер, чтоб дебажить потом проще было 
			lua.pushJavaFunction(args -> {
				String separator = "   ";
				StringBuilder result = new StringBuilder();

				for (int i = 1; i <= args.getTop(); i++) {
					switch (args.type(i)) {
						case NIL: result.append("nil"); result.append(separator); break;
						case BOOLEAN: result.append(args.toBoolean(i)); result.append(separator); break;
						case NUMBER: result.append(args.toNumber(i)); result.append(separator); break;
						case STRING: result.append(args.toString(i)); result.append(separator); break;
						case TABLE: result.append("table"); result.append(separator); break;
						case FUNCTION: result.append("function"); result.append(separator); break;
						case THREAD: result.append("thread"); result.append(separator); break;
						case LIGHTUSERDATA: result.append("userdata"); result.append(separator); break;
						case USERDATA: result.append("userdata"); result.append(separator); break;
					}
				}
				System.out.println(result.toString());

				return 0;
			});
			lua.setGlobal("LOG");

			// Пушим все апихи
			for (APIBase api : APIList) {
				api.pushTable();
			}

			// Пушим все компоненты
			for (ComponentBase component : componentList) {
				component.pushProxy();
			}

			Platform.runLater(() -> {
				// Фокусирование экрана при клике на эту злоебучую область
				mainGridPane.setOnMousePressed(event -> screenImageView.requestFocus());

				// Ивенты клавиш всему окну
				mainGridPane.setOnKeyPressed(event -> {
//					System.out.println("PRESSED: " + event.getCharacter() + ", " + event.getText() + ", " + event.getCode());

					lastCode = event.getCode();
					
					// Системная клавиша никогда не приведет к KeyTyped-ивенту
					if (event.getText().length() == 0) {
						pushKeySignal(lastCode, "", "key_down");
						codes.put(lastCode, "");
					}
				});

				// Этот ивент всегда следует сразу за KeyPressed в случае несистемных клавиш
				mainGridPane.setOnKeyTyped(event -> {
					if (!codes.containsKey(lastCode)) {
//						System.out.println("TYPED: " + event.getCharacter() + ", " + event.getText() + ", " + event.getCode());

						String character = event.getCharacter();
						pushKeySignal(lastCode, character, "key_down");
						codes.put(lastCode, character);
					}
				});

				mainGridPane.setOnKeyReleased(event -> {
//					System.out.println("RELEASED: " + event.getCharacter() + ", " + event.getText() + ", " + event.getCode());
					
					KeyCode keyCode = event.getCode();
					if (codes.containsKey(keyCode)) {
						pushKeySignal(keyCode, codes.get(keyCode), "key_up");
						codes.remove(keyCode);
					}
				});

				// А эт уже ивенты тача, драга и прочего конкретно на экранной хуйне этой
				screenImageView.setOnMousePressed(event -> {
					// Сигнал вставки из буфера обмена
					if (event.getButton() == MouseButton.MIDDLE) {
						LuaState luaState = new LuaState();
						
						luaState.pushString("clipboard");
						luaState.pushString(keyboardComponent.address);
						luaState.pushString(getClipboard());
						luaState.pushString(playerTextField.getText());
						
						pushSignal(luaState);
					}
					else
						pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "touch", true);
				});

				screenImageView.setOnMouseDragged(event -> {
					if (event.getButton() != MouseButton.MIDDLE)
						pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "drag", false);
				});

				screenImageView.setOnMouseReleased(event -> {
					if (event.getButton() != MouseButton.MIDDLE)
						pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "drop", true);
				});

				screenImageView.setOnScroll(event -> {
					pushTouchSignal(event.getSceneX(), event.getSceneY(), event.getDeltaY() > 0 ? 1 : -1, "scroll", true);
				});
			});
			
			try {
				// Грузим машин-кодыч
				lua.setTotalMemory((int) (RAMSlider.getValue() * 1024 * 1024));
				lua.load(IO.loadResourceAsString("resources/Machine.lua"), "=machine");
				lua.call(0, 0);

				error("computer halted");
			}
			catch (Exception e) {
				if (shuttingDown) {
					System.out.println("Shutting down normally");
					// Чистим экран исключительно по завершению процесса луа
					// Иначе может возникнуть случай, когда экран уже очищен для выключения,
					// а процесс луа еще дорабатывает остатки и рисует хуйню на экране
					gpuComponent.flush();
					gpuComponent.updaterThread.update();
				}
				else {
					error(e.getMessage());
				}
			}
		}
		
		private void pushKeySignal(KeyCode keyCode, String text, String name) {
			KeyMap.OCKey ocKey = KeyMap.get(keyCode);

			LuaState luaState = new LuaState();
			luaState.pushString(name);
			luaState.pushString(keyboardComponent.address);
			luaState.pushInteger(text.length() > 0 ? text.codePointAt(0) : ocKey.unicode);
			luaState.pushInteger(ocKey.ascii);
			luaState.pushString(playerTextField.getText());
			
			pushSignal(luaState);
		}

		private int getOCButton(MouseEvent event) {
			switch (event.getButton()) {
				case SECONDARY: return 1;
				default: return 0;
			}
		}

		private void pushTouchSignal(double sceneX, double sceneY, int state, String name, boolean notDrag) {
			Bounds bounds = screenImageView.getBoundsInLocal();
			double
				p1 = (bounds.getWidth() - screenImageViewBlurSize) / gpuComponent.GlyphWIDTHMulWidth,
				p2 = (bounds.getHeight() - screenImageViewBlurSize) / gpuComponent.GlyphHEIGHTMulHeight;

//			System.out.println(bounds.getWidth() + ", " + bounds.getHeight() + ", " + screenImageView.getFitWidth() + ", " + screenImageView.getFitHeight());
			
			double
				x = (sceneX - screenImageView.getLayoutX()) / p1 / Glyph.WIDTH + 1,
				y = (sceneY - screenImageView.getLayoutY()) / p2 / Glyph.HEIGHT + 1;
			
			int OCPixelClickX = (int) x;
			int OCPixelClickY = (int) y;
			
//			System.out.println("Pushing touch signal: " + x + ", " + y);
			if (notDrag || OCPixelClickX != lastOCPixelClickX || OCPixelClickY != lastOCPixelClickY) {
				
				LuaState luaState = new LuaState();
				luaState.pushString(name);
				luaState.pushString(screenComponent.address);
				if (screenComponent.precise) {
					luaState.pushNumber(x);
					luaState.pushNumber(y);
				}
				else {
					luaState.pushInteger(OCPixelClickX);
					luaState.pushInteger(OCPixelClickY);
				}
				luaState.pushInteger(state);
				luaState.pushString(playerTextField.getText());
				
				pushSignal(luaState);
			}

			lastOCPixelClickX = OCPixelClickX;
			lastOCPixelClickY = OCPixelClickY;
		}

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
				long deadline = timeout == Double.POSITIVE_INFINITY ? Long.MAX_VALUE : System.currentTimeMillis() + (long) (Math.max(0, timeout) * 1000);

//                System.out.println("Pulling signal infinite: " + (timeout == Double.POSITIVE_INFINITY) + ", timeout:" + timeout + ", deadline: " + deadline + ", delta: " + (deadline - System.currentTimeMillis()));
				
				while (System.currentTimeMillis() <= deadline) {
					if (signalStack[0] != null) {
						LuaState result = signalStack[0];

						// Шифтим
						boolean needClearEnd = signalStack[signalStack.length - 1] != null;
							
						for (int i = 1; i < signalStack.length; i++)
							signalStack[i - 1] = signalStack[i];

						if (needClearEnd)
							signalStack[signalStack.length - 1] = null;

						return result;
					}

					try {
						// Ждем на 1 мскек больше, т.к. wait(0) ждет бисканечна))00
						wait(deadline - System.currentTimeMillis() + 1);
					}
					catch (ThreadDeath | InterruptedException e) {
						System.out.println("Поток интерруптнулся чет у компа");
						lua.setTotalMemory(1);
						
						break;
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
			
			File EEPROMFile = new File(eepromComponent.realPath);
			if (EEPROMFile.exists()) {
				try {
					System.out.println("Loading EEPROM from " + eepromComponent.realPath);
					eepromComponent.code = IO.loadFileAsByteArray(EEPROMFile.toURI());
					
					// Экранчик надо чистить, а то вдруг там бсод закрался
					gpuComponent.flush();
					gpuComponent.updaterThread.update();

					propertiesVBox.setDisable(true);
					powerButton.setSelected(true);
					screenImageView.requestFocus();
					player.play(player.computerRunning);

					luaThread = new LuaThread();
					luaThread.start();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				error("EEPROM.lua file not exists");
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
		chooseFile("Choose EEPROM.lua file", (file) -> {
			EEPROMPathTextField.setText(file.getPath());
			eepromComponent.realPath = file.getPath();
		});
	}

	public void onHDDChooseClicked() {
		chooseDirectory("Choose HDD directory", (file) -> {
			HDDPathTextField.setText(file.getPath());
			filesystemComponent.realPath = file.getPath();
		});
	}
}
