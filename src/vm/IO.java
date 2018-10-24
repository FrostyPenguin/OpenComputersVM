package vm;

import org.json.JSONArray;
import org.json.JSONObject;
import vm.computer.Machine;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IO {
	public static final File
		dataFile = new File(System.getProperty("user.home"), "OpenComputersVM"),
		configFile = new File(dataFile, "Config4.json"),
		librariesFile = new File(dataFile, "Libraries"),
		machinesFile = new File(dataFile, "Machines");
	
	public static InputStream getResourceAsStream(String name) {
		return Main.class.getResourceAsStream(name);
	}
	
	public static byte[] loadFileAsByteArray(URI uri) throws IOException {
		return Files.readAllBytes(Paths.get(uri));
	}

	public static String loadFileAsString(URI uri) throws IOException {
		return new String(loadFileAsByteArray(uri));
	}
	
	public static String loadResourceAsString(String name) throws IOException {
		InputStream inputStream = getResourceAsStream(name);
		
		byte[] buffer = new byte[8 * 1024];
		StringBuilder stringBuilder = new StringBuilder();
		
		int count = 0;
		while ((count = inputStream.read(buffer)) > 0)
			stringBuilder.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
		
		return stringBuilder.toString();
	}
	
	public static void copyResourceToFile(String name, File file) throws IOException {
		Files.copy(
			getResourceAsStream(name),
			file.toPath(),
			StandardCopyOption.REPLACE_EXISTING
		);
	}
	
	public static void unzipResource(String name, File where) throws IOException {
		ZipInputStream zipInputStream = new ZipInputStream(getResourceAsStream(name));
		FileOutputStream fileOutputStream;
		ZipEntry zipEntry;
		File outputFile;
        int readCount;
        byte[] buffer = new byte[4096];
		
		while((zipEntry = zipInputStream.getNextEntry()) != null) {
            outputFile = new File(where, zipEntry.getName());
            
            System.out.println("Unzipping " + outputFile.getPath());
            
		    if (zipEntry.isDirectory()) {
                outputFile.mkdirs();        
            }
            else {
                fileOutputStream = new FileOutputStream(outputFile);

                while ((readCount = zipInputStream.read(buffer)) > 0)
                    fileOutputStream.write(buffer, 0, readCount);

                fileOutputStream.close();
            }
		}
		
		zipInputStream.closeEntry();
		zipInputStream.close();
	}
	
	public static void saveConfig() throws IOException {
		System.out.println("Saving config file...");

		JSONArray JSONMachines = new JSONArray();
		for (Machine machine : Machine.list) {
			JSONMachines.put(machine.toJSONObject());
		}

		Files.write(
			Paths.get(IO.configFile.toURI()),
			new JSONObject()
				.put("machines", JSONMachines)
				.toString(2).getBytes(StandardCharsets.UTF_8)
		);
	}
}
