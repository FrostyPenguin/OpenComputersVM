package vm.computer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Machine {
	public static final ArrayList<Machine> list = new ArrayList<>();
	private static final int screenImageViewBlurSize = 82;
	
	// Жабафыховские обжекты
	public Slider RAMSlider;
	public GridPane windowGridPane;
	public GridPane screenGridPane;
	public ImageView screenImageView, boardImageView;
	public ToggleButton powerButton;
	public TextField EEPROMPathTextField, HDDPathTextField, tunnelChannelTextField, screensHorizontallyTextField, screensVerticallyTextField, playerTextField;
	public Button toolbarButton;
	
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
	public Filesystem filesystemComponent, temporaryFilesystemComponent;
	public Modem modemComponent;
	public Tunnel tunnelComponent;
//    public Internet internetComponent;

	private Stage stage;
	private Player computerRunningPlayer;
	private boolean toolbarHidden;
	
	// Пустой конструктор требуется FXML-ебале для инициализации
	public Machine() {
		computerRunningPlayer = new Player("computer_running.mp3");
		computerRunningPlayer.setRepeating();
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
						machine.gpuComponent.updaterThread.update();
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
						machine.eepromComponent = new EEPROM(machine, address, component.getString("path"), component.getString("data"));
						break;
					case "filesystem":
						boolean temporary = component.getBoolean("temporary");
						Filesystem filesystem = new Filesystem(machine, address, component.getString("label"), component.getString("path"), temporary);

						if (temporary) {
							machine.temporaryFilesystemComponent = filesystem;
						}
						else {
							machine.filesystemComponent = filesystem;
						}
						break;
					case "modem":
						machine.modemComponent = new Modem(machine, address, component.getString("wakeMessage"), component.getBoolean("wakeMessageFuzzy"));
						break;
					case "tunnel":
						machine.tunnelComponent = new Tunnel(machine, address, component.getString("channel"), component.getString("wakeMessage"), component.getBoolean("wakeMessageFuzzy"));
					case "internet":
//                        machine.internetComponent = new Internet(machine, address);
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
			
			machine.toolbarHidden = machineConfig.getBoolean("toolbarHidden");
			machine.updateToolbar();

			DropShadow effect = new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(0, 0, 0, 0.5), 0, 0, 0, 0);
			effect.setWidth(screenImageViewBlurSize + 2);
			effect.setHeight(screenImageViewBlurSize + 2);
			machine.screenImageView.setEffect(effect);

			// При закрытии окошка машину над оффнуть, а то хуй проссыт, будет ли там поток дрочиться или плеер этот асинхронники свои сувать меж булок
			stage.setOnCloseRequest(event -> {
				machine.shutdown(true);
			});
	
			// Авторесайз пикчи, чтоб охуенно и пиздато все было
			machine.screenGridPane.widthProperty().addListener((observable, oldValue, newValue) -> {
				double cyka = newValue.doubleValue();
				machine.screenImageView.setFitWidth(cyka > machine.gpuComponent.GlyphWIDTHMulWidth ? machine.gpuComponent.GlyphWIDTHMulWidth : cyka);
			});

			machine.screenGridPane.heightProperty().addListener((observable, oldValue, newValue) -> {
				double cyka = newValue.doubleValue();
				machine.screenImageView.setFitHeight(cyka > machine.gpuComponent.GlyphHEIGHTMulHeight ? machine.gpuComponent.GlyphHEIGHTMulHeight : cyka);
			});
			
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
			
			// Продрачиваем дефолтные компоненты
			String address, type, filesystemAddress = null;
			JSONObject component;
			File machineFile = null;
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
						File temporaryFile = new File(IO.temporaryFile, address);
						temporaryFile.mkdirs();
						component.put("path", temporaryFile.getPath());
					}
					// А если это обычный хард, то запоминаем его адрес, чтоб потом его в биос дату вхуячить
					else {
						filesystemAddress = address;
						
						// Заодно создаем основной путь всей вирт. машины
						machineFile = new File(IO.machinesFile, address);
						
						File HDDFile = new File(machineFile, "HDD/");
						HDDFile.mkdirs();
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
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject toJSONObject() {
		JSONArray components = new JSONArray();
		for (int j = 0; j < componentList.size(); j++) {
			components.put(componentList.get(j).toJSONObject());
		}
		
		return new JSONObject()
			.put("x", stage.getX())
			.put("y", stage.getY())
			.put("width", stage.getWidth())
			.put("height", stage.getHeight())
			.put("toolbarHidden", toolbarHidden)
			.put("components", components)
			.put("totalMemory", RAMSlider.getValue())
			.put("player", playerTextField.getText());
	}
	
	private void updateToolbar() {
		ColumnConstraints columnConstraints = windowGridPane.getColumnConstraints().get(1);
		
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
	
	public void onToolbarButtonPressed() {
		toolbarHidden = !toolbarHidden;
		updateToolbar();
		screenImageView.requestFocus();
	}
	
	public void onGenerateButtonTouch() {
		generate();
	}

	public void onPowerButtonTouch() {
		new Player("click.mp3").play();
		if (started) {
			shutdown(true);
		} else {
			boot();
		}
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
		private ArrayList<LuaState> signalStack = new ArrayList<>();
		private HashMap<KeyCode, Boolean> pressedKeyCodes = new HashMap<>();
		private int lastOCPixelClickX, lastOCPixelClickY;

		// Интересное решение: данный костыль работает "костыльнее", однако быстрее аналога на machine.lua
		private boolean shuttingDown = false;

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
				windowGridPane.setOnMousePressed(event -> {
					screenImageView.requestFocus();
				});

				// Ивенты клавиш всему окну
				windowGridPane.setOnKeyPressed(event -> {
					KeyCode keyCode = event.getCode();
					// Иначе оно спамит даунами
					if (!isKeyPressed(keyCode)) {
						pressedKeyCodes.put(keyCode, true);
						pushKeySignal(keyCode, event.getText(), "key_down");
					}
				});

				windowGridPane.setOnKeyReleased(event -> {
					KeyCode keyCode = event.getCode();
					pressedKeyCodes.put(keyCode, false);
					pushKeySignal(keyCode, event.getText(), "key_up");
				});

				// А эт уже ивенты тача, драга и прочего конкретно на экранной хуйне этой
				screenImageView.setOnMousePressed(event -> {
					pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "touch", true);
				});

				screenImageView.setOnMouseDragged(event -> {
					pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "drag", false);
				});

				screenImageView.setOnMouseReleased(event -> {
					pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "drop", true);
				});

				screenImageView.setOnScroll(event -> {
					pushTouchSignal(event.getSceneX(), event.getSceneY(), event.getDeltaY() > 0 ? 1 : -1, "scroll", true);
				});
			});
			
			try {
				// Грузим машин-кодыч
				lua.setTotalMemory((int) (RAMSlider.getValue() * 1024));
				lua.load(IO.loadResourceAsString("resources/Machine.lua"), "=machine");
				lua.newThread();
				lua.resume(1, 0);

				if (shuttingDown) {
					System.out.println("Успешно вырубаем компек))0");
				}
				else {
					error("computer halted");
				}
			}
			catch (Exception e) {
				error(e.getMessage());
			}
		}

		private void error(String text) {
			gpuComponent.rawError("Unrecoverable error\n\n" + text);
			gpuComponent.updaterThread.update();
			
			powerButton.setSelected(false);
			shutdown(false);
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
			signalStack.add(signal);

			synchronized (this) {
				notify();
			}
		}

		public LuaState pullSignal(double timeout) {
			synchronized (this) {
				long deadline = timeout == Double.POSITIVE_INFINITY ? Long.MAX_VALUE : System.currentTimeMillis() + (long) (timeout * 1000);

//                System.out.println("Pulling signal infinite: " + (timeout == Double.POSITIVE_INFINITY) + ", timeout:" + timeout + ", deadline: " + deadline + ", delta: " + (deadline - System.currentTimeMillis()));
				
				while (System.currentTimeMillis() <= deadline) {
					if (signalStack.size() > 0) {
						LuaState result = signalStack.get(0);

						// Шифтим
						signalStack.remove(0);

						return result;
					}

					try {
						// Ждем на 1 мскек больше, т.к. wait(0) ждет бисканечна))00
						wait(deadline - System.currentTimeMillis() + 1);
					}
					catch (ThreadDeath | InterruptedException e) {
						System.out.println("Поток интерруптнулся чет у компа");
						
						lua.yield(0);
						shuttingDown = true;
					}
				}
				
				return new LuaState();
			}
		}

		public boolean isKeyPressed(KeyCode keyCode) {
			return pressedKeyCodes.getOrDefault(keyCode, false);
		}
	}
	
	public void shutdown(boolean resetGPU) {
		if (started) {
			started = false;
			
			computerRunningPlayer.stop();
			
			RAMSlider.setDisable(false);
			
			if (resetGPU) {
				gpuComponent.flush();
				gpuComponent.updaterThread.update();
			}
			
			luaThread.interrupt();
		}
	}

	public void boot() {
		if (!started) {
			try {
				// Грузим биос-хуйню из файла
				eepromComponent.loadCode();

				// ПОДРУБАЛИТИ
				started = true;
				startTime = System.currentTimeMillis();

				// Экранчик надо чистить, а то вдруг там бсод закрался
				gpuComponent.flush();
				gpuComponent.updaterThread.update();
				
				// Оффаем слайдер памяти, а то хуйня эта сангаровская ругается
				RAMSlider.setDisable(true);
				screenImageView.requestFocus();

				// Играем звук компека)00
				computerRunningPlayer.play();

				// Запускаем луа-машину
				luaThread = new LuaThread();
				luaThread.start();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
