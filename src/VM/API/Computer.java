package VM.API;

import VM.Signal;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.concurrent.ThreadLocalRandom;

public class Computer extends LuaTable {
    private static final int totalMemory = 1024 * 1024 * 4;
    private static final int
        freeMemoryMin = (int) (totalMemory * 0.1),
        freeMemoryMax = (int) (totalMemory * 0.5);
    
    private long startTime = System.currentTimeMillis();
    
    public Computer(Signal signal) {
        set("uptime", new ZeroArgFunction() {
            public synchronized LuaValue call() {
                return LuaValue.valueOf((System.currentTimeMillis() - startTime) / 1000f);
            }
        });
        
        set("pullSignal", new LuaFunction() {
            public synchronized Varargs invoke(Varargs timeout) {
               return signal.pull(timeout.arg(1).isnil() ? -1 : timeout.arg(1).tofloat());
            }
        });
        
        set("pushSignal", new LuaFunction() {
            public synchronized Varargs invoke(Varargs data) {
                signal.push(data);
                
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
    }
}
