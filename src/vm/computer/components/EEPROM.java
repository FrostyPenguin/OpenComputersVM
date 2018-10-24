package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.CRC32;

public class EEPROM extends FilesystemBase {
	public String data;
	public byte[] code;
	
	public EEPROM(Machine machine, String address, String label, String realPath, String data) {
		super(machine, address, "eeprom", label, realPath);
		
		this.data = data;
	}

	@Override
	public void pushProxyFields() {
		super.pushProxyFields();

		machine.lua.pushJavaFunction(args -> {
			code = args.checkByteArray(1);

			try {
				Files.write(
					Paths.get(realPath),
					code
				);
			}
			catch (IOException e) {
				e.printStackTrace();
			}

			return 0;
		});
		machine.lua.setField(-2, "set");

		machine.lua.pushJavaFunction(args -> {
			data = args.checkString(1);
			return 0;
		});
		machine.lua.setField(-2, "setData");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushByteArray(code);
			return 1;
		});
		machine.lua.setField(-2, "get");
		
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushString(data);
			return 1;
		});
		machine.lua.setField(-2, "getData");

		machine.lua.pushJavaFunction(args -> {
			CRC32 crc32 = new CRC32();
			crc32.update(code);
			machine.lua.pushString(Long.toHexString(crc32.getValue()));
			
			return 1;
		});
		machine.lua.setField(-2, "getChecksum");
	}

	@Override
	public JSONObject toJSONObject() {
		return super.toJSONObject()
			.put("data", data);
	}
}
