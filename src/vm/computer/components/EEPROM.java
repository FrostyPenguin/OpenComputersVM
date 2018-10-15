package vm.computer.components;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import vm.Main;

import java.io.File;
import java.io.IOException;

public class EEPROM extends ComponentBase {
    public String data, realPath;
    
    private String code;
    
    public EEPROM(String address, String realPath, String dataValue) {
        super(address,"eeprom");
        
        this.realPath = realPath;
        this.data = dataValue;

        set("set", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                value.checkstring();

                code = value.tojstring();

                return LuaValue.NIL;
            }
        });

        set("get", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(code);
            }
        });

        set("setData", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                value.checkstring();

                data = value.tojstring();

                return LuaValue.NIL;
            }
        });

        set("getData", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(data);
            }
        });
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject().put("data", data).put("path", realPath);
    }
    
    public void loadCode() throws IOException {
        code = Main.loadFile(new File(realPath).toURI());
    }
}