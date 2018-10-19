package vm.computer;

import li.cil.repack.com.naef.jnlua.LuaState;

public class LuaUtils {
    public static void pushVoidFunction(LuaState lua, String name) {
        lua.pushJavaFunction(args -> {
            return 0;
        }); 
        lua.setField(-2, name);
    }

    public static void pushBooleanFunction(LuaState lua, String name, boolean b) {
        lua.pushJavaFunction(args -> {
            lua.pushBoolean(b);
            return 1;
        });
        lua.setField(-2, name);
    }
    
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
    
    public static void printArgs(String prefix, LuaState args) {
        StringBuilder meow = new StringBuilder(prefix);
        
        for (int i = 1; i <= args.getTop(); i++) {
            meow.append(args.toString(i)).append("   ");
        }
        
        System.out.println(meow);
    }
}
