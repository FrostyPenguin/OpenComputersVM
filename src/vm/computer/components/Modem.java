package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.Machine;
import vm.computer.components.base.NetworkBase;

import java.util.HashMap;

import org.json.JSONObject;

public class Modem extends NetworkBase {
	private final HashMap<Integer, Boolean> openPorts = new HashMap<>();
	private int strength = 512;

	public Modem(Machine machine, String address, JSONObject obj) {
		super(machine, address, obj);
	}

	@Override
	public void pushProxyFields() {
		super.pushProxyFields();

		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);

			int port = args.toInteger(1);
			if (rawIsOpen(port)) {
				// On envoie bêtement notre message à toutes les machines
				for (Machine machine : Machine.list) 
					if(machine.listComponents.containsKey("modem"))
						machine.listComponents.get("modem").forEach((s,modem) ->{
							pushModemMessageSignal(machine, modem.address, port, args, 2);
				});

				machine.lua.pushBoolean(true);
				return 1;
			}
			else {
				machine.lua.pushBoolean(false);
				return 1;
			}
		});
		machine.lua.setField(-2, "broadcast");

		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(2);
			args.checkString(1);

			String remoteAddress = args.toString(1);
			int port = args.toInteger(2);

			if (rawIsOpen(port)) {
				// Продрачиваем машинки и ищем нужную сетевуху
				for (Machine machine : Machine.list) {
					if(machine.listComponents.containsKey("modem") && machine.listComponents.get("modem").containsKey(remoteAddress)) {
						pushModemMessageSignal(machine, remoteAddress, port, args, 3);
						machine.lua.pushBoolean(true);
						return 1;
					}
				}
			}

			machine.lua.pushBoolean(false);
			return 1;
		});
		machine.lua.setField(-2, "send");

		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);

			int port = args.toInteger(1);
			boolean isClosed = !rawIsOpen(port);
			if (isClosed)
				openPorts.put(port, true);

			machine.lua.pushBoolean(isClosed);
			return 1;
		});
		machine.lua.setField(-2, "open");

		machine.lua.pushJavaFunction(args -> {
			if (args.isNoneOrNil(1)) {
				openPorts.clear();

				machine.lua.pushBoolean(true);
				return 1;
			}
			else {
				int port = args.toInteger(1);
				if (rawIsOpen(port)) {
					openPorts.put(port, false);

					machine.lua.pushBoolean(true);
					return 1;
				}

				machine.lua.pushBoolean(false);
				return 1;
			}
		});
		machine.lua.setField(-2, "close");

		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);

			strength = args.toInteger(1);

			return 0;
		});
		machine.lua.setField(-2, "setStrength");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(strength);

			return 1;
		});
		machine.lua.setField(-2, "getStrength");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(true);

			return 1;
		});
		machine.lua.setField(-2, "isWireless");
	}

	@Override
	public void pushModemMessageSignal(Machine machine, String remoteAddress, int port, LuaState message, int fromIndex) {
		//Si le PC distant a ouvert le port
		try {
			if (((Modem)machine.listComponents.get("modem").get(remoteAddress)).rawIsOpen(port)) {
				super.pushModemMessageSignal(machine, remoteAddress, port, message, fromIndex);
			}
		}catch (NullPointerException ignored){

		}
	}

	public boolean rawIsOpen(int port) {
		return openPorts.getOrDefault(port, false);
	}
}
