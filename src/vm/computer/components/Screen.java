package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;

public class Screen extends ComponentBase {
    public boolean precise;
    
    public Screen(Machine machine, String address, boolean precise) {
        super(machine, address, "screen");
        
        this.precise = precise;
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        machine.lua.pushJavaFunction(args -> {
            args.checkBoolean(1);

            precise = args.toBoolean(1);
            
            return 0;
        });
        machine.lua.setField(-2, "setPrecise");

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushBoolean(precise);

            return 1;
        });
        machine.lua.setField(-2, "isPrecise");
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject()
            .put("precise", precise);
    }
}
