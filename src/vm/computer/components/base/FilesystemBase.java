package vm.computer.components.base;

import org.json.JSONObject;
import vm.computer.Machine;

public class FilesystemBase extends ComponentBase {
	public String realPath, label;

	public FilesystemBase(Machine machine, String address, JSONObject obj) {
		super(machine, address, obj);

		this.label = obj.optString("label","");
		this.realPath = obj.getString("path");
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
