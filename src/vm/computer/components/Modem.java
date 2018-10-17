package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;
import vm.computer.LuaUtils;
import vm.computer.Machine;

import java.util.HashMap;

public class Modem extends ComponentBase {
    private String wakeMessage;
    private boolean wakeMessageFuzzy;
    private int strength = 512;
    private HashMap<Integer, Boolean> openPorts = new HashMap<>();

    public Modem(LuaState lua, String address, String wakeMessage, boolean wakeMessageFuzzy) {
        super(lua, address, "modem");

        this.wakeMessage = wakeMessage;
        this.wakeMessageFuzzy = wakeMessageFuzzy;
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);

            int port = args.toInteger(1);
            if (rawIsOpen(port)) {
                // Тупо сендим всем машинкам наше йоба-сообщение
                for (Machine machine : Machine.list) {
                    // Нуачо, нах себе-то слать
                    if (!machine.modemComponent.address.equals(address)) {
                        pushMessageSignal(machine, port, args, 2);
                    }
                }

                lua.pushBoolean(true);
                return 1;
            }
            else {
                lua.pushBoolean(false);
                return 1;
            }
        });
        lua.setField(-2, "broadcast");

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            args.checkString(2);

            String remoteAddress = args.toString(1);
            int port = args.toInteger(2);

            if (rawIsOpen(port)) {
                // Продрачиваем машинки и ищем нужную сетевуху
                for (Machine machine : Machine.list) {
                    // ОПАЧКИ СТОПЭ ПОЯСНИ ЗА АДРЕС
                    if (machine.modemComponent.address.equals(remoteAddress)) {
                        pushMessageSignal(machine, port, args, 3);

                        lua.pushBoolean(true);
                        return 1;
                    }
                }
            }

            lua.pushBoolean(false);
            return 1;
        });
        lua.setField(-2, "send");

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);

            int port = args.toInteger(1);
            boolean isClosed = !rawIsOpen(port);
            if (isClosed)
                openPorts.put(port, true);

            lua.pushBoolean(isClosed);
            return 1;
        });
        lua.setField(-2, "open");

        lua.pushJavaFunction(args -> {
            if (args.isNoneOrNil(1)) {
                openPorts.clear();

                lua.pushBoolean(true);
                return 1;
            }
            else {
                int port = args.toInteger(1);
                if (rawIsOpen(port)) {
                    openPorts.put(port, false);

                    lua.pushBoolean(true);
                    return 1;
                }

                lua.pushBoolean(false);
                return 1;
            }
        });
        lua.setField(-2, "close");

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);

            strength = args.toInteger(1);

            return 0;
        });
        lua.setField(-2, "setStrength");

        lua.pushJavaFunction(args -> {
            lua.pushInteger(strength);

            return 1;
        });
        lua.setField(-2, "getStrength");

        lua.pushJavaFunction(args -> {
            lua.pushBoolean(true);

            return 1;
        });
        lua.setField(-2, "isWireless");

        lua.pushJavaFunction(args -> {
            lua.pushInteger(8192);

            return 1;
        });
        lua.setField(-2, "maxPacketSize");
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject()
            .put("wakeMessage", wakeMessage)
            .put("wakeMessageFuzzy", wakeMessageFuzzy);
    }

    private void pushMessageSignal(Machine machine, int port, LuaState message, int fromIndex) {
        // Если удаленный писюк порт открыл
        if (machine.modemComponent.rawIsOpen(port)) {
            LuaState signal = new LuaState();

            signal.pushString("modem_message");
            signal.pushString(machine.modemComponent.address);
            signal.pushString(address);
            signal.pushInteger(port);
            signal.pushInteger(0);
            LuaUtils.pushSignalData(signal, message, fromIndex, message.getTop());

            machine.luaThread.pushSignal(signal);
        }
    }

    public boolean rawIsOpen(int port) {
        return openPorts.getOrDefault(port, false);
    }
}
