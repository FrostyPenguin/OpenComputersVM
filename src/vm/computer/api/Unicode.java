package vm.computer.api;

import vm.computer.Glyph;
import vm.computer.Machine;

public class Unicode extends APIBase {
	public Unicode(Machine machine) {
		super(machine, "unicode");
	}

    @Override
    public void pushFields() {
        machine.lua.pushJavaFunction(args -> {
            String value = machine.lua.checkString(1);
            int limit = machine.lua.checkInteger(2), width = 0, end = 0;

            while (width < limit) {
                width += Math.max(1, Math.ceil(Glyph.map[value.codePointAt(end)].width / (double) Glyph.WIDTH));
                end++;
            }

            if (end > 1)
                machine.lua.pushString(value.substring(0, end - 1));
            else
                machine.lua.pushString("");

            return 1;
        });
        machine.lua.setField(-2,"wtrunc");

        machine.lua.pushJavaFunction(args -> {
            String text = args.checkString(1);

            int length = 0;
            for (int i = 0; i < text.length(); i++) {
                length += Math.ceil(Glyph.map[text.codePointAt(i)].width / (double) Glyph.WIDTH);
            }
            machine.lua.pushInteger(length);

            return 1;
        });
        machine.lua.setField(-2,"wlen");

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushInteger(Glyph.map[args.checkString(1).codePointAt(0)].width);

            return 1;
        });
        machine.lua.setField(-2,"charWidth");

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushInteger(args.checkString(1).length());

            return 1;
        });
        machine.lua.setField(-2,"len");

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushInteger(args.checkString(1).length());

            return 1;
        });
        machine.lua.setField(-2,"len");

        machine.lua.pushJavaFunction(args -> {
            machine.lua.pushString(new String(new int[] {args.checkInteger(1)}, 0, 1));

            return 1;
        });
        machine.lua.setField(-2,"char");

        machine.lua.pushJavaFunction(args -> {
            String string = args.checkString(1);
            int from = args.checkInteger(2);

            int substringFrom = Math.max(0, from < 0 ? string.length() + from : from - 1), substringTo;

            if (args.isNoneOrNil(3)) {
                substringTo = string.length();
            }
            else {
                int to = machine.lua.checkInteger(3);
                substringTo = Math.min(string.length(), to < 0 ? string.length() + to + 1 : to);
            }

            machine.lua.pushString(substringTo > substringFrom ? string.substring(substringFrom, substringTo) : "");

            return 1;
        });
        machine.lua.setField(-2,"sub");

        machine.lua.pushJavaFunction(args -> {
            args.checkString(1);

            machine.lua.pushString(args.toString(1).toUpperCase());

            return 1;
        });
        machine.lua.setField(-2,"upper");

        machine.lua.pushJavaFunction(args -> {
            args.checkString(1);

            machine.lua.pushString(args.toString(1).toLowerCase());

            return 1;
        });
        machine.lua.setField(-2,"lower");
    }
}
