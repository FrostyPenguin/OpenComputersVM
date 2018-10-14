package vm.computer.components;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import vm.computer.ComponentBase;
import vm.computer.LuaValues;

import java.io.File;

public class Filesystem extends ComponentBase {
    private static final int
        spaceUsed = 0,
        spaceTotal = 12 * 1024 * 1024;
    
    private String
        realPath,
        label;
    
    private File getFsFile(String path) {
        return new File(realPath + path);
    }

    private File getFsFile(Varargs varargs) {
        return getFsFile(varargs.arg1().tojstring());
    }
    
    public Filesystem(String realPath) {
        super("filesystem");
        
        this.realPath = realPath;

        set("getLabel", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(label);
            }
        });

        set("setLabel", new OneArgFunction() {
            public LuaValue call(LuaValue luaValue) {
                luaValue.checkjstring();
                
                label = luaValue.tojstring();
                
                return LuaValue.TRUE;
            }
        });
        
        set("lastModified", new VarArgFunction() {
            public Varargs invoke(Varargs varargs) {
                varargs.arg(1).checkjstring();
                
                File file = getFsFile(varargs);
                if (file.exists()) {
                    return LuaValue.valueOf(getFsFile(varargs).lastModified());
                }
                else {
                    return LuaValues.falseAndReason("file doesn't exists");
                }
            }
        });
        
        set("size", new VarArgFunction() {
            public Varargs invoke(Varargs varargs) {
                varargs.arg(1).checkjstring();
                
                File file = getFsFile(varargs);
                if (file.exists()) {
                    return LuaValue.valueOf(getFsFile(varargs).length());
                }
                else {
                    return LuaValues.falseAndReason("file doesn't exists");
                }
            }
        });
        
        set("exists", new VarArgFunction() {
            public Varargs invoke(Varargs varargs) {
                varargs.arg(1).checkjstring();
                
                return LuaValue.valueOf(getFsFile(varargs).exists());
            }
        });
        
        set("list", new VarArgFunction() {
            public Varargs invoke(Varargs varargs) {
                varargs.arg(1).checkjstring();
                
                File file = getFsFile(varargs);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        File[] list = file.listFiles();
                        LuaTable luaTable = new LuaTable();

                        for (int i = 0; i < list.length; i++) {
                            luaTable.set(i + 1, LuaValue.valueOf(list[i].getName()));
                        }

                        return luaTable;
                    }
                    else {
                        return LuaValues.falseAndReason("path is not a directory");
                    }
                }
                else {
                    return LuaValues.falseAndReason("path doesn't exists");
                }
            }
        });
        
        set("spaceUsed", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(spaceUsed);
            }
        });

        set("spaceTotal", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(spaceTotal);
            }
        });
        
        set("isReadOnly", LuaValues.FALSE_FUNCTION);
        
//        try {
//            File file = new File("aefae");
//            FileInputStream fileInputStream = new FileInputStream(file);
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }
    
//    private class Handle {
//        
//    }
}
