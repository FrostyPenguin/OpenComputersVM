package emulator.computer;

import emulator.computer.API.Component;
import emulator.computer.API.Computer;
import emulator.computer.components.GPU;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.lib.jse.JsePlatform;

public class Machine extends Thread {
    public String initCode;
    
    private double screenScale = 1;
    private Pane screensPane;
    
    private double oldMouseX, oldMouseY;
    private boolean mouseClicked;

    public Machine(Pane screensPane, String initCode) {
        this.screensPane = screensPane;
        this.initCode = initCode;
    }
    
    private void updateScale(ImageView imageView) {
        imageView.setScaleX(screenScale);
        imageView.setScaleY(screenScale);
    }
    
    @Override
    public void run() {
        try {
            ImageView imageView = new ImageView();
            imageView.setLayoutX(0);
            imageView.setLayoutY(0);
            updateScale(imageView);

            Platform.runLater(() -> {
                imageView.setOnMousePressed(event -> {
                    imageView.toFront();
                    
                    oldMouseX = event.getScreenX();
                    oldMouseY = event.getScreenY();
                    mouseClicked = true;
                });

                imageView.setOnMouseDragged(event -> {
                    if (mouseClicked) {
                        double newX = event.getScreenX(), newY = event.getScreenY();

                        imageView.setX(imageView.getX() + newX - oldMouseX);
                        imageView.setY(imageView.getY() + newY - oldMouseY);

                        oldMouseX = newX;
                        oldMouseY = newY;
                    }
                });

                imageView.setOnMouseReleased(event -> {
                    mouseClicked = false;
                });
                
                imageView.setOnScroll(event -> {
                    imageView.toFront();
                    
                    screenScale *= event.getDeltaY() > 0 ? 1.1 : 0.9;
                    updateScale(imageView);
                });

                screensPane.getChildren().add(imageView);
            });

            Globals globals = JsePlatform.standardGlobals();

            Component component = new Component();
            component.add(new GPU(imageView, 80, 25));
            
            globals.set("component", component);
            globals.set("computer", new Computer());

            globals.load(initCode).call();
        }
        catch (LuaError e) {
            System.out.println("Lua error: " + e.getMessage());
        }
    }
}
