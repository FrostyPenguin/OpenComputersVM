package emulator.computer.API;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;


public class Computer extends LuaTable {
    public LuaValue[] signalStack = new LuaValue[256];
    
    private long startTime = System.currentTimeMillis();
    
    private void shiftSignalStack() {
        // Выискиваем эту залупу
        int firstNullIndex = signalStack.length - 1;
        for (int i = 1; i < signalStack.length; i++) {
            if (signalStack[i] == null) {
                firstNullIndex = i;
                break;
            }
        }
        
        // Шифтим элементы
        for (int i = 1; i < signalStack.length; i++) {
            signalStack[i - 1] = signalStack[i];
        }

        // Чистим вилочкой, учитывая шифтинг
        for (int i = firstNullIndex - 1; i < signalStack.length; i++) {
            signalStack[i] = null;
        }
    }
    
    public Computer() {
        set("uptime", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf((System.currentTimeMillis() - startTime) / 1000f);
            }
        });
        
        set("pullSignal", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue timeout) {
                boolean infinite = timeout.isnil();
                long deadline = infinite ? 0 : System.currentTimeMillis() + (long) (timeout.tofloat() * 1000);

                while (infinite || System.currentTimeMillis() <= deadline) {
                    if (signalStack[0] != null) {
                        LuaValue value = signalStack[0];
                        shiftSignalStack();

                        return value;
                    }
                }

                return LuaValue.NIL;
            }
        });
    }
}
