package vm.computer;

import li.cil.repack.com.naef.jnlua.LuaState;

public class LuaUtils {
    public static int pushSignalData(LuaState lua, LuaState source, int from, int to) {
        int counter = 0;
        
        for (int i = from; i <= to; i++) {
            switch (source.type(i)) {
                case NIL: lua.pushNil(); counter++; break;
                case BOOLEAN: lua.pushBoolean(source.toBoolean(i)); counter++; break;
                case NUMBER: lua.pushNumber(source.toNumber(i)); counter++; break;
                case STRING: lua.pushString(source.toString(i)); counter++; break;
                default:
                    System.out.println("Пошел на хуй");
                    break;
            }
        }
        
        return counter;
    }
}
