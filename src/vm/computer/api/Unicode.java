package vm.computer.api;

import li.cil.repack.com.naef.jnlua.LuaState;

public class Unicode {
    public Unicode(LuaState lua) {
        lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            
            lua.pushString(new String(new int[] {args.toInteger(1)}, 0, 1));
            
            return 1;
        });
        lua.setField(-2,"char");

        lua.pushJavaFunction(args -> {
            args.checkString(1);
            args.checkInteger(2);

            String string = args.toString(1);

            int from = args.toInteger(2);
            int to = args.isNoneOrNil(3) ? string.length() : args.toInteger(3);

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


            
            lua.pushString(from < to ? string.substring(from, to) : "");

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
