package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;
import vm.computer.components.base.NetworkBase;

import java.util.Objects;
import java.util.UUID;

public class Tunnel extends NetworkBase {
	public String channel;
	
	public Tunnel(Machine machine, String address, JSONObject obj) {
		super(machine, address, obj);

		this.channel = obj.optString("channel",UUID.randomUUID().toString());
	}

	@Override
	public void pushProxyFields() {
		super.pushProxyFields();

		machine.lua.pushJavaFunction(args -> {
			for (Machine machine : Machine.list) 
				if(machine.listComponents.get("tunnel") != null) 
					machine.listComponents.get("tunnel").forEach(((s, tunnel_base) -> {
						Tunnel tunnel = (Tunnel)tunnel_base;
						if (Objects.equals(tunnel.channel, this.channel))
							pushModemMessageSignal(machine, tunnel.address, 0, args, 1);
			}));			

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
