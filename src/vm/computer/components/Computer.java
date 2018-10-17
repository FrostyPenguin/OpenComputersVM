package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import vm.computer.Machine;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class Computer extends ComponentBase {
    private MidiChannel midiChannel;
    private Machine machine;
    
    public Computer(LuaState lua, String address, Machine machine) {
        super(lua, address, "computer");
        
        this.machine = machine;

        try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            midiChannel = synthesizer.getChannels()[0];
            midiChannel.programChange(19);
        }
        catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            args.checkInteger(2);

            rawBeep(args.toInteger(1), args.toInteger(2));

            return 0;
        });
        lua.setField(-2,  "beep");

        lua.pushJavaFunction(args -> {
            machine.boot();

            return 0;
        });
        lua.setField(-2,  "start");

        lua.pushJavaFunction(args -> {
            machine.shutdown(true);

            return 0;
        });
        lua.setField(-2,  "stop");

        lua.pushJavaFunction(args -> {
            lua.pushBoolean(true);

            return 1;
        });
        lua.setField(-2,  "isRunning");
    }

    public void rawBeep(int frequency, double duration) {
        try {
            midiChannel.noteOn((int) (frequency / 2000d * 127), 127);
            Thread.sleep((long) (duration * 1000));
            midiChannel.allNotesOff();
            Thread.sleep(100);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
