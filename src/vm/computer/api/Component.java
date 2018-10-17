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

            return 0;
        });
        lua.setField(-2, "proxy");

        lua.pushJavaFunction(args -> {
            final int[] index = {0};

            String filter = args.isString(1) ? args.toString(1) : null;
            boolean exact = args.isBoolean(2) ? args.toBoolean(2) : true;

            lua.pushJavaFunction(iteratorArgs -> {
                if (index[0] < list.size()) {
                    if (filter == null) {
                        ComponentBase component = list.get(index[0]);

                        lua.pushString(component.address);
                        lua.pushString(component.type);

                        index[0]++;

                        return 2;
                    }
                    else {
                        for (int i = index[0]; i < list.size(); i++) {
                            ComponentBase component = list.get(i);

                            if (exact ? component.type.equals(filter) : component.type.contains(filter)) {
                                lua.pushString(component.address);
                                lua.pushString(component.type);

                                index[0] = i + 1;

                                return 2;
                            }
                        }

                        return 0;
                    }
                }
                else {
                    return 0;
                }
            });

            return 1;
        });
        lua.setField(-2, "list");
    }
}
