package vm.computer.components;

import vm.computer.Machine;

public class Keyboard extends ComponentBase {
    public Keyboard(Machine machine, String address) {
        super(machine, address, "keyboard");
    }
}
