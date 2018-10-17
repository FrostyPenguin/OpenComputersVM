package vm.computer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaStateFiveThree;
import li.cil.repack.com.naef.jnlua.NativeSupport;
import org.apache.commons.lang3.SystemUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import vm.IO;
import vm.computer.api.Component;
import vm.computer.api.Computer;
import vm.computer.api.Unicode;
import vm.computer.components.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class Machine {
    public static final ArrayList<Machine> list = new ArrayList<>();
    
    // Жабафыховские обжекты
    public GridPane windowGridPane;
    public GridPane screenGridPane;
    public ImageView screenImageView, boardImageView;
    public ToggleButton powerButton;
    public TextField EEPROMPathTextField, HDDPathTextField, nameTextField;
    public Button toolbarButton;
    
    // Машиновская поебистика
    public boolean started = false;
    public long startTime;
    public LuaThread luaThread;
    public Component componentAPI;
    public Computer computerAPI;
    public Unicode unicodeAPI;
    public GPU gpuComponent;
    public EEPROM eepromComponent;
    public Keyboard keyboardComponent;
    public Screen screenComponent;
    public vm.computer.components.Computer computerComponent;
    public Filesystem filesystemComponent;
    public Modem modemComponent;
//    public Internet internetComponent;

    private Stage stage;
    private Player computerRunningPlayer;
    private boolean toolbarHidden;
    private LuaState lua;
    
    public static void fromJSONObject(JSONObject machineConfig) {
        try {
            // Ну че, создаем окошко, грузим фхмл-файл и ставим сцену окошку
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(Machine.class.getResource("Window.fxml"));
            stage.setScene(new Scene(fxmlLoader.load()));
            
            // Выдрачиваем машинку из фхмл-контроллера и запоминаем эту стейдж-залупу
            Machine machine = fxmlLoader.getController();
            machine.stage = stage;

            // Инициализируем корректную Lua-машину
            machine.lua = LuaStateFactory.load52(4 * 1024 * 1024);

            // По дефолту принт будет выводить хуйню в консоль
            machine.lua.pushJavaFunction(args -> {
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
            machine.lua.setGlobal("print");

            // Компудахтерное апи
            machine.lua.newTable();
            machine.computerAPI = new Computer(machine.lua, machine);
            machine.lua.setGlobal("computer");

            // Компонентное апи
            machine.lua.newTable();
            machine.componentAPI = new Component(machine.lua);
            machine.lua.setGlobal("component");

            // Юникодное апи
            machine.lua.newTable();
            machine.unicodeAPI = new Unicode(machine.lua);
            machine.lua.setGlobal("unicode");
            
            // Инициализируем компоненты из конфига МОШЫНЫ
            JSONArray components = machineConfig.getJSONArray("components");
            JSONObject component;
            String address;
            
            for (int i = 0; i < components.length(); i++) {
                component = components.getJSONObject(i);
                address = component.getString("address");

                switch (component.getString("type")) {
                    case "gpu":
                        machine.gpuComponent = new GPU(machine.lua, address, machine.screenImageView);
                        machine.gpuComponent.rawSetResolution(component.getInt("width"), component.getInt("height"));
                        machine.gpuComponent.update();
                        break;
                    case "screen":
                        machine.screenComponent = new Screen(machine.lua, address, component.getBoolean("precise"));
                        break;
                    case "keyboard":
                        machine.keyboardComponent = new Keyboard(machine.lua, address);
                        break;
                    case "computer":
                        machine.computerComponent = new vm.computer.components.Computer(machine.lua, address, machine);
                        break;
                    case "eeprom":
                        machine.eepromComponent = new EEPROM(machine.lua, address, component.getString("path"), component.getString("data"));
                        break;
                    case "filesystem":
                        machine.filesystemComponent = new Filesystem(machine.lua, address, component.getString("path"));
                        break;
                    case "modem":
                        machine.modemComponent = new Modem(machine.lua, address, component.getString("wakeMessage"), component.getBoolean("wakeMessageFuzzy"));
                        break;
                    case "internet":
//                        machine.internetComponent = new Internet(address);
                        break;
                }
            }

            // Инсертим компоненты в компонентное апи
            machine.componentAPI.list.add(machine.gpuComponent);
            machine.componentAPI.list.add(machine.eepromComponent);
            machine.componentAPI.list.add(machine.keyboardComponent);
            machine.componentAPI.list.add(machine.screenComponent);
            machine.componentAPI.list.add(machine.computerComponent);
            machine.componentAPI.list.add(machine.filesystemComponent);
            machine.componentAPI.list.add(machine.modemComponent);
//            machine.componentAPI.list.add(machine.internetComponent);

            // Пидорасим главное йоба-окошечко так, как надо
            machine.updateControls();
        
            machine.stage.setX(machineConfig.getDouble("x"));
            machine.stage.setY(machineConfig.getDouble("y"));
            machine.stage.setWidth(machineConfig.getDouble("width"));
            machine.stage.setHeight(machineConfig.getDouble("height"));
            
            machine.nameTextField.setText(machineConfig.getString("name"));
            machine.updateTitle();
            
            machine.toolbarHidden = machineConfig.getBoolean("toolbarHidden");
            machine.updateToolbar();

            // При закрытии окошка машину над оффнуть, а то хуй проссыт, будет ли там поток дрочиться или плеер этот асинхронники свои сувать меж булок
            machine.stage.setOnCloseRequest(event -> {
                machine.shutdown(true);
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
    
    
    public JSONObject toJSONObject() {
        JSONArray components = new JSONArray();
        for (int j = 0; j < componentAPI.list.size(); j++) {
            components.put(componentAPI.list.get(j).toJSONObject());
        }
        
        return new JSONObject()
            .put("name", nameTextField.getText())
            .put("x", stage.getX())
            .put("y", stage.getY())
            .put("width", stage.getWidth())
            .put("height", stage.getHeight())
            .put("toolbarHidden", toolbarHidden)
            .put("components", components);
    }
    
    private void updateControls() {
        HDDPathTextField.setText(filesystemComponent.realPath);
        EEPROMPathTextField.setText(eepromComponent.realPath);
    }

    private void updateTitle() {
        stage.setTitle(nameTextField.getText());
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
    }
    
    public void onGenerateButtonTouch() {
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
            Machine.fromJSONObject(generatedMachine);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPowerButtonTouch() {
        new Player("click.mp3");
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

                String file = "/Users/igor/Documents/GitHub/OpenComputersVM/src/vm/libraries/lua" + (use53 ? "53" : "52") + "/native." + architecture + "." + extension;
                System.out.println("Loading lua library: " + file);
                System.load(file);
            });
        }

        public static LuaState load52(int memory) {
            prepareLoad(false);

            LuaState lua = new LuaState(memory);

            lua.openLib(LuaState.Library.BASE);
            lua.openLib(LuaState.Library.BIT32);
            lua.openLib(LuaState.Library.COROUTINE);
            lua.openLib(LuaState.Library.DEBUG);
            lua.openLib(LuaState.Library.ERIS);
            lua.openLib(LuaState.Library.MATH);
            lua.openLib(LuaState.Library.STRING);
            lua.openLib(LuaState.Library.TABLE);
            lua.pop(8);

            return lua;
        }

        public static LuaState load53(int memory) {
            prepareLoad(true);

            LuaState lua = new LuaStateFiveThree(memory);

            lua.openLibs();
            lua.openLib(LuaState.Library.BASE);
            lua.openLib(LuaState.Library.COROUTINE);
            lua.openLib(LuaState.Library.DEBUG);
            lua.openLib(LuaState.Library.ERIS);
            lua.openLib(LuaState.Library.MATH);
            lua.openLib(LuaState.Library.STRING);
            lua.openLib(LuaState.Library.TABLE);
            lua.openLib(LuaState.Library.UTF8);
            lua.pop(8);

            return lua;
        }
    }

    public class LuaThread extends Thread {
        private LuaState[] signalStack;
        private HashMap<KeyCode, Boolean> pressedKeyCodes = new HashMap<>();
        private double lastClickX, lastClickY;

        public LuaThread() {
            signalStack = new LuaState[256];

            Platform.runLater(() -> {
                // Ивенты клавиш всему окну
                windowGridPane.setOnKeyPressed(event -> {
                    // Иначе оно спамит даунами
                    if (!isKeyPressed(event.getCode())) {
                        pressedKeyCodes.put(event.getCode(), true);
                        pushKeySignal(event, "key_down");
                    }
                });

                windowGridPane.setOnKeyReleased(event -> {
                    pressedKeyCodes.put(event.getCode(), false);
                    pushKeySignal(event, "key_up");
                });

                // А эт уже ивенты экранчика)0
                screenImageView.setOnMousePressed(event -> {
                    pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "touch");
                });

                screenImageView.setOnMouseDragged(event -> {
                    double sceneX = event.getSceneX(), sceneY = event.getSceneY();
                    double p = screenImageView.getFitWidth() / screenImageView.getImage().getWidth();
                    if (
                        screenComponent.precise ||
                        (
                            Math.abs(sceneX - lastClickX) / p >= Glyph.WIDTH ||
                            Math.abs(sceneY - lastClickY) / p >= Glyph.HEIGHT
                        )
                    ) {
                        pushTouchSignal(sceneX, sceneY, getOCButton(event), "drag");
                    }
                });

                screenImageView.setOnMouseReleased(event -> {
                    pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "drop");
                });

                screenImageView.setOnScroll(event -> {
                    pushTouchSignal(event.getSceneX(), event.getSceneY(), event.getDeltaY() > 0 ? 1 : -1, "scroll");
                });
            });
        }

        @Override
        public void run() {
            try {
                // Грузим машин-кодыч
                lua.load(IO.loadResourceAsString("machine.lua"), "=machine");
                lua.call(0, 0);

                error("computer halted");
            }
            catch (Exception e) {
                if (e.getMessage().contains("java.lang.ThreadDeath")) {
                    System.out.println("А НУ ПАШОЛ АЦУДА СО СВОИМИ ЭКСШПНАМИУарфа: " + e.getMessage());
                }
                else {
                    error(e.getMessage());
                }
            }
        }

        private void error(String text) {
            gpuComponent.rawError("Unrecoverable error\n\n" + text);
            gpuComponent.update();

            for (int i = 0; i < 2; i++) {
                computerComponent.rawBeep(1400, 0.3);
            }

            powerButton.setSelected(false);
            shutdown(false);
        }
        
        private void pushKeySignal(KeyEvent event, String name) {
            String text = event.getText();
            int OCKeyboardCode = KeyMap.get(event.getCode());

            LuaState luaState = new LuaState();

            luaState.pushString(name);
            luaState.pushString(keyboardComponent.address);
            luaState.pushInteger(text.length() > 0 ? text.codePointAt(0) : OCKeyboardCode);
            luaState.pushInteger(OCKeyboardCode);

            pushSignal(luaState);
        }

        private int getOCButton(MouseEvent event) {
            switch (event.getButton()) {
                case MIDDLE: return 3;
                case SECONDARY: return 2;
                default: return 1;
            }
        }

        private void pushTouchSignal(double screenX, double screenY, int state, String name) {
            double p = screenImageView.getFitWidth() / screenImageView.getImage().getWidth();

            double
                x = (screenX - screenImageView.getLayoutX()) / p / Glyph.WIDTH + 1,
                y = (screenY - screenImageView.getLayoutY()) / p / Glyph.HEIGHT + 1;

            LuaState luaState = new LuaState();
            
            luaState.pushString(name);
            luaState.pushString(screenComponent.address);
            if (screenComponent.precise) {
                luaState.pushNumber(x);
                luaState.pushNumber(y);
            }
            else {
                luaState.pushInteger((int) x);
                luaState.pushInteger((int) y);
            }
            luaState.pushInteger(state);
            luaState.pushString("Player");
            
            pushSignal(luaState);

            lastClickX = screenX;
            lastClickY = screenY;
        }

        public void pushSignal(LuaState signal) {
            int nullIndex = -1;

            for (int i = 0; i < signalStack.length; i++) {
                if (signalStack[i] == null) {
                    nullIndex = i;
                    break;
                }
            }

            if (nullIndex >= 0) {
                signalStack[nullIndex] = signal;
            }

            synchronized (this) {
                notify();
            }
        }

        public LuaState pullSignal(double timeout) {
            synchronized (this) {
                boolean infinite = timeout < 0;
                long deadline = infinite ? 0 : System.currentTimeMillis() + (long) (timeout * 1000);

                while (infinite || System.currentTimeMillis() <= deadline) {
                    if (signalStack[0] != null) {
                        LuaState result = signalStack[0];

                        // Шифтим
                        boolean needClearEnd = signalStack[signalStack.length - 1] != null;

                        for (int i = 1; i < signalStack.length; i++) {
                            signalStack[i - 1] = signalStack[i];
                        }

                        if (needClearEnd) {
                            signalStack[signalStack.length - 1] = null;
                        }

                        return result;
                    }

                    try {
//                    System.out.println("Waiting");
                        if (infinite) {
                            wait();
                        }
                        else {
                            wait(deadline - System.currentTimeMillis());
                        }
                    }
                    catch (InterruptedException e) {
                        System.out.println("computer thread was interrupted");
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
            
            if (resetGPU) {
                gpuComponent.flush();
                gpuComponent.update();
            }
            
            luaThread.interrupt();
            luaThread.stop();
        }
    }

    public void boot() {
        if (!started) {
            started = true;
            startTime = System.currentTimeMillis();

            gpuComponent.flush();
            gpuComponent.update();

            try {
                eepromComponent.loadCode();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            // Бесконечно играем звук компека)00
            computerRunningPlayer = new Player("computer_running.mp3");
            computerRunningPlayer.setRepeating();
            computerRunningPlayer.play();
            
            // Запускаем луа-машину
            luaThread = new LuaThread();
            luaThread.start();
        }
    }
}
