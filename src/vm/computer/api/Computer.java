package vm.computer.api;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.LuaUtils;
import vm.computer.Machine;

public class Computer {
	public static final int
		energy = 85,
        maxEnergy = 100;
	
	public Computer(Machine machine) {
		machine.lua.pushJavaFunction(args -> {
			machine.lua.newTable();
			return 1;
		});
		machine.lua.setField(-2,"getProgramLocations");
		
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushString(machine.temporaryFilesystemComponent.address);

			return 1;
		});
		machine.lua.setField(-2,  "tmpAddress");
		
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushNumber((System.currentTimeMillis() - machine.startTime) / 1000d);
			return 1;
		});
		machine.lua.setField(-2,"uptime");

		machine.lua.pushJavaFunction(args -> {
			LuaState signal = new LuaState();
			LuaUtils.pushSignalData(signal, args, 1, args.getTop());
			machine.luaThread.pushSignal(signal);
			
			return 0;
		});
		machine.lua.setField(-2,"pushSignal");
		
		machine.lua.pushJavaFunction(args -> {
			LuaState signal = machine.luaThread.pullSignal(args.isNoneOrNil(1) ? Double.POSITIVE_INFINITY : args.checkNumber(1));

			return LuaUtils.pushSignalData(machine.lua, signal, 1, signal.getTop());
		});
		machine.lua.setField(-2,"pullSignal");
		
		machine.lua.pushJavaFunction(args -> {
			machine.lua.newTable();
			return 1;
		});
		machine.lua.setField(-2,"users");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(machine.lua.getTotalMemory());
			return 1;
		});
		machine.lua.setField(-2,"totalMemory");
		
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(machine.lua.getFreeMemory());
			return 1;
		});
		machine.lua.setField(-2,"freeMemory");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(false);
			return 1;
		});
		machine.lua.setField(-2,"isRobot");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(true);
			return 1;
		});
		machine.lua.setField(-2,"addUser");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(true);
			return 1;
		});
		machine.lua.setField(-2,"removeUser");

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushInteger(maxEnergy);
            return 1;
        });
        machine.lua.setField(-2,"maxEnergy");
		
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(energy);
			return 1;
		});
		machine.lua.setField(-2,"energy");
	}
}
