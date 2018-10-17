package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;

public class Keyboard extends ComponentBase {
    public Keyboard(LuaState lua, String address) {
        super(lua, address, "keyboard");
    }
}
