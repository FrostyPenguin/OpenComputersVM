package VM.Computer;

import org.luaj.vm2.LuaTable;

import java.util.UUID;

public class ComponentBase extends LuaTable {
    public ComponentBase(String type) {
        set("type", type);
        set("slot", -1);
        set("address", UUID.randomUUID().toString());
    }
}
