package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;

public class ComponentBase {
    public int proxyReference;
    public String address, type;
    public LuaState lua;
    
    public ComponentBase(LuaState lua, String address, String type) {
        this.type = type;
        this.lua = lua;
        this.address = address;
        
        lua.newTable();
        pushProxy();
        proxyReference = lua.ref(LuaState.REGISTRYINDEX);
    }
    
    public void pushProxy() {
        lua.pushString(address);
        lua.setField(-2, "address");

        lua.pushString(type);
        lua.setField(-2, "type");

        lua.pushInteger(-1);
        lua.setField(-2, "slot");
    }

    public JSONObject toJSONObject() {
        return new JSONObject()
            .put("type", type)
            .put("address", address);
    }
}
