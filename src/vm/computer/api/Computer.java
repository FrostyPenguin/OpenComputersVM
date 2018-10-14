package vm.computer.api;

import vm.computer.LuaValues;
import vm.computer.Machine;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.concurrent.ThreadLocalRandom;

public class Computer extends LuaTable {
    private static final double energy = 10000;
    private static final int totalMemory = 1024 * 1024 * 4;
    private static final int
        freeMemoryMin = (int) (totalMemory * 0.1),
        freeMemoryMax = (int) (totalMemory * 0.5);
    
    public Computer(Machine machine) {
        set("isRobot", LuaValues.FALSE_FUNCTION);
        set("users", LuaValues.EMPTY_TABLE);
        set("addUser", LuaValues.TRUE_FUNCTION);
        set("removeUser", LuaValues.TRUE_FUNCTION);

        set("address", new ZeroArgFunction() {
            public LuaValue call() {
                return machine.computerComponent.get("address");
            }
        });

        set("uptime", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf((System.currentTimeMillis() - machine.startTime) / 1000.0d);
            }
        });

        set("pullSignal", new LuaFunction() {
            public Varargs invoke(Varargs timeout) {
                return machine.luaThread.pullSignal(timeout.isnil(1) ? -1 : timeout.tofloat(1));
            }
        });

        set("pushSignal", new LuaFunction() {
            public Varargs invoke(Varargs data) {
                machine.luaThread.pushSignal(data);

                return LuaValue.NIL;
            }
        });

        set("totalMemory", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(totalMemory);
            }
        });

        set("freeMemory", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(ThreadLocalRandom.current().nextInt(freeMemoryMin, freeMemoryMax));
            }
        });

        ZeroArgFunction energyFunction = new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(energy);
            }
        };

        set("energy", energyFunction);
        set("maxEnergy", energyFunction);
    }
}