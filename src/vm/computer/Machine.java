package vm.computer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import vm.Main;
import vm.StaticControls;
import vm.computer.api.Component;
import vm.computer.api.Unicode;
import vm.computer.components.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class Machine {
    public static Machine current;
    
    public boolean started = false;
    public long startTime;
    
    public EEPROM eepromComponent;
    public GPU gpuComponent;
    public Screen screenComponent;
    public Keyboard keyboardComponent;
    public Computer computerComponent;
    public Filesystem filesystemComponent;
    
    public LuaThread luaThread;
    
    private Player computerRunningPlayer;
    private ScreenWidget screenWidget;
    private double lastClickX, lastClickY;

    public Machine() {
        // Добавляем новый экранчик в пиздюлину
        screenWidget = new ScreenWidget();
        screenWidget.setLayoutX(30);
        screenWidget.setLayoutY(30);

        // Впездываем этой хуйне ебливой (ну, которая лейбл) всякие ивенты и прочую залупу
        screenWidget.label.setOnMousePressed(event -> {
            lastClickX = event.getSceneX();
            lastClickY = event.getSceneY();
        });

        screenWidget.label.setOnMouseDragged(event -> {
            screenWidget.setLayoutX(screenWidget.getLayoutX() + event.getSceneX() - lastClickX);
            screenWidget.setLayoutY(screenWidget.getLayoutY() + event.getSceneY() - lastClickY);

            lastClickX = event.getSceneX();
            lastClickY = event.getSceneY();
        });

        screenWidget.label.setOnScroll(event -> {
            screenWidget.scale += event.getDeltaY() > 0 ? 0.05 : -0.05;
            screenWidget.applyScale();
        });

        // Тута ивенты всего виджета экрана целиком для клавы и фокуса
        screenWidget.setOnMousePressed(event -> {
            focusScreenWidget(false);
        });
        
        StaticControls.screensPane.getChildren().add(screenWidget);
        focusScreenWidget(true);

        // Инициализируем некоторые компоненты
        gpuComponent = new GPU(screenWidget);
        gpuComponent.rawSetResolution(80, 25);
        gpuComponent.update();
        
        keyboardComponent = new Keyboard();
        screenComponent = new Screen();
        computerComponent = new Computer(this);
        eepromComponent = new EEPROM();
        filesystemComponent = new Filesystem(StaticControls.HDDPathTextField.getText());
    }

    public void focusScreenWidget(boolean force) {
        if (force || !screenWidget.isFocused()) {
            screenWidget.toFront();
            screenWidget.requestFocus();
            Machine.current = this;

            StaticControls.powerButton.setSelected(started);
        }
    }

    public class LuaThread extends Thread {
        private Varargs[] signalStack;
        private HashMap<KeyCode, Boolean> pressedKeyCodes = new HashMap<>();
        private Machine machine;

        public LuaThread(Machine machine) {
            this.machine = machine;

            signalStack = new Varargs[256];

            Platform.runLater(() -> {
                // Ивенты клавиш всему скринвиджету
                screenWidget.setOnKeyPressed(event -> {
                    // Иначе оно спамит даунами
                    if (!isKeyPressed(event.getCode())) {
                        pressedKeyCodes.put(event.getCode(), true);
                        pushKeySignal(event, "key_down");
                    }
                });

                screenWidget.setOnKeyReleased(event -> {
                    pressedKeyCodes.put(event.getCode(), false);
                    pushKeySignal(event, "key_up");
                });

                // А эт уже ивенты экранчика)0
                screenWidget.imageView.setOnMousePressed(event -> {
                    pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "touch");
                });

                screenWidget.imageView.setOnMouseDragged(event -> {
                    double sceneX = event.getSceneX(), sceneY = event.getSceneY();
                    if (screenComponent.precise || (Math.abs(sceneX - lastClickX) / screenWidget.scale >= Glyph.WIDTH || Math.abs(sceneY - lastClickY) / screenWidget.scale >= Glyph.HEIGHT )) {
                        pushTouchSignal(sceneX, sceneY, getOCButton(event), "drag");
                    }
                });

                screenWidget.imageView.setOnMouseReleased(event -> {
                    pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "drop");
                });

                screenWidget.imageView.setOnScroll(event -> {
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

                Component component = new Component();

                component.add(gpuComponent);
                component.add(keyboardComponent);
                component.add(screenComponent);
                component.add(computerComponent);
                component.add(eepromComponent);
                component.add(filesystemComponent);

                globals.set("debug", new DebugLib().call(LuaValue.NIL, globals));
                globals.set("component", component);
                globals.set("computer", new vm.computer.api.Computer(machine));
                globals.set("unicode", new Unicode());

                Varargs varargs = globals.load(loadResource("machine.lua"), "machine").invoke();
                
                if (varargs.narg() > 0) {
                    if (varargs.arg(1).toboolean()) {
                        System.out.println("Result: " + varargs.tojstring());
                    }
                    else {
                        error(varargs.arg(2).tojstring());
                    }
                }
                else {
                    error("computer halted");
                }
            }
            catch (LuaError e) {
                error(e.getMessage());
            }
            catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
        
        private void pushKeySignal(KeyEvent event, String name) {
            String text = event.getText();
            int OCKeyboardCode = KeyMap.get(event.getCode());

            pushSignal(LuaValue.varargsOf(new LuaValue[] {
                LuaValue.valueOf(name),
                keyboardComponent.get("address"),
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
            double
                x = (screenX - screenWidget.getLayoutX()) / Glyph.WIDTH / screenWidget.scale + 1,
                y = (screenY - screenWidget.getLayoutY() - screenWidget.imageView.getLayoutY()) / Glyph.HEIGHT / screenWidget.scale + 1;

            pushSignal(LuaValue.varargsOf(new LuaValue[] {
                LuaValue.valueOf(name),
                screenComponent.get("address"),
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
    
    private String loadFile(URI uri) throws IOException {
        return new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
    }
    
    private String loadResource(String name) throws URISyntaxException, IOException {
        return loadFile(Main.class.getResource("resources/" + name).toURI());
    }

    public void boot() {
        if (!started) {
            started = true;
            startTime = System.currentTimeMillis();

            try {
                eepromComponent.code = loadFile(new File(StaticControls.EEPROMPathTextField.getText()).toURI());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            // Бесконечно играем звук компека)00
            computerRunningPlayer = new Player("computer_running.mp3");
            computerRunningPlayer.setRepeating(true);
            computerRunningPlayer.play();
            
            // Запускаем луа-машину
            luaThread = new LuaThread(this);
            luaThread.start();
        }
    }

    public class ScreenWidget extends Pane {
        public ImageView imageView;
        public Label label;
        public double scale = 1;

        public ScreenWidget() {
            label = new Label("My cool machine");
            label.setPrefHeight(20);
            label.setPadding(new Insets(0, 0, 0, 5));
            Main.addStyleSheet(label, "screenLabel.css");
            
            imageView = new ImageView();
            imageView.setLayoutY(label.getPrefHeight());
            imageView.setPreserveRatio(false);
            imageView.setSmooth(false);

            Main.addStyleSheet(this, "screenWidget.css");
            this.setId("eblo");

            // Эффектики
//            imageView.setEffect(new Bloom(0.4));

            // Добавляем говнище на экранчик
            getChildren().addAll(label, imageView);
        }

        public void applyScale() {
            double
                newWidth = gpuComponent.GlyphWIDTHMulWidth * scale,
                newHeight = gpuComponent.GlyphHEIGHTMulHeight * scale;

            new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(prefWidthProperty(), getPrefWidth()),
                    new KeyValue(prefHeightProperty(), getPrefHeight()),
                    
                    new KeyValue(imageView.fitWidthProperty(), imageView.getFitWidth()),
                    new KeyValue(imageView.fitHeightProperty(), imageView.getFitHeight()),

                    new KeyValue(label.prefWidthProperty(), label.getPrefWidth())
                ),
                new KeyFrame(new Duration(100),
                    new KeyValue(prefWidthProperty(), newWidth + label.getPrefHeight()),
                    new KeyValue(prefHeightProperty(), newHeight),

                    new KeyValue(imageView.fitWidthProperty(), newWidth),
                    new KeyValue(imageView.fitHeightProperty(), newHeight),

                    new KeyValue(label.prefWidthProperty(), newWidth)
                )
            ).play();
        }
    }
}
