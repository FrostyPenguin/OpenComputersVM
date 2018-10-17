package vm.computer.api;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.LuaUtils;
import vm.computer.Machine;

public class Computer {
    public Computer(LuaState lua, Machine machine) {
        lua.pushJavaFunction(args -> {
            LuaState signal = machine.luaThread.pullSignal(args.isNoneOrNil(1) ? -1 : args.toNumber(1));

            return LuaUtils.pushSignalData(lua, signal, 1, signal.getTop());
        });
        lua.setField(-2,"pullSignal");
        
        lua.pushJavaFunction(args -> {
            lua.newTable();
            return 1;
        });
        lua.setField(-2,"users");

        lua.pushJavaFunction(args -> {
            lua.pushInteger(lua.getFreeMemory());
            return 1;
        });
        lua.setField(-2,"freeMemory");

        lua.pushJavaFunction(args -> {
            lua.pushBoolean(false);
            return 1;
        });
        lua.setField(-2,"isRobot");

        lua.pushJavaFunction(args -> {
            lua.pushBoolean(true);
            return 1;
        });
        lua.setField(-2,"addUser");

        lua.pushJavaFunction(args -> {
            lua.pushBoolean(true);
            return 1;
        });
        lua.setField(-2,"removeUser");
    }
}
