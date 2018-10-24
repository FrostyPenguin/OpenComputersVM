package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;

public class FilesystemBase extends ComponentBase {
	public String realPath, label;

	public FilesystemBase(Machine machine, String address, String type, String label, String realPath) {
		super(machine, address, type);

		this.label = label;
		this.realPath = realPath;
	}

	@Override
	public void pushProxyFields() {
		super.pushProxyFields();

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushString(label);

			return 1;
		});
		machine.lua.setField(-2, "getLabel");

		machine.lua.pushJavaFunction(args -> {
			label = args.checkString(1);
			machine.lua.pushBoolean(true);
			
			return 1;
		});
		machine.lua.setField(-2, "setLabel");
	}

	@Override
	public JSONObject toJSONObject() {
		return super.toJSONObject()
			.put("label", label)
			.put("path", realPath);
	}
}
