package VM.components;

import VM.ComponentBase;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class Screen extends ComponentBase {
    public boolean precise = false;
    
    public Screen() {
        super("screen");
        
        set("setPrecise", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                value.checkboolean();

                precise = value.toboolean();

                return LuaValue.NIL;
            }
        });

        set("isPrecise", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(precise);
            }
        });
    }
}
