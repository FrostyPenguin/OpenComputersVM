package vm.computer.controller;

import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import li.cil.repack.com.naef.jnlua.LuaState;
import vm.Main;
import vm.computer.Glyph;
import vm.computer.KeyMap;
import vm.computer.Machine;
import vm.computer.components.Screen;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenController {
    public ImageView screenImageView;
    public GridPane screenGridPane;
    private static final int screenImageViewBlurSize = 82;

    public String address;
    public Machine machine;

    public int GlyphWIDTHMulWidth,GlyphHEIGHTMulHeight;
    private int lastOCPixelClickX, lastOCPixelClickY;

    private final HashMap<KeyCode, String> codes = new HashMap<>();
    private KeyCode lastCode;
    private Timer keyDownRepeater;
    private String keyboard;

    public ScreenController(Machine machine, String address, String keyboard) {
        this.address = address;
        this.machine = machine;
        this.keyboard = keyboard;

    }



    @FXML
    private void initialize(){
        // Applique une ombre manuellement à l'image de l'écran
        DropShadow effect = new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(0, 0, 0, 0.5), 0, 0, 0, 0);
        effect.setWidth(screenImageViewBlurSize + 2);
        effect.setHeight(screenImageViewBlurSize + 2);
        this.screenImageView.setEffect(effect);

        // Ajuste la taille de l'image de l'écran
        this.screenGridPane.widthProperty().addListener((observable, oldValue, newValue) -> this.checkImageViewBindings());
        this.screenGridPane.heightProperty().addListener((observable, oldValue, newValue) -> this.checkImageViewBindings());

        // Focus sur l'écran lors du clic sur cette zone
        screenGridPane.setOnMousePressed(event -> screenImageView.requestFocus());

        // Gestion des événements de clavier pour toute la fenêtre
        screenGridPane.setOnKeyPressed(event -> {
            lastCode = event.getCode();

            // La touche système ne génère jamais d'événement KeyTyped
            if (event.getText().length() == 0) {
                codes.put(lastCode, "");
                pushKeyDownSignalRepeated(lastCode, "");
            }
        });

        // Cet événement suit immédiatement KeyPressed pour les touches non systèmes
        screenGridPane.setOnKeyTyped(event -> {
            if (!codes.containsKey(lastCode)) {
                String character = event.getCharacter();
                codes.put(lastCode, character);
                pushKeyDownSignalRepeated(lastCode, character);
            }
        });

        if(keyboard != null)
        screenGridPane.setOnKeyReleased(event -> {
            KeyCode keyCode = event.getCode();
            if (codes.containsKey(keyCode)) {
                pushKeySignal(keyCode, codes.get(keyCode), "key_up");
                codes.remove(keyCode);
                cancelKeyRepetition();
            }
        });

        // Gestion des événements de toucher et de glisser sur l'écran
        screenImageView.setOnMousePressed(event -> {
            if(machine.luaThread == null) return;
            // Signal de collage depuis le presse-papiers
            if (event.getButton() == MouseButton.MIDDLE) {
                LuaState luaState = new LuaState();

                luaState.pushString("clipboard");
                luaState.pushString(keyboard);
                luaState.pushString(getClipboard());
                luaState.pushString(machine.playerTextField.getText());

                machine.luaThread.pushSignal(luaState);
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

        screenImageView.setOnScroll(event -> pushTouchSignal(event.getSceneX(), event.getSceneY(), event.getDeltaY() > 0 ? 1 : -1, "scroll", true));

        screenImageView.setImage(new Image(Objects.requireNonNull(Main.class.getResource("resources/images/black.png")).toString()));
        setGlyphMul(1280,896);
        checkImageViewBindings();
    }

    private int getOCButton(MouseEvent event) {
        if (Objects.requireNonNull(event.getButton()) == MouseButton.SECONDARY) {
            return 1;
        }
        return 0;
    }

    private void pushKeySignal(KeyCode keyCode, String text, String name) {
        if(keyboard == null) return;
        if(machine.luaThread == null) return;
        KeyMap.OCKey ocKey = KeyMap.get(keyCode);

        LuaState luaState = new LuaState();
        luaState.pushString(name);
        luaState.pushString(((Screen)machine.listComponents.get("screen").get(address)).keyboard);
        luaState.pushInteger(text.length() > 0 ? text.codePointAt(0) : ocKey.unicode);
        luaState.pushInteger(ocKey.ascii);
        luaState.pushString(machine.playerTextField.getText());

        machine.luaThread.pushSignal(luaState);
    }

    // Cette fonction démarre la multiple répétition de l'événement de touche enfoncée
    private void cancelKeyRepetition() {
        if (keyDownRepeater != null) {
            keyDownRepeater.cancel();
            keyDownRepeater.purge();
        }
    }

    private void pushKeyDownSignalRepeated(KeyCode keyCode, String text) {
        pushKeySignal(keyCode, text, "key_down");

        cancelKeyRepetition();
        keyDownRepeater = new Timer();
        keyDownRepeater.schedule(new TimerTask() {
            @Override
            public void run() {
                pushKeySignal(keyCode, text, "key_down");
            }
        }, 500, 50);
    }

    private String getClipboard() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        }
        catch (HeadlessException | IOException | UnsupportedFlavorException e) {
            e.printStackTrace();
        }

        return "";
    }

    public void checkImageViewBindings() {
        double
                width = this.screenGridPane.getWidth(),
                height = this.screenGridPane.getHeight();
        this.screenImageView.setFitWidth(width > this.GlyphWIDTHMulWidth ? this.GlyphWIDTHMulWidth : width);
        this.screenImageView.setFitHeight(height > this.GlyphHEIGHTMulHeight ? this.GlyphHEIGHTMulHeight : height);
    }

    private void pushTouchSignal(double sceneX, double sceneY, int state, String name, boolean notDrag) {
        if(machine.luaThread == null) return;
        Bounds bounds = screenImageView.getBoundsInLocal();
        double
                p1 = (bounds.getWidth() - screenImageViewBlurSize) / this.GlyphWIDTHMulWidth,
                p2 = (bounds.getHeight() - screenImageViewBlurSize) / this.GlyphHEIGHTMulHeight;

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
            luaState.pushString(this.address);
            if (((Screen)machine.listComponents.get("screen").get(address)).precise) {
                luaState.pushNumber(x);
                luaState.pushNumber(y);
            }
            else {
                luaState.pushInteger(OCPixelClickX);
                luaState.pushInteger(OCPixelClickY);
            }
            luaState.pushInteger(state);
            luaState.pushString(machine.playerTextField.getText());

            machine.luaThread.pushSignal(luaState);
        }

        lastOCPixelClickX = OCPixelClickX;
        lastOCPixelClickY = OCPixelClickY;
    }

    public void setGlyphMul(int glyphWIDTHMulWidth,int glyphHEIGHTMulHeight) {
        GlyphHEIGHTMulHeight = glyphHEIGHTMulHeight;
        GlyphWIDTHMulWidth = glyphWIDTHMulWidth;
    }
}
