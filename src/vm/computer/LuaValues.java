package vm.computer;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LuaValues {
    public static final ZeroArgFunction NIL_FUNCTION = new ZeroArgFunction() {
        public LuaValue call() {
            return LuaValue.NIL;
        }
    };

    public static final ZeroArgFunction TRUE_FUNCTION = new ZeroArgFunction() {
        public LuaValue call() {
            return LuaValue.TRUE;
        }
    };

    public static final ZeroArgFunction FALSE_FUNCTION = new ZeroArgFunction() {
        public LuaValue call() {
            return LuaValue.FALSE;
        }
    };

    public static final LuaTable EMPTY_TABLE = new LuaTable();
    
    public static Varargs falseAndReason(String reason) {
        return LuaValue.varargsOf(new LuaValue[] {
            LuaValue.valueOf(false),
            LuaValue.valueOf(reason)
        });
    }
}
