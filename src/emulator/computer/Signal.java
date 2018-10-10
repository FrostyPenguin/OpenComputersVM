package emulator.computer;

import emulator.computer.components.Keyboard;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import java.util.HashMap;

public class Signal extends Thread {
    public Varargs[] stack = new Varargs[256];

    private double mouseOldX, mouseOldY;
    private boolean mouseClicked;
    
    private HashMap<KeyCode, Boolean> keyCodes = new HashMap<>();
    private Keyboard keyboard;
    
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
                try {
                    System.out.println("Waiting");
                    if (infinite) {
                        wait();
                    } 
                    else {
                        wait(deadline - System.currentTimeMillis());
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }

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
            }

            return LuaValue.NIL;
        }
    }

    public boolean isKeyPressed(KeyCode keyCode) {
        return keyCodes.getOrDefault(keyCode, false);
    }

    private void pushKeySignal(KeyEvent event, String name) {
        Key key = KeyMap.get(event.getCode());
        
        push(LuaValue.varargsOf(new LuaValue[] {
            LuaValue.valueOf(name),
            keyboard.get("address"),
            LuaValue.valueOf(isKeyPressed(KeyCode.SHIFT) ? key.upper : key.unicode),
            LuaValue.valueOf(key.ascii)
        }));
    }
    
    public Signal(Keyboard keyboard, ImageView imageView) {
        super();
        this.keyboard = keyboard;

        Platform.runLater(() -> {
            imageView.setOnKeyPressed(event -> {
                // Иначе оно спамит даунами
                if (!isKeyPressed(event.getCode())) {
//                    System.out.println("Key down: " + event.getText());

                    keyCodes.put(event.getCode(), true);
                    pushKeySignal(event, "key_down");
                }
            });

            imageView.setOnKeyReleased(event -> {
//                System.out.println("Key up: " + event.getText());

                keyCodes.put(event.getCode(), false);
                pushKeySignal(event, "key_up");
            });
            
            imageView.setOnMousePressed(event -> {
                imageView.toFront();
                imageView.requestFocus();

                mouseOldX = event.getScreenX();
                mouseOldY = event.getScreenY();
                mouseClicked = true;
            });

            imageView.setOnMouseDragged(event -> {
                if (mouseClicked) {
                    double newX = event.getScreenX(), newY = event.getScreenY();

                    imageView.setX(imageView.getX() + newX - mouseOldX);
                    imageView.setY(imageView.getY() + newY - mouseOldY);

                    mouseOldX = newX;
                    mouseOldY = newY;
                }
            });

            imageView.setOnMouseReleased(event -> {
                mouseClicked = false;
            });

            imageView.setOnScroll(event -> {
                imageView.toFront();

                double scale = event.getDeltaY() > 0 ? 1.2 : 0.8;

                new Timeline(
                    new KeyFrame(
                        new Duration(0),
                        new KeyValue(imageView.scaleXProperty(), imageView.getScaleX()),
                        new KeyValue(imageView.scaleYProperty(), imageView.getScaleY())
                    ),
                    new KeyFrame(
                        new Duration(50),
                        new KeyValue(imageView.scaleXProperty(), imageView.getScaleX() * scale),
                        new KeyValue(imageView.scaleYProperty(), imageView.getScaleY() * scale)
                    )
                ).play();
            });

            imageView.requestFocus();
        });
        
        start();
    }
}
