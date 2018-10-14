package VM;

import VM.API.Component;
import VM.API.Unicode;
import VM.components.GPU;
import VM.components.Keyboard;
import VM.components.Screen;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.effect.Bloom;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Machine {
    private static final double energy = 10000;
    private static final int totalMemory = 1024 * 1024 * 4;
    private static final int
        freeMemoryMin = (int) (totalMemory * 0.1),
        freeMemoryMax = (int) (totalMemory * 0.5);
    
    public static Machine current;
    
    public boolean running = false;
    public long startTime;
    
    private EEPROM eepromComponent;
    private GPU gpuComponent;
    private Screen screenComponent;
    private Keyboard keyboardComponent;
    private Computer computerComponent;
    
    private LuaThread luaThread;
    private SignalThread signalThread;
    
    private Player computerRunningPlayer;
    private ScreenWidget screenWidget;
    private Varargs[] signalStack;
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
        computerComponent = new Computer();
        eepromComponent = new EEPROM();
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
                Globals globals = JsePlatform.standardGlobals();
                
                Component component = new Component();
                
                component.add(gpuComponent);
                component.add(keyboardComponent);
                component.add(screenComponent);
                component.add(computerComponent);
                component.add(eepromComponent);

                globals.set("debug", new DebugLib().call(LuaValue.NIL, globals));
                globals.set("component", component);
                globals.set("computer", new ComputerAPI());
                globals.set("unicode", new Unicode());
                
                Varargs varargs = globals.load(code).invoke();
                System.out.println("Result: " + varargs.tojstring());
            }
            catch (LuaError e) {
                String error = "VM runtime error: " + e.getMessage();
                System.out.println(error);
//                gpuComponent.rawError(error);
//                gpuComponent.update();
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
    
    private String loadResource(String name) {
        return Main.loadFile(Main.class.getResource("../resources/" + name).getFile());
    }

    public void boot() {
        if (!running) {
            running = true;
            startTime = System.currentTimeMillis();

            eepromComponent.code = loadResource("EEPROM.lua");
            
            computerRunningPlayer = new Player("computer_running.mp3");
            computerRunningPlayer.setRepeating(true);
            computerRunningPlayer.play();

            // Стартуем листенер сигналов
            signalThread = new SignalThread();
            signalThread.start();

            // Запускаем луа-машину
//            luaThread = new LuaThread(loadResource("Machine.lua"));
            luaThread = new LuaThread(eepromComponent.code);
            luaThread.start();
        }
    }

    public class SignalThread extends Thread {
        public SignalThread() {
            signalStack = new Varargs[256];
            
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
    
    private LuaValue uptime() {
        return LuaValue.valueOf((System.currentTimeMillis() - startTime) / 1000.0d);
    }
    
    public class ComputerAPI extends LuaTable {
        public ComputerAPI() {
            set("isRobot", LuaValues.FALSE_FUNCTION);
            set("users", LuaValues.EMPTY_TABLE);
            set("addUser", LuaValues.TRUE_FUNCTION);
            set("removeUser", LuaValues.TRUE_FUNCTION);
            
            set("address", new ZeroArgFunction() {
                public LuaValue call() {
                    return computerComponent.get("address");
                }
            });

            set("realTime", new ZeroArgFunction() {
                public LuaValue call() {
                    return uptime();
                }
            });

            set("uptime", new ZeroArgFunction() {
                public LuaValue call() {
                    return uptime();
                }
            });

            set("pullSignal", new LuaFunction() {
                public Varargs invoke(Varargs timeout) {
                    return signalThread.pull(timeout.arg(1).isnil() ? -1 : timeout.arg(1).tofloat());
                }
            });

            set("pushSignal", new LuaFunction() {
                public Varargs invoke(Varargs data) {
                    signalThread.push(data);

                    return LuaValue.NIL;
                }
            });

            set("totalMemory", new ZeroArgFunction() {
                public LuaValue call() {
                    return LuaValue.valueOf(totalMemory);
                }
            });

            set("freeMemory", new ZeroArgFunction() {
                public LuaValue call() {
                    return LuaValue.valueOf(ThreadLocalRandom.current().nextInt(freeMemoryMin, freeMemoryMax));
                }
            });

            ZeroArgFunction energyFunction = new ZeroArgFunction() {
                public LuaValue call() {
                    return LuaValue.valueOf(energy);
                }
            };
            
            set("energy", energyFunction);
            set("maxEnergy", energyFunction);
        }
    }

    public class Computer extends ComponentBase {
        public Computer() {
            super("computer");
            
            set("beep", new TwoArgFunction() {
                public LuaValue call(LuaValue time, LuaValue frequency) {
                    return LuaValue.NIL;
                }
            });
            
            set("isRunning", LuaValues.TRUE_FUNCTION);
            
            set("start", new ZeroArgFunction() {
                public LuaValue call() {
                    boot();
                    return LuaValue.NIL;
                }
            });
            
            set("stop", new ZeroArgFunction() {
                public LuaValue call() {
                    shutdown();
                    return LuaValue.NIL;
                }
            });
        }
    }
    
    public class EEPROM extends ComponentBase {
        public String data, code;
        
        public EEPROM() {
            super("eeprom");
            
            set("set", new OneArgFunction() {
                public LuaValue call(LuaValue value) {
                    value.checkstring();

                    code = value.tojstring();

                    return LuaValue.NIL;
                }
            });

            set("get", new ZeroArgFunction() {
                public LuaValue call() {
                    return LuaValue.valueOf(code);
                }
            });
            
            set("setData", new OneArgFunction() {
                public LuaValue call(LuaValue value) {
                    value.checkstring();
                    
                    data = value.tojstring();
                    
                    return LuaValue.NIL;
                }
            });

            set("getData", new ZeroArgFunction() {
                public LuaValue call() {
                    return LuaValue.valueOf(data);
                }
            });
        }
    }
}
