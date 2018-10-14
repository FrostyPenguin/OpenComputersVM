package vm.computer.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

public class Unicode extends LuaTable {
    public Unicode() {
        set("char", new OneArgFunction() {
            public LuaValue call(LuaValue code) {
                code.checkint();
                
                return LuaValue.valueOf(new String(new int[] {code.toint()}, 0, 1));
            }
        });

        set("sub", new VarArgFunction() {
            public Varargs invoke(Varargs varargs) {
                varargs.checkjstring(1);
                varargs.checkint(2);
                
                String string = varargs.tojstring(1);
                int from = varargs.toint(2);
                int to = varargs.isnil(3) ? string.length() : varargs.toint(3);

//                System.out.println("INITIAL FROM/TO: " + from + ", " + to);

                from = from < 0 ? string.length() + from : from - 1;
                to = to < 0 ? string.length() + to + 1 : to;
                
                if (from >= string.length()) {
//                    System.out.println("FROM >= STRING LENGTH: " + from + ", " + string.length());
                    from = string.length() - 1;
                }

                if (to > string.length()) {
//                    System.out.println("TO > STRING LENGTH: " + to + ", " + string.length());
                    to = string.length();
                }

//                System.out.println("RESULTING FROM/TO: " + from + ", TO: " + to);
                
                if (from >= to) {
                    return LuaValue.valueOf("");
                }
                else {
                    return LuaValue.valueOf(string.substring(from, to));
                }
            }
        });

        set("upper", new OneArgFunction() {
            public LuaValue call(LuaValue luaValue) {
                luaValue.checkjstring();
                
                return LuaValue.valueOf(luaValue.tojstring().toUpperCase());
            }
        });

        set("lower", new OneArgFunction() {
            public LuaValue call(LuaValue luaValue) {
                luaValue.checkjstring();

                return LuaValue.valueOf(luaValue.tojstring().toLowerCase());
            }
        });
    }
}
