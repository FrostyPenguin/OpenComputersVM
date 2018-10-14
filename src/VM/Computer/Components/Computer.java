package VM.Computer.Components;

import VM.Computer.ComponentBase;
import VM.Computer.LuaValues;
import VM.Computer.Machine;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Computer extends ComponentBase {
    private Machine machine;

    public Computer(Machine machine) {
        super("computer");

        this.machine = machine;

        set("beep", new TwoArgFunction() {
            public LuaValue call(LuaValue luaFrequency, LuaValue luaTime) {
                luaFrequency.checkint();
                luaTime.checkint();

                int
                    frequency = luaFrequency.toint(),
                    time = luaTime.toint();

                byte[] sin = new byte[time * frequency];
                double data = 2 * Math.PI * 1000 / frequency;

                for (int i = 0; i < sin.length; i++) {
                    sin[i] = (byte) (Math.sin(data * i) * 127);
                }

                try {
                    AudioFormat format = new AudioFormat(frequency, 8, 1, true, true);
                    SourceDataLine line = AudioSystem.getSourceDataLine(format);

                    line.open(format, frequency);
                    line.write(sin, 0, sin.length);
                    line.start();
                    line.drain();
                    line.close();
                }
                catch (LineUnavailableException e) {
                    e.printStackTrace();
                }

                return LuaValue.NIL;
            }
        });

        set("isRunning", LuaValues.TRUE_FUNCTION);

        set("start", new ZeroArgFunction() {
            public LuaValue call() {
                machine.boot();
                return LuaValue.NIL;
            }
        });

        set("stop", new ZeroArgFunction() {
            public LuaValue call() {
                machine.shutdown();
                return LuaValue.NIL;
            }
        });
    }
}