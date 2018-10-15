package vm.computer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.jse.JsePlatform;
import vm.Main;
import vm.computer.api.Component;
import vm.computer.api.Unicode;
import vm.computer.components.*;

import java.io.IOException;
import java.net.URISyntaxException;
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

    public Machine(JSONObject machineConfig) {
        // Добавляем новый экранчик в пиздюлину
        screenWidget = new ScreenWidget(
            machineConfig.getDouble("x"),
            machineConfig.getDouble("y"),
            machineConfig.getDouble("scale"),
            machineConfig.getString("name")
        );

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

        // Тута ивенты всего виджета экрана целиком для клавы и фокуса
        screenWidget.setOnMousePressed(event -> {
            focusScreenWidget(false);
        });
        
        Main.instance.screensPane.getChildren().add(screenWidget);
        focusScreenWidget(true);

        // Инициализируем компоненты
        JSONArray components = machineConfig.getJSONArray("components");
        
        JSONObject component;
        String address;
        for (int i = 0; i < components.length(); i++) {
            component = components.getJSONObject(i);
            address = component.getString("address");
            
            switch (component.getString("type")) {
                case "gpu":
                    gpuComponent = new GPU(address, screenWidget);
                    gpuComponent.rawSetResolution(component.getInt("width"), component.getInt("height"));
                    gpuComponent.update();
                    break;
                case "screen":
                    screenComponent = new Screen(address, component.getBoolean("precise"));
                    break;
                case "keyboard":
                    keyboardComponent = new Keyboard(address);
                    break;
                case "computer":
                    computerComponent = new Computer(address, this);
                    break;
                case "eeprom":
                    eepromComponent = new EEPROM(address, component.getString("path"), component.getString("data"));
                    break;
                case "filesystem":
                    filesystemComponent = new Filesystem(address, component.getString("path"));
                    break;
            }
        }
    }

    public void focusScreenWidget(boolean force) {
        if (force || !screenWidget.isFocused()) {
            screenWidget.toFront();
            screenWidget.requestFocus();
            Machine.current = this;

            Main.instance.powerButton.setSelected(started);
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

                Varargs varargs = globals.load(Main.loadResource("machine.lua"), "machine").invoke();
                
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

    public class ScreenWidget extends Pane {
        public ImageView imageView;
        public Label label;
        public Rectangle rectangle;
        public Button closeButton;
        public Slider slider;
        
        public double scale = 1;

        private Timeline scaleTimeline;

        public ScreenWidget(double x, double y, double s, String name) {
            scale = s;
            
            setLayoutX(x);
            setLayoutY(y);
            Main.addStyleSheet(this, "screenWidget.css");
            getStyleClass().setAll("pane");
            
            label = new Label(name);
            label.setPrefHeight(22);
            label.setPadding(new Insets(0, 0, 0, 7));
            label.setAlignment(Pos.CENTER);
            label.getStyleClass().setAll("label");
            
            closeButton = new Button();
            closeButton.setLayoutX(6);
            closeButton.setLayoutY(6);
            closeButton.setPrefSize(10, 10);
            closeButton.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
            closeButton.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
            closeButton.getStyleClass().setAll("actionButton", "closeButton");
            closeButton.setOnMouseClicked(event -> {
                remove();
            });
            
            slider = new Slider(0.4, 1, scale);
            slider.setLayoutX(21);
            slider.setLayoutY(4.5);
            slider.setPrefWidth(65);
            slider.setOnMousePressed(event -> {
                scale = slider.getValue();
                applyScale(100);
            });
            slider.setOnMouseDragged(event -> {
                scale = slider.getValue();
                applyScale(10);
            });

            rectangle = new Rectangle(0, 0, 1, 1);
            rectangle.setLayoutY(label.getPrefHeight());
            rectangle.setSmooth(false);
            rectangle.setFill(Color.color(0.6, 0.6, 0.6));
            
            imageView = new ImageView();
            imageView.setLayoutY(label.getPrefHeight() + 1);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(false);
            imageView.getStyleClass().setAll("imageView");

            // Эффектики
//            imageView.setEffect(new Bloom(0.8));

            // Добавляем говнище на экранчик
            getChildren().addAll(label, imageView, rectangle, closeButton, slider);
        }

        public void applyScale(int duration) {
            double
                newWidth = gpuComponent.GlyphWIDTHMulWidth * scale,
                newHeight = gpuComponent.GlyphHEIGHTMulHeight * scale + label.getPrefHeight() + 1;

            if (scaleTimeline != null)
                scaleTimeline.stop();
            scaleTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(prefWidthProperty(), getPrefWidth()),
                    new KeyValue(prefHeightProperty(), getPrefHeight()),
                    
                    new KeyValue(imageView.fitWidthProperty(), imageView.getFitWidth()),
                    new KeyValue(imageView.fitHeightProperty(), imageView.getFitHeight()),

                    new KeyValue(label.prefWidthProperty(), label.getPrefWidth()),
                    
                    new KeyValue(rectangle.widthProperty(), rectangle.getWidth())
                ),
                new KeyFrame(new Duration(duration),
                    new KeyValue(prefWidthProperty(), newWidth),
                    new KeyValue(prefHeightProperty(), newHeight),

                    new KeyValue(imageView.fitWidthProperty(), newWidth),
                    new KeyValue(imageView.fitHeightProperty(), newHeight - label.getPrefHeight() - 1),

                    new KeyValue(label.prefWidthProperty(), newWidth),

                    new KeyValue(rectangle.widthProperty(), newWidth)
                )
            );

            scaleTimeline.play();
        }
        
        public void remove() {
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(scaleXProperty(), getScaleX()),
                    new KeyValue(scaleYProperty(), getScaleY()),
                    new KeyValue(opacityProperty(), getOpacity())
                ),
                new KeyFrame(new Duration(80),
                    new KeyValue(opacityProperty(), 0)
                ),
                new KeyFrame(new Duration(90),
                    new KeyValue(scaleXProperty(), 0.1),
                    new KeyValue(scaleYProperty(), 0.1)
                )
            );

            timeline.setOnFinished(event -> {
                Main.instance.screensPane.getChildren().remove(this);
                shutdown(false);
            });

            timeline.play();
        }
    }
}
