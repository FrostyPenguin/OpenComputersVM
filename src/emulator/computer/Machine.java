package emulator.computer;

import emulator.computer.API.Component;
import emulator.computer.API.Computer;
import emulator.computer.API.Unicode;
import emulator.computer.components.GPU;
import emulator.computer.components.Keyboard;
import emulator.computer.components.Screen;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.lib.jse.JsePlatform;

public class Machine {
    private LuaMachine luaMachine;

    public void shutdown() {
        if (luaMachine != null) {
            luaMachine.interrupt();
        }
    }

    public void boot(ImageView imageView, String code) {
        shutdown();
        luaMachine = new LuaMachine(imageView, code);
        luaMachine.start();
    }
    
    class LuaMachine extends Thread {
        private GridPane windowGridPane;
        private ImageView imageView;
        private String code;

        public LuaMachine(ImageView imageView, String code) {
            this.imageView = imageView;
            this.code = code;
        }

        @Override
        public void run() {
            try {
                Keyboard keyboard = new Keyboard();
                GPU gpu = new GPU(imageView, 80, 25);
                Screen screen = new Screen();

                Signal signal = new Signal(imageView, keyboard, screen);
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
        }
    }
}
