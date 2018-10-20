package vm.computer.api;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.Machine;
import vm.computer.components.ComponentBase;

public class Component extends APIBase {		
	public Component(Machine machine) {
		super(machine, "component");
	}

	@Override
	public void pushFields() {
		machine.lua.pushJavaFunction(args -> {
			args.checkString(1);
			String address = args.toString(1);

			for (ComponentBase component : machine.componentList) {
				if (component.address.equals(address)) {
					machine.lua.rawGet(LuaState.REGISTRYINDEX, component.proxyReference);
					return 1;
				}
			}

			machine.lua.pushNil();
			machine.lua.pushString("no such component");
			return 2;
		});
		machine.lua.setField(-2, "proxy");

		machine.lua.pushJavaFunction(args -> {
			String filter = args.isString(1) ? args.toString(1) : null;
			boolean exact = args.isBoolean(2) ? args.toBoolean(2) : true;

			machine.lua.newTable();
			int tableIndex = machine.lua.getTop();

			for (ComponentBase component : machine.componentList) {
				if (filter == null || (exact ? component.type.equals(filter) : component.type.contains(filter))) {
					machine.lua.pushString(component.address);
					machine.lua.pushString(component.type);
					machine.lua.setTable(tableIndex);
				}
			}

			return 1;
		});
		machine.lua.setField(-2, "list");

		machine.lua.pushJavaFunction(args -> {
			String address = args.checkString(1);

			for (ComponentBase component : machine.componentList) {
				if (component.address.equals(address)) {
					machine.lua.pushString(component.type);
					return 1;
				}
			}

			machine.lua.pushNil();
			machine.lua.pushString("no such component");
			return 2;
		});
		machine.lua.setField(-2, "type");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(-1);
			return 1;
		});
		machine.lua.setField(-2, "slot");
	}
}
