package vm.computer;

import org.luaj.vm2.LuaTable;

import java.util.UUID;

public class ComponentBase extends LuaTable {
    public void save() {
        
    }
    
    public ComponentBase(String type) {
        set("type", type);
        set("address", UUID.randomUUID().toString());
        set("slot", -1);
    }
}
