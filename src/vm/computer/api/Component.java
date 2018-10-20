package vm.computer.api;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.components.ComponentBase;

import java.util.ArrayList;

public class Component {
	public ArrayList<ComponentBase> list = new ArrayList<>();
		
	public Component(LuaState lua) {
		lua.pushJavaFunction(args -> {
			args.checkString(1);
			String address = args.toString(1);

			for (ComponentBase component : list) {
				if (component.address.equals(address)) {
					lua.rawGet(LuaState.REGISTRYINDEX, component.proxyReference);
					return 1;
				}
			}

			lua.pushNil();
			lua.pushString("no such component");
			return 2;
		});
		lua.setField(-2, "proxy");

		lua.pushJavaFunction(args -> {
			String filter = args.isString(1) ? args.toString(1) : null;
			boolean exact = args.isBoolean(2) ? args.toBoolean(2) : true;

			lua.newTable();
			int tableIndex = lua.getTop();
			
			for (ComponentBase component : list) {
				if (filter == null || (exact ? component.type.equals(filter) : component.type.contains(filter))) {
					lua.pushString(component.address);
					lua.pushString(component.type);
					lua.setTable(tableIndex);
				}
			}
		   
			return 1;
		});
		lua.setField(-2, "list");

		lua.pushJavaFunction(args -> {
			String address = args.checkString(1);
			
			for (ComponentBase component : list) {
				if (component.address.equals(address)) {
					lua.pushString(component.type);
					return 1;
				}
			}

			lua.pushNil();
			lua.pushString("no such component");
			return 2;
		});
		lua.setField(-2, "type");
		
		lua.pushJavaFunction(args -> {
			lua.pushInteger(-1);
			return 1;
		});
		lua.setField(-2, "slot");
	}
}
