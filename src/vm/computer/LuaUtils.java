package vm.computer;

import li.cil.repack.com.naef.jnlua.LuaState;

public class LuaUtils {
	public static void pushIntegerFunction(LuaState lua, String name, int value) {
		lua.pushJavaFunction(args -> {
			lua.pushInteger(value);
		    return 1;
		});
		lua.setField(-2, name);
	}

    public static void pushNumberFunction(LuaState lua, String name, double value) {
        lua.pushJavaFunction(args -> {
            lua.pushNumber(value);
            return 1;
        });
        lua.setField(-2, name);
    }
	
	public static void pushVoidFunction(LuaState lua, String name) {
		lua.pushJavaFunction(args -> 0); 
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
					System.out.println("Пошел на хуй" + source.type(i).toString());
					break;
			}
		}
		
		return counter;
	}
	
}
