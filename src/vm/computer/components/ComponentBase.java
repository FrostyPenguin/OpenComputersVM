package vm.computer.components;

import org.json.JSONObject;
import org.luaj.vm2.LuaTable;

public class ComponentBase extends LuaTable {
    public String type, address;
    
    public ComponentBase(String address, String type) {
        this.type = type;
        this.address = address;

//        UUID.randomUUID().toString()
        
        set("type", type);
        set("address", address);
        set("slot", -1);
    }

    public JSONObject toJSONObject() {
        return new JSONObject().put("type", type).put("address", address);
    }
}
