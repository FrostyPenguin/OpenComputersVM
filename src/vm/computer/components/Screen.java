package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;

public class Screen extends ComponentBase {
    public boolean precise;
    
    public Screen(LuaState lua, String address, boolean precise) {
        super(lua, address, "screen");
        
        this.precise = precise;
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        lua.pushJavaFunction(args -> {
            args.checkBoolean(1);

            precise = args.toBoolean(1);
            
            return 0;
        });
        lua.setField(-2, "setPrecise");

        lua.pushJavaFunction(args -> {
            lua.pushBoolean(precise);

            return 1;
        });
        lua.setField(-2, "isPrecise");
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject()
            .put("precise", precise);
    }
}
