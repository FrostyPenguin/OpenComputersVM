package vm.computer.api;

import vm.computer.Machine;

public abstract class APIBase {
	public Machine machine;
	
	private String name;

	public APIBase(Machine machine, String name) {
		this.machine = machine;
		this.name = name;

		machine.APIList.add(this);
	}

	public void pushTable() {
		machine.lua.newTable();
		pushFields();
		machine.lua.setGlobal(name);
	}
	
	public abstract void pushFields();
}
