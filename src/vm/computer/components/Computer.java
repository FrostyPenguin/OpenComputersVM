package vm.computer.components;

import vm.computer.Machine;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

public class Computer extends ComponentBase {
	private MidiChannel midiChannel;
	
	public Computer(Machine machine, String address) {
		super(machine, address, "computer");
		
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
	public void pushProxyFields() {
		super.pushProxyFields();
		
		machine.lua.pushJavaFunction(args -> {
			rawBeep(args.checkInteger(1), (long) (args.checkNumber(2) * 1000));

			return 0;
		});
		machine.lua.setField(-2,  "beep");

		machine.lua.pushJavaFunction(args -> {
			machine.boot();

			return 0;
		});
		machine.lua.setField(-2,  "start");

		machine.lua.pushJavaFunction(args -> {
			machine.shutdown(true);

			return 0;
		});
		machine.lua.setField(-2,  "stop");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(true);

			return 1;
		});
		machine.lua.setField(-2,  "isRunning");
	}

	public void rawBeep(int frequency, long duration) {
		try {
			midiChannel.noteOn((int) (frequency / 2000d * 127), 127);
			Thread.sleep(duration);
			midiChannel.allNotesOff();
			Thread.sleep(100);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
