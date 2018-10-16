package vm.computer;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import vm.IO;
import vm.computer.api.Component;
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
    public ImageView screenImageView;
    public ToggleButton powerButton;
    public TextField EEPROMPathTextField, HDDPathTextField, nameTextField;
    public Button toolbarButton;
    
    // Машиновская поебистика
    public boolean started = false;
    public long startTime;
    public LuaThread luaThread;
    public Component componentAPI;
    public EEPROM eepromComponent;
    public GPU gpuComponent;
    public Screen screenComponent;
    public Keyboard keyboardComponent;
    public Computer computerComponent;
    public Filesystem filesystemComponent;
    public Internet internetComponent;
    public Modem modemComponent;

    private Stage stage;
    private Player computerRunningPlayer;
    private boolean toolbarHidden;
    
    public static void fromJSONObject(JSONObject machineConfig) {
        try {
            // Ну че, создаем окошко, грузим фхмл-файл и ставим сцену окошку
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(Machine.class.getResource("Window.fxml"));
            stage.setScene(new Scene(fxmlLoader.load()));
            
            // Выдрачиваем машинку из фхмл-контроллера и запоминаем эту стейдж-залупу
            Machine machine = fxmlLoader.getController();
            machine.stage = stage;
            
            // Инициализируем компоненты из конфига МОШЫНЫ
            machine.componentAPI = new Component();

            JSONArray components = machineConfig.getJSONArray("components");
            JSONObject component;
            String address;
            for (int i = 0; i < components.length(); i++) {
                component = components.getJSONObject(i);
                address = component.getString("address");

                switch (component.getString("type")) {
                    case "gpu":
                        machine.gpuComponent = new GPU(address, machine.screenImageView);
                        machine.gpuComponent.rawSetResolution(component.getInt("width"), component.getInt("height"));
                        machine.gpuComponent.update();
                        break;
                    case "screen":
                        machine.screenComponent = new Screen(address, component.getBoolean("precise"));
                        break;
                    case "keyboard":
                        machine.keyboardComponent = new Keyboard(address);
                        break;
                    case "computer":
                        machine.computerComponent = new Computer(address, machine);
                        break;
                    case "eeprom":
                        machine.eepromComponent = new EEPROM(address, component.getString("path"), component.getString("data"));
                        break;
                    case "filesystem":
                        machine.filesystemComponent = new Filesystem(address, component.getString("path"));
                        break;
                    case "internet":
                        machine.internetComponent = new Internet(address);
                        break;
                    case "modem":
                        machine.modemComponent = new Modem(address, component.getString("wakeMessage"), component.getBoolean("wakeMessageFuzzy"));
                        break;
                }
            }

            // Инсертим компоненты в компонентное апи
            machine.componentAPI.list.add(machine.gpuComponent);
            machine.componentAPI.list.add(machine.keyboardComponent);
            machine.componentAPI.list.add(machine.screenComponent);
            machine.componentAPI.list.add(machine.computerComponent);
            machine.componentAPI.list.add(machine.eepromComponent);
            machine.componentAPI.list.add(machine.filesystemComponent);
            machine.componentAPI.list.add(machine.internetComponent);
            machine.componentAPI.list.add(machine.modemComponent);

            // Пидорасим главное йоба-окошечко так, как надо
            machine.updateControls();
            machine.nameTextField.setText(machineConfig.getString("name"));
            machine.updateTitle();
            machine.stage.setX(machineConfig.getDouble("x"));
            machine.stage.setY(machineConfig.getDouble("y"));
            machine.stage.setWidth(machineConfig.getDouble("width"));
            machine.stage.setHeight(machineConfig.getDouble("height"));
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
        toolbarButton.setText(toolbarHidden ? "<" : ">");
        windowGridPane.getColumnConstraints().set(1, new ColumnConstraints(toolbarHidden ? 0 : 294));
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
        new Player("click.mp3").play();

        if (powerButton.isSelected()) {
            shutdown(true);
        } else {
            boot();
        }
    }

    public class LuaThread extends Thread {
        private Varargs[] signalStack;
        private HashMap<KeyCode, Boolean> pressedKeyCodes = new HashMap<>();
        private Machine machine;
        private double lastClickX, lastClickY;

        public LuaThread(Machine machine) {
            this.machine = machine;

            signalStack = new Varargs[256];

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
        
        private void error(String text) {
            gpuComponent.rawError("Unrecoverable error\n\n" + text);
            gpuComponent.update();

            for (int i = 0; i < 2; i++) {
                computerComponent.rawBeep(1400, 0.3);
            }
            
            shutdown(false);
        }

        @Override
        public void run() {
            try {
                Globals globals = JsePlatform.standardGlobals();
                
                globals.set("debug", new DebugLib().call(LuaValue.NIL, globals));
                globals.set("component", componentAPI);
                globals.set("computer", new vm.computer.api.Computer(machine));
                globals.set("unicode", new Unicode());

                Varargs varargs = globals.load(IO.loadResourceAsString("machine.lua"), "machine").invoke();
                
                if (varargs.narg() > 0) {
                    if (varargs.toboolean(1)) {
                        System.out.println("Result: " + varargs.tojstring());
                    }
                    else {
                        error(varargs.tojstring(2));
                    }
                }
                else {
                    error("computer halted");
                }
            }
            catch (LuaError e) {
                error(e.getMessage());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        private void pushKeySignal(KeyEvent event, String name) {
            String text = event.getText();
            int OCKeyboardCode = KeyMap.get(event.getCode());

            pushSignal(LuaValue.varargsOf(new LuaValue[] {
                LuaValue.valueOf(name),
                LuaValue.valueOf(keyboardComponent.address),
                LuaValue.valueOf(text.length() > 0 ? text.codePointAt(0) : OCKeyboardCode),
                LuaValue.valueOf(OCKeyboardCode)
            }));
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

            pushSignal(LuaValue.varargsOf(new LuaValue[] {
                LuaValue.valueOf(name),
                LuaValue.valueOf(screenComponent.address),
                LuaValue.valueOf(screenComponent.precise ? x : (int) x),
                LuaValue.valueOf(screenComponent.precise ? y : (int) y),
                LuaValue.valueOf(state),
                LuaValue.valueOf("Player")
            }));

            lastClickX = screenX;
            lastClickY = screenY;
        }

        public void pushSignal(Varargs signal) {
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

        public Varargs pullSignal(float timeout) {
            synchronized (this) {
                boolean infinite = timeout < 0;
                long deadline = infinite ? 0 : System.currentTimeMillis() + (long) (timeout * 1000);

                while (infinite || System.currentTimeMillis() <= deadline) {
                    if (signalStack[0] != null) {
                        Varargs result = signalStack[0];

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

                return LuaValue.NIL;
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
            luaThread = new LuaThread(this);
            luaThread.start();
        }
    }
}
