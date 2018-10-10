package emulator.computer;

import emulator.computer.API.Component;
import emulator.computer.API.Computer;
import emulator.computer.API.Unicode;
import emulator.computer.components.GPU;
import emulator.computer.components.Keyboard;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;

public class Machine extends Thread {
    public String initCode;
    private Pane screensPane;

    public Machine(Pane screensPane, String initCode) {
        this.screensPane = screensPane;
        this.initCode = initCode;
        
        start();
    }
    
    @Override
    public void run() {
        try {
            ImageView imageView = new ImageView();
            imageView.setLayoutX(0);
            imageView.setLayoutY(0);

            Platform.runLater(() -> {
                screensPane.getChildren().add(imageView);
            });

            Keyboard keyboard = new Keyboard();
            GPU gpu = new GPU(imageView, 80, 25);
            
            Signal signalThread = new Signal(keyboard, imageView);
            Computer computer = new Computer(signalThread);

            Component component = new Component();
            component.add(gpu);
            component.add(keyboard);

            Globals globals = JsePlatform.standardGlobals();
            
            globals.set("computer", computer);
            globals.set("component", component);
            globals.set("unicode", new Unicode());

            globals.load(initCode).call();
        }
        catch (Exception e) {
            System.out.println("VM runtime error: " + e.getMessage());
        }
    }
}
