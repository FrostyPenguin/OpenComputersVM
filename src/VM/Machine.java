package VM;

import VM.API.Component;
import VM.API.Computer;
import VM.API.Unicode;
import VM.components.GPU;
import VM.components.Keyboard;
import VM.components.Screen;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

public class Machine {
    public static Machine current;
    public boolean running = false;
    
    private GPU gpuComponent;
    private Screen screenComponent;
    private Keyboard keyboardComponent;
    private Player computerRunningPlayer;
    private ScreenWidget screenWidget;

    private LuaThread luaThread;
    private SignalThread signalThread;
    
    private Varargs[] signalStack = new Varargs[256];
    private HashMap<KeyCode, Boolean> pressedKeyCodes = new HashMap<>();

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
    }

    public void focusScreenWidget(boolean force) {
        if (force || !screenWidget.isFocused()) {
            screenWidget.toFront();
            screenWidget.requestFocus();
            Machine.current = this;

            StaticControls.powerButton.setSelected(running);
        }
    }

    class LuaThread extends Thread {
        private String code;

        public LuaThread(String code) {
            this.code = code;
        }

        @Override
        public void run() {
            try {
                Computer computer = new Computer(signalThread);
                
                Component component = new Component();
                component.add(gpuComponent);
                component.add(keyboardComponent);
                component.add(screenComponent);

                Globals globals = JsePlatform.standardGlobals();

                globals.set("computer", computer);
                globals.set("component", component);
                globals.set("unicode", new Unicode());
                
                globals.load(code).call();
            }
            catch (LuaError e) {
                gpuComponent.rawError("VM runtime error: " + e.getMessage());
                gpuComponent.update();
            }
        }
    }

    public void shutdown() {
        if (running) {
            running = false;

            computerRunningPlayer.stop();
            gpuComponent.flush();
            gpuComponent.update();
            
            signalThread.interrupt();
            signalThread.stop();
            
            luaThread.interrupt();
            luaThread.stop();
        }
    }

    public void boot() {
        try {
            running = true;

            String code = new String(Files.readAllBytes(new File(StaticControls.EEPROMPathTextField.getText()).toPath()), StandardCharsets.UTF_8);

            computerRunningPlayer = new Player("computer_running.mp3");
            computerRunningPlayer.setRepeating(true);
            computerRunningPlayer.play();

            // Стартуем листенер сигналов
            signalThread = new SignalThread();
            signalThread.start();
            
            // Запускаем луа-машину
            luaThread = new LuaThread(code);
            luaThread.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class SignalThread extends Thread {
        public SignalThread() {
            Platform.runLater(() -> {
                // Ивенты клавиш всему скринвиджету
                screenWidget.setOnKeyPressed(event -> {
                    // Иначе оно спамит даунами
                    if (!signalThread.isKeyPressed(event.getCode())) {
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

        private void pushKeySignal(KeyEvent event, String name) {
            String text = event.getText();
            int OCKeyboardCode = KeyMap.get(event.getCode());

            signalThread.push(LuaValue.varargsOf(new LuaValue[] {
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

            signalThread.push(LuaValue.varargsOf(new LuaValue[] {
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

        public void push(Varargs signal) {
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

        public Varargs pull(float timeout) {
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
                        System.out.println("Machine thread was interrupted");
                    }
                }

                return LuaValue.NIL;
            }
        }

        public boolean isKeyPressed(KeyCode keyCode) {
            return pressedKeyCodes.getOrDefault(keyCode, false);
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
            imageView.setEffect(new Bloom(0.4));

            // Добавляем говнище на экранчик
            getChildren().addAll(label, imageView);
        }

        public void applyScale() {
            double
                width = gpuComponent.GlyphWIDTHMulWidth * scale,
                height = gpuComponent.GlyphHEIGHTMulHeight * scale;

            setPrefSize(width, height + label.getPrefHeight());

            imageView.setFitWidth(width);
            imageView.setFitHeight(height);

            label.setPrefWidth(width);
        }
    }
}
