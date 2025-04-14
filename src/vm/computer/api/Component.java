package vm.computer.api;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.Machine;

public class Component extends APIBase {		
	public Component(Machine machine) {
		super(machine, "component");
	}

	@Override
	public void pushFields() {
		machine.lua.pushJavaFunction(args -> {
			args.checkString(1);
			String address = args.toString(1);
			boolean found[] = {false};
			machine.listComponents.forEach((type, list)->{
				if (!found[0] && list.containsKey(address)){
					machine.lua.rawGet(LuaState.REGISTRYINDEX, list.get(address).proxyReference);
					found[0] = true;
				}
			});
			if(!found[0]){
				machine.lua.pushNil();
				machine.lua.pushString("no such component");
				return 2;
			}
			return 1;
		});
		machine.lua.setField(-2, "proxy");

		machine.lua.pushJavaFunction(args -> {
			String filter = args.isNoneOrNil(1) ? "" : args.checkString(1);
			boolean exact = args.isNoneOrNil(2) || args.checkBoolean(2);

			machine.lua.newTable();
			int tableIndex = machine.lua.getTop();

			machine.listComponents.forEach((type, list)->{
				if (filter.isEmpty() || (exact ? type.equals(filter) : type.contains(filter))) {
					list.forEach((addr,cmp)->{
						machine.lua.pushString(addr);
						machine.lua.pushString(type);
						machine.lua.setTable(tableIndex);
					});
				}
			});

			return 1;
		});
		machine.lua.setField(-2, "list");

		machine.lua.pushJavaFunction(args -> {
			String address = args.checkString(1);

			boolean found[] = {false};
			machine.listComponents.forEach((type, list)->{
				if (!found[0] && list.containsKey(address)){
					machine.lua.pushString(type);
					found[0] = true;
				}
			});
			if(!found[0]){
				machine.lua.pushNil();
				machine.lua.pushString("no such component");
				return 2;
			}
			return 1;
		});
		machine.lua.setField(-2, "type");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(-1);
			return 1;
		});
		machine.lua.setField(-2, "slot");
	}
}
