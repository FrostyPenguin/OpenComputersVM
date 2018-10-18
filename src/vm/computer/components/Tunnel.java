package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;

public class Tunnel extends NetworkBase {
    public String channel;
    
    public Tunnel(Machine machine, String address, String channel, String wakeUpMessage, boolean wakeUpMessageFuzzy) {
        super(machine, address, "tunnel", wakeUpMessage, wakeUpMessageFuzzy);
        
        this.channel = channel;
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        machine.lua.pushJavaFunction(args -> {
            for (Machine machine : Machine.list) {
                pushModemMessageSignal(machine, machine.tunnelComponent.address, 0, args, 1);
            }

            machine.lua.pushBoolean(true);
            return 1;
        });
        machine.lua.setField(-2, "send");
        
        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushString(channel);
            return 1;
        });
        machine.lua.setField(-2, "getChannel");
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject()
            .put("channel", channel);
    }
}
