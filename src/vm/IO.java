package vm;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IO {
    public static InputStream getResourceAsStream(String name) {
        return Main.class.getResourceAsStream("resources/" + name);
    }

    public static String loadFileAsString(URI uri) throws IOException {
        return new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
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
}
