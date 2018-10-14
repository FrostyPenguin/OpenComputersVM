package vm.computer.components;

import vm.computer.ComponentBase;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class EEPROM extends ComponentBase {
    public String data, code;

    public EEPROM() {
        super("eeprom");

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
}