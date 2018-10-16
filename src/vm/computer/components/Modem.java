package vm.computer.components;

import org.json.JSONObject;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import vm.computer.LuaValues;
import vm.computer.Machine;

import java.util.HashMap;

public class Modem extends ComponentBase {
    private String wakeMessage;
    private boolean wakeMessageFuzzy;
    private int strength = 512;
    private HashMap<Integer, Boolean> openPorts = new HashMap<>();

    public Modem(String address, String wm, boolean wmf) {
        super(address, "modem");
        
        wakeMessage = wm;
        wakeMessageFuzzy = wmf;

        set("broadcast", new VarArgFunction() {
            public Varargs invoke(Varargs varargs) {
                varargs.checkint(1);
                
                int port = varargs.toint(1);
                if (rawIsOpen(port)) {
                    // Тупо сендим всем машинкам наше йоба-сообщение
                    for (Machine machine : Machine.list) {
                        // Нуачо, нах себе-то слать
                        if (!machine.modemComponent.address.equals(address)) {
                            pushMessageSignal(machine, port, getMessageData(varargs, 2));
                        }
                    }
                    
                    return LuaValue.TRUE;
                }
                else {
                    return LuaValue.FALSE;
                }
            }
        });

        set("send", new VarArgFunction() {
            public Varargs invoke(Varargs varargs) {
                varargs.checkstring(1);
                varargs.checkint(2);

                String remoteAddress = varargs.tojstring(1);
                int port = varargs.toint(2);
                
                if (rawIsOpen(port)) {
                    // Продрачиваем машинки и ищем нужную сетевуху
                    for (Machine machine : Machine.list) {
                        // ОПАЧКИ СТОПЭ ПОЯСНИ ЗА АДРЕС
                        if (machine.modemComponent.address.equals(remoteAddress)) {
                            pushMessageSignal(machine, port, getMessageData(varargs, 3));
                            
                            return LuaValue.TRUE;
                        }
                    }
                }
                
                return LuaValue.FALSE;
            }
        });
        
        set("open", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                value.checkint();

                int port = value.toint();
                boolean isClosed = !rawIsOpen(port);
                if (isClosed)
                    openPorts.put(port, true);

                return LuaValue.valueOf(isClosed);
            }
        });

        set("close", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                if (value.isnil()) {
                    openPorts.clear();
                    
                    return LuaValue.TRUE;
                }
                else {
                    int port = value.toint();
                    if (rawIsOpen(port)) {
                        openPorts.put(port, false);
                        
                        return LuaValue.TRUE;
                    }
                    
                    return LuaValue.FALSE;
                }
            }
        });

        set("setStrength", new OneArgFunction() {
            public LuaValue call(LuaValue value) {
                value.checkint();

                strength = value.toint();

                return LuaValue.NIL;
            }
        });

        set("getStrength", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(strength);
            }
        });

        set("isWireless", LuaValues.TRUE_FUNCTION);
        set("maxPacketSize", LuaValues.integerFunction(8192));
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject().put("wakeMessage", wakeMessage).put("wakeMessageFuzzy", wakeMessageFuzzy);
    }

    private Varargs getMessageData(Varargs varargs, int fromIndex) {
        LuaValue[] data = new LuaValue[varargs.narg() - fromIndex + 1];
        for (int i = fromIndex; i <= varargs.narg(); i++)
            data[i - fromIndex] = varargs.arg(i);

        return LuaValue.varargsOf(data);
    }
    
    private void pushMessageSignal(Machine machine, int port, Varargs message) {
        // Если удаленный писюк порт открыл
        if (machine.modemComponent.rawIsOpen(port)) {
            LuaValue[] data = new LuaValue[message.narg() + 5];

            data[0] = LuaValue.valueOf("modem_message");
            data[1] = LuaValue.valueOf(machine.modemComponent.address);
            data[2] = LuaValue.valueOf(address);
            data[3] = LuaValue.valueOf(port);
            data[4] = LuaValue.valueOf(0);

            for (int i = 1; i <= message.narg(); i++)
                data[4 + i] = message.arg(i);

            machine.luaThread.pushSignal(LuaValue.varargsOf(data));
        }
    }
    
    public boolean rawIsOpen(int port) {
        return openPorts.getOrDefault(port, false);
    }
}