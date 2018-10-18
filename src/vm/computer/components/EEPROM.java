package vm.computer.components;

import org.json.JSONObject;
import vm.IO;
import vm.computer.Machine;

import java.io.File;
import java.io.IOException;

public class EEPROM extends ComponentBase {
    public String data, realPath;

    private String code;
    
    public EEPROM(Machine machine, String address, String realPath, String dataValue) {
        super(machine, address, "eeprom");

        this.realPath = realPath;
        this.data = dataValue;
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        machine.lua.pushJavaFunction(args -> {
            args.checkString(1);
            
            code = args.toString(1);
            
            return 0;
        });
        machine.lua.setField(-2, "set");

        machine.lua.pushJavaFunction(args -> {
            args.checkString(1);

            data = args.toString(1);

            return 0;
        });
        machine.lua.setField(-2, "setData");

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushString(code);

            return 1;
        });
        machine.lua.setField(-2, "get");
        
        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushString(data);
            
            return 1;
        });
        machine.lua.setField(-2, "getData");
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject()
            .put("data", data)
            .put("path", realPath);
    }

    public void loadCode() throws IOException {
        System.out.println("Loading EEPROM source code from: " + realPath);

        code = IO.loadFileAsString(new File(realPath).toURI());
    }
}
