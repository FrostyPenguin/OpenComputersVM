package vm.computer.components;

import org.json.JSONObject;
import vm.computer.LuaUtils;
import vm.computer.Machine;

public class Screen extends ComponentBase {
	public boolean precise;
	public int blocksHorizontally, blocksVertically;

	public Screen(Machine machine, String address, boolean precise, int blocksHorizontally, int blocksVertically) {
		super(machine, address, "screen");
		
		this.precise = precise;
		this.blocksHorizontally = blocksHorizontally;
		this.blocksVertically = blocksVertically;
	}

	@Override
	public void pushProxyFields() {
		super.pushProxyFields();

		// Количество блоков экрана по вертикали и горизонтали
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(blocksHorizontally);
			machine.lua.pushInteger(blocksVertically);

			return 2;
		});
		machine.lua.setField(-2, "getAspectRatio");
		
		// Табличка с одним единственным адресом клавиатурного компонента
		machine.lua.pushJavaFunction(args -> {
			machine.lua.newTable();
			int tableIndex = machine.lua.getTop();
			
			machine.lua.pushInteger(1);
			machine.lua.pushString(machine.keyboardComponent.address);
			machine.lua.setTable(tableIndex);

			return 1;
		});
		machine.lua.setField(-2, "getKeyboards");
		
		// Дохуя четко ивентящийся дисплей
		machine.lua.pushJavaFunction(args -> {
			boolean oldValue = precise;
			precise = args.checkBoolean(1);
			machine.lua.pushBoolean(oldValue);
			
			return 1;
		});
		machine.lua.setField(-2, "setPrecise");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(precise);
			
			return 1;
		});
		machine.lua.setField(-2, "isPrecise");

		LuaUtils.pushBooleanFunction(machine.lua, "isOn", true);
		LuaUtils.pushBooleanFunction(machine.lua, "turnOn", true);
		LuaUtils.pushBooleanFunction(machine.lua, "turnOff", true);
		LuaUtils.pushBooleanFunction(machine.lua, "setTouchModeInverted", true);
		LuaUtils.pushBooleanFunction(machine.lua, "isTouchModeInverted", true);
	}

	@Override
	public JSONObject toJSONObject() {
		return super.toJSONObject()
			.put("precise", precise)
			.put("blocksHorizontally", blocksHorizontally)
			.put("blocksVertically", blocksVertically);
	}
}
