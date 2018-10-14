package vm.computer.components;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import vm.computer.ComponentBase;
import vm.computer.LuaValues;
import vm.computer.Machine;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class Computer extends ComponentBase {
    private MidiChannel midiChannel;
    
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
        
//        byte[] sin = new byte[(int) (duration * frequency)];
//        double data = 2 * Math.PI * 1000 / frequency;
//
//        for (int i = 0; i < sin.length; i++) {
//            sin[i] = (byte) (Math.sin(data * i) * 127);
//        }
//
//        try {
//            AudioFormat format = new AudioFormat(frequency, 8, 1, true, true);
//            SourceDataLine line = AudioSystem.getSourceDataLine(format);
//
//            line.open(format, frequency);
//            line.write(sin, 0, sin.length);
//            line.start();
//            line.drain();
//            line.close();
//        }
//        catch (LineUnavailableException e) {
//            e.printStackTrace();
//        }
    }
    
    public Computer(Machine machine) {
        super("computer");

        try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            midiChannel = synthesizer.getChannels()[0];
            midiChannel.programChange(19);
        }
        catch (MidiUnavailableException e) {
            e.printStackTrace();
        }

        set("beep", new TwoArgFunction() {
            public LuaValue call(LuaValue frequency, LuaValue duration) {
                frequency.checkint();
                duration.checkint();

                rawBeep(frequency.toint(), duration.todouble());
                
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
                machine.shutdown(true);
                return LuaValue.NIL;
            }
        });
    }
}