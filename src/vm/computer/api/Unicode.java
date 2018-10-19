package vm.computer.api;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.Glyph;

public class Unicode {
    public Unicode(LuaState lua) {
        lua.pushJavaFunction(args -> {
            String value = lua.checkString(1);
            int limit = lua.checkInteger(2), width = 0, end = 0;

            while (width < limit) {
                width += Math.max(1, Math.ceil(Glyph.map[value.codePointAt(end)].width / (double) Glyph.WIDTH));
                end++;
            }

            if (end > 1)
                lua.pushString(value.substring(0, end - 1));
            else
                lua.pushString("");
            
            return 1;
        });
        lua.setField(-2,"wtrunc");
        
        lua.pushJavaFunction(args -> {
            String text = args.checkString(1);

            int length = 0;
            for (int i = 0; i < text.length(); i++) {
                length += Math.ceil(Glyph.map[text.codePointAt(i)].width / (double) Glyph.WIDTH);
            }
            lua.pushInteger(length);
            
            return 1;
        });
        lua.setField(-2,"wlen");
        
        lua.pushJavaFunction(args -> {
            lua.pushInteger(Glyph.map[args.checkString(1).codePointAt(0)].width);

            return 1;
        });
        lua.setField(-2,"charWidth");
        
        lua.pushJavaFunction(args -> {
            lua.pushInteger(args.checkString(1).length());

            return 1;
        });
        lua.setField(-2,"len");
        
        lua.pushJavaFunction(args -> {
            lua.pushInteger(args.checkString(1).length());

            return 1;
        });
        lua.setField(-2,"len");
        
        lua.pushJavaFunction(args -> {
            lua.pushString(new String(new int[] {args.checkInteger(1)}, 0, 1));
            
            return 1;
        });
        lua.setField(-2,"char");

        lua.pushJavaFunction(args -> {
            String string = args.checkString(1);
            int from = args.checkInteger(2);
            
            int substringFrom = Math.max(0, from < 0 ? string.length() + from : from - 1), substringTo;
            
            if (args.isNoneOrNil(3)) {
                substringTo = string.length();
            }
            else {
                int to = lua.checkInteger(3);
                substringTo = Math.min(string.length(), to < 0 ? string.length() + to + 1 : to);
            }

            lua.pushString(substringTo > substringFrom ? string.substring(substringFrom, substringTo) : "");
            
            return 1;
        });
        lua.setField(-2,"sub");

        lua.pushJavaFunction(args -> {
            args.checkString(1);

            lua.pushString(args.toString(1).toUpperCase());

            return 1;
        });
        lua.setField(-2,"upper");

        lua.pushJavaFunction(args -> {
            args.checkString(1);

            lua.pushString(args.toString(1).toLowerCase());

            return 1;
        });
        lua.setField(-2,"lower");
    }
}
