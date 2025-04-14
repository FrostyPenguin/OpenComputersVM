package vm.computer.components;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import org.json.JSONObject;
import vm.computer.LuaUtils;
import vm.computer.Machine;
import vm.computer.components.base.ComponentWindowed;
import vm.computer.controller.ScreenController;

import java.io.IOException;

public class Screen extends ComponentWindowed {

	public boolean precise, isOn;
	public int blocksHorizontally, blocksVertically;

	public ScreenController controller;

	public String keyboard;





	public Screen(Machine machine, String address, JSONObject obj) throws IOException {
		super(machine, address, obj);

		this.keyboard = obj.optString("keyboard",null);
		this.precise = obj.optBoolean("precise",false);
		this.blocksHorizontally = obj.optInt("blocksHorizontally",1);
		this.blocksVertically = obj.optInt("blocksVertically",1);
		this.isOn = true;

		FXMLLoader fxmlLoader = new FXMLLoader(Machine.class.getResource("screen.fxml"));
		controller = new ScreenController(machine,address,this.keyboard);
		fxmlLoader.setController(controller);
		if (keyboard != null) {
			stage.setTitle("Screen@" + address + " | Keyboard@" + this.keyboard);
		}else {
			stage.setTitle("Screen@" + address);
		}
		stage.setScene(new Scene(fxmlLoader.load()));
	}


	@Override
	public void pushProxyFields() {
		super.pushProxyFields();

		// Number of screen blocks vertically and horizontally
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(blocksHorizontally);
			machine.lua.pushInteger(blocksVertically);

			return 2;
		});
		machine.lua.setField(-2, "getAspectRatio");
		
		// A table with one single address for the keyboard component
		machine.lua.pushJavaFunction(args -> {
			machine.lua.newTable();
			int tableIndex = machine.lua.getTop();
			if (keyboard != null) {
				machine.lua.pushInteger(1);
				machine.lua.pushString(this.keyboard);
				machine.lua.setTable(tableIndex);
			}

			return 1;
		});
		machine.lua.setField(-2, "getKeyboards");
		
		
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

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(!isOn);
			isOn = true;
			return 1;
		});
		machine.lua.setField(-2, "turnOn");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(isOn);
			isOn = false;
			return 1;
		});
		machine.lua.setField(-2, "turnOff");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(isOn);
			return 1;
		});
		machine.lua.setField(-2, "isOn");

		LuaUtils.pushBooleanFunction(machine.lua, "setTouchModeInverted", true);
		LuaUtils.pushBooleanFunction(machine.lua, "isTouchModeInverted", true);
	}

	@Override
	public JSONObject toJSONObject() {
		return super.toJSONObject()
				.put("precise", precise)
				.put("blocksHorizontally", blocksHorizontally)
				.put("blocksVertically", blocksVertically)
				.put("keyboard",keyboard);
	}
}
