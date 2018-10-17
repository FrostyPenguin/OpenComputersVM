package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;
import vm.IO;

import java.io.File;
import java.io.IOException;

public class EEPROM extends ComponentBase {
    public String data, realPath;

    private String code;
    
    public EEPROM(LuaState lua, String address, String realPath, String dataValue) {
        super(lua, address, "eeprom");

        this.realPath = realPath;
        this.data = dataValue;
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        lua.pushJavaFunction(args -> {
            args.checkString(1);
            
            code = args.toString(1);
            
            return 0;
        });
        lua.setField(-2, "set");

        lua.pushJavaFunction(args -> {
            args.checkString(1);

            data = args.toString(1);

            return 0;
        });
        lua.setField(-2, "setData");

        lua.pushJavaFunction(args -> {
            lua.pushString(code);

            return 1;
        });
        lua.setField(-2, "get");
        
        lua.pushJavaFunction(args -> {
            lua.pushString(data);
            
            return 1;
        });
        lua.setField(-2, "getData");
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
