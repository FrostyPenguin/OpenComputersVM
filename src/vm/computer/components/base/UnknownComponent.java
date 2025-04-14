package vm.computer.components.base;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.Machine;
import vm.computer.controller.UnknownController;

import java.io.IOException;

import org.json.JSONObject;

public class UnknownComponent extends ComponentWindowed {
    public UnknownController controller;
    public UnknownComponent(Machine machine, String address, JSONObject obj) throws IOException {
        super(machine, address, obj);
        FXMLLoader fxmlLoader = new FXMLLoader(Machine.class.getResource("unknown.fxml"));
        controller = new UnknownController(machine,address,type);
        fxmlLoader.setController(controller);
        stage.setScene(new Scene(fxmlLoader.load()));
    }


    @Override
    public void pushProxy(){
        machine.lua.newTable();
        super.pushProxyFields();
        machine.lua.newTable();
        machine.lua.pushJavaFunction(args -> {
            String key = args.toString(2);
            machine.lua.pushJavaFunction(_args -> {
                controller.print(key + "() called; answer :");
                try {
                    String[] tokens = controller.read().split("\\s+");
                    int i = 0;
                    while (i < tokens.length) {
                        if (tokens[i] == "true" || tokens[i] == "false") {
                            machine.lua.pushBoolean(tokens[i] == "true");
                        } else {
                            try {
                                machine.lua.pushNumber(Double.parseDouble(tokens[i]));
                            } catch (NumberFormatException e) {
                                machine.lua.pushString(tokens[i]);
                            }
                        }
                        i++;
                    }
                    return i;
                } catch (InterruptedException e) {
                    machine.lua.pushString(e.getMessage());
                }
                return 1;
            });

            return 1;
        });
        machine.lua.setField(-2, "__index");
        machine.lua.setMetatable(-2);
        proxyReference = machine.lua.ref(LuaState.REGISTRYINDEX);

    }
}
