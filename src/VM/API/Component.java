package VM.API;

import VM.ComponentBase;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;

import java.util.ArrayList;

public class Component extends LuaTable {
    private ArrayList<ComponentBase> list = new ArrayList<>();

    public void add(ComponentBase c) {
        list.add(c);
    }

    class ListIterator extends LibFunction {
        private int index = 0;
        private LuaValue filter;

        public ListIterator(LuaValue value) {
            filter = value;
        }

        public Varargs invoke(Varargs varargs) {
            if (index < list.size()) {
                if (filter.isnil()) {
                    ComponentBase component = list.get(index);
                    index++;

                    return LuaValue.varargsOf(new LuaValue[] {
                        component.get("address"),
                        component.get("type")
                    });
                }
                else {
                    for (int i = index; i < list.size(); i++) {
                        ComponentBase component = list.get(i);

                        if (component.get("type").eq(filter).toboolean()) {
                            index = i + 1;

                            return LuaValue.varargsOf(new LuaValue[] {
                                component.get("address"),
                                component.get("type")
                            });
                        }
                    }

                    return LuaValue.NIL;
                }
            }
            else {
                return LuaValue.NIL;
            }
        }
    }

    public Component() {
        set("list", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                return new ListIterator(value);
            }
        });

        set("proxy", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                String requiredAddress = value.tojstring();

                for (ComponentBase component : list) {
                    if (component.get("address").tojstring().equals(requiredAddress)) {
                        return component;
                    }
                }

                return LuaValue.NIL;
            }
        });
    }
}