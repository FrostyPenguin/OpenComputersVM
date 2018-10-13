package VM;

import VM.API.Component;
import VM.API.Computer;
import VM.API.Unicode;
import VM.components.GPU;
import VM.components.Keyboard;
import VM.components.Screen;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Pane;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Machine {
    public String EEPROMPath, HDDPath;
    public boolean running = false;
    
    private LuaMachine luaMachine;
    private GPU gpu;
    private Player computerRunningPlayer;
    private ScreenWidget screenWidget;
    
    public Machine(String EEPROMPath, String HDDPath, Pane screensPane, ToggleButton powerButton) {
        this.EEPROMPath = EEPROMPath;
        this.HDDPath = HDDPath;

        // Добавляем новый экранчик в пиздюлину
        screenWidget = new ScreenWidget();
        screenWidget.setLayoutX(10);
        screenWidget.setLayoutY(10);
        screenWidget.setOnMousePressed(event -> {
            if (!screenWidget.isFocused()) {
                screenWidget.toFront();
                screenWidget.requestFocus();
                Main.currentMachine = this;
                
                powerButton.setSelected(running);
            }
        });
        
        screensPane.getChildren().add(screenWidget);

        // Заранее создаем гпуху для шутдауна и прочей залупы
        this.gpu = new GPU(screenWidget, 80, 25);
    }

    class LuaMachine extends Thread {
        private String code;

        public LuaMachine(String code) {
            this.code = code;
        }

        @Override
        public void run() {
            try {
                Keyboard keyboard = new Keyboard();
                Screen screen = new Screen();

                Signal signal = new Signal(screenWidget, keyboard, screen);
                Computer computer = new Computer(signal);
                Component component = new Component();
                component.add(gpu);
                component.add(keyboard);
                component.add(screen);

                Globals globals = JsePlatform.standardGlobals();

                globals.set("computer", computer);
                globals.set("component", component);
                globals.set("unicode", new Unicode());
                
                globals.load(code).call();
            }
            catch (LuaError e) {
                System.out.println("VM runtime error: " + e.getMessage());
            }

            shutdown();
        }
    }

    public void shutdown() {
        if (luaMachine != null) {
            computerRunningPlayer.stop();
            gpu.flush();
            gpu.update();

            running = false;

            luaMachine.interrupt();
            luaMachine.stop();
        }
    }

    public void boot() {
        try {
            String code = new String(Files.readAllBytes(new File(EEPROMPath).toPath()), StandardCharsets.UTF_8);
            
            computerRunningPlayer = new Player("computer_running.mp3");
            computerRunningPlayer.setRepeating(true);
            computerRunningPlayer.play();

            running = true;

            luaMachine = new LuaMachine(code);
            luaMachine.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
