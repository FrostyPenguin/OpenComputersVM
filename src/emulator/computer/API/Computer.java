package emulator.computer.API;

import emulator.computer.Signal;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ZeroArgFunction;


public class Computer extends LuaTable {
    private long startTime = System.currentTimeMillis();
    
    private Object mutex = new Object();
    
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
    }
}
