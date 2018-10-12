package emulator.computer;

import emulator.computer.components.Keyboard;
import emulator.computer.components.Screen;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.HashMap;

public class Signal extends Thread {
    private Varargs[] stack = new Varargs[256];
    
    private HashMap<KeyCode, Boolean> keyCodes = new HashMap<>();
    private Keyboard keyboard;
    private Screen screen;
    private ImageView imageView;
    
    private double lastX, lastY;
    
    public void push(Varargs signal) {
        int nullIndex = -1;

        for (int i = 0; i < stack.length; i++) {
            if (stack[i] == null) {
                nullIndex = i;
                break;
            }
        }
        
        if (nullIndex >= 0) {
            stack[nullIndex] = signal;
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
                if (stack[0] != null) {
                    Varargs result = stack[0];

                    // Шифтим
                    boolean needClearEnd = stack[stack.length - 1] != null;

                    for (int i = 1; i < stack.length; i++) {
                        stack[i - 1] = stack[i];
                    }

                    if (needClearEnd) {
                        stack[stack.length - 1] = null;
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
        return keyCodes.getOrDefault(keyCode, false);
    }

    private void pushKeySignal(KeyEvent event, String name) {
//        System.out.println("key text: " + event.getText() + ", key code name: " + event.getCode().getName());
        
        String text = event.getText();
        int OCKeyboardCode = KeyMap.get(event.getCode());
                
        push(LuaValue.varargsOf(new LuaValue[] {
            LuaValue.valueOf(name),
            keyboard.get("address"),
            LuaValue.valueOf(text.length() > 0 ? text.codePointAt(0) : OCKeyboardCode),
            LuaValue.valueOf(OCKeyboardCode)
        }));

//        System.out.println("SIGNALSTACK: ");
//        for (Varargs v : stack)
//            if (v != null)
//                System.out.println(v.tojstring());
    }
    
    private int getOCButton(MouseEvent event) {
        switch (event.getButton()) {
            case MIDDLE: return 3;
            case SECONDARY: return 2;
            default: return 1;
        }
    }

    private void pushTouchSignal(double sceneX, double sceneY, int state, String name) {
        imageView.requestFocus();
        
        lastX = sceneX;
        lastY = sceneY;
        
        double
            x = (sceneX - imageView.getX()) / Glyph.WIDTH + 1,
            y = (sceneY - imageView.getY()) / Glyph.HEIGHT + 1;

        push(LuaValue.varargsOf(new LuaValue[] {
            LuaValue.valueOf(name),
            screen.get("address"),
            LuaValue.valueOf(screen.precise ? x : (int) x),
            LuaValue.valueOf(screen.precise ? y : (int) y),
            LuaValue.valueOf(state),
            LuaValue.valueOf("Player")
        }));
    } 
    
    public Signal(ImageView imageView, Keyboard keyboard, Screen screen) {
        super();
        this.keyboard = keyboard;
        this.screen = screen;
        this.imageView = imageView;

        Platform.runLater(() -> {
            imageView.setOnKeyPressed(event -> {
                // Иначе оно спамит даунами
                if (!isKeyPressed(event.getCode())) {
                    keyCodes.put(event.getCode(), true);
                    pushKeySignal(event, "key_down");
                }
            });

            imageView.setOnKeyReleased(event -> {
                keyCodes.put(event.getCode(), false);
                pushKeySignal(event, "key_up");
            });
            
            imageView.setOnMousePressed(event -> {
                pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "touch");
            });

            imageView.setOnMouseDragged(event -> {
                double sceneX = event.getSceneX(), sceneY = event.getSceneY();
                if (screen.precise || (Math.abs(sceneX - lastX) >= Glyph.WIDTH || Math.abs(sceneY - lastY) >= Glyph.HEIGHT))
                    pushTouchSignal(sceneX, sceneY, getOCButton(event), "drag");
            });

            imageView.setOnMouseReleased(event -> {
                pushTouchSignal(event.getSceneX(), event.getSceneY(), getOCButton(event), "drop");
            });

            imageView.setOnScroll(event -> {
                pushTouchSignal(event.getSceneX(), event.getSceneY(), event.getDeltaY() > 0 ? 1 : -1, "scroll");
            });

            imageView.requestFocus();
        });
        
        start();
    }
}
