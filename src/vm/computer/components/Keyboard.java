package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;
import vm.computer.components.base.ComponentBase;

public class Keyboard extends ComponentBase {
    public boolean from_screen = false;
    public Keyboard(Machine machine, String address, JSONObject obj) {
        super(machine, address, obj);
    }

    public Keyboard(Machine machine, String address) {
        super(machine, address, new JSONObject()
        .put("type", "keyboard"));
        from_screen = true;
    }

    @Override
    public JSONObject toJSONObject(){
        if(from_screen) return null;
        return super.toJSONObject();
    }
}
