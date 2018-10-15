package vm.computer.components;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class Screen extends ComponentBase {
    public boolean precise = false;
    
    public Screen(String address, boolean p) {
        super(address, "screen");
        
        precise = p;
        
        set("setPrecise", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                value.checkboolean();

                precise = value.toboolean();

                return LuaValue.NIL;
            }
        });

        set("isPrecise", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(precise);
            }
        });
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject().put("precise", precise);
    }
}
