package vm.computer.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;

public class Unicode extends LuaTable {
    public Unicode() {
        set("char", new OneArgFunction() {
            public LuaValue call(LuaValue code) {
                code.checkint();
                return LuaValue.valueOf(new String(new int[] {code.toint()}, 0, 1));
            }
        });
    }
}
