package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;
import vm.computer.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Filesystem extends ComponentBase {
    public String realPath, label;

    private static final int spaceUsed = 0, spaceTotal = 12 * 1024 * 1024;
    private Player[] players = new Player[7];
    private HashMap<Integer, Handle> handles = new HashMap<>();
    
    public Filesystem(LuaState lua, String address, String realPath) {
        super(lua, address, "filesystem");

        this.realPath = realPath;

        // Создаем дохуяллион плееров для воспроизведения наших прекрасных звуков харда
        for (int i = 0; i < players.length; i++) {
            players[i] = new Player("hdd_access" + i + ".mp3");
        }
    }
    
    @Override
    public void pushProxy() {
        super.pushProxy();

        // Чтение из хендла
        lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            args.checkInteger(2);

            int id = args.toInteger(1);
            if (handles.containsKey(id)) {
                playSound();
                lua.pushString(handles.get(id).read(args));

                return 1;
            }
            else {
                lua.pushBoolean(false);
                lua.pushString("handle id doesn't exists");

                return 2;
            }
        });
        lua.setField(-2, "read");
        
        // Запись в хендл
        lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            args.checkString(2);

            int id = args.toInteger(1);
            if (handles.containsKey(id)) {
                playSound();
                lua.pushInteger(handles.get(id).write(args));
                
                return 1;
            }
            else {
                lua.pushBoolean(false);
                lua.pushString("handle id doesn't exists");

                return 2;
            }
        });
        lua.setField(-2, "write");

        // Закрытие хендлов
        lua.pushJavaFunction(args -> {
            args.checkInteger(1);

            int id = args.toInteger(1);
            if (handles.containsKey(id)) {
                handles.get(id).close();
                handles.remove(id);
            }
            
            return 0;
        });
        lua.setField(-2, "close");
        
        // Открытие хендля для чтения/записи
        lua.pushJavaFunction(args -> {
            args.checkString(1);
            
            boolean reading = false, binary = false, append = false;
            if (!args.isNoneOrNil(2)){
                lua.checkString(2);

                String mode = lua.toString(2);
                reading = mode.contains("r");
                binary = mode.contains("b");
                append = mode.contains("a");
            }
            
            File file = getFsFile(args);
            if (file.getParentFile().exists()) {
                lua.pushInteger(reading ? new ReadHandle(file, binary, append).id : new WriteHandle(file, binary, append).id);
                
                return 1;
            }
            else {
                lua.pushBoolean(false);
                lua.pushString("parent directory doesn't exists");
                
                return 2;
            }
        });
        lua.setField(-2, "open");
        
        // Лейбл
        lua.pushJavaFunction(args -> {
            lua.pushString(label);
            
            return 1;
        });
        lua.setField(-2, "getLabel");

        lua.pushJavaFunction(args -> {
            args.checkString(1);
            
            label = args.toString(1);
            
            lua.pushBoolean(true);
            return 1;
        });
        lua.setField(-2, "setLabel");

        // Данные о файле - размер, таймштамп
        lua.pushJavaFunction(args -> {
            args.checkString(1);

            playSound();

            File file = getFsFile(args);
            if (file.exists()) {
                lua.pushInteger((int) file.lastModified());
                
                return 1;
            }
            else {
                lua.pushBoolean(false);
                lua.pushString("file doesn't exists");
                
                return 2;
            }
        });
        lua.setField(-2, "lastModified");

        lua.pushJavaFunction(args -> {
            args.checkString(1);

            playSound();

            File file = getFsFile(args);
            if (file.exists()) {
                lua.pushInteger((int) file.length());

                return 1;
            }
            else {
                lua.pushBoolean(false);
                lua.pushString("file doesn't exists");
                
                return 2;
            }
        });
        lua.setField(-2, "size");

        lua.pushJavaFunction(args -> {
            args.checkString(1);

            playSound();

            lua.pushBoolean(getFsFile(args).exists());
            
            return 1;
        });
        lua.setField(-2, "exists");

        lua.pushJavaFunction(args -> {
            args.checkString(1);

            playSound();

            File file = getFsFile(args);
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] list = file.listFiles();

                    lua.newTable();
                    int tableIndex = lua.getTop();
                    
                    for (int i = 0; i < list.length; i++) {
                        lua.pushInteger(i + 1);
                        lua.pushString(list[i].getName());
                        lua.setTable(tableIndex);
                    }
                    
                    return 1;
                }
                else {
                    lua.pushBoolean(false);
                    lua.pushString("path is not a directory");

                    return 2;
                }
            }
            else {
                lua.pushBoolean(false);
                lua.pushString("path doesn't exists");

                return 2;
            }
        });
        lua.setField(-2, "list");

        lua.pushJavaFunction(args -> {
            lua.pushInteger(spaceUsed);

            return 1;
        });
        lua.setField(-2, "spaceUsed");

        lua.pushJavaFunction(args -> {
            lua.pushInteger(spaceTotal);

            return 1;
        });
        lua.setField(-2, "spaceTotal");

        lua.pushJavaFunction(args -> {
            lua.pushBoolean(false);

            return 1;
        });
        lua.setField(-2, "isReadOnly");
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject().put("path", realPath);
    }

    private abstract class Handle {
        public int id;

        public Handle() {
            do {
                id = ThreadLocalRandom.current().nextInt();
            } while(handles.containsKey(id));

            handles.put(id, this);
        }

        public abstract int write(LuaState args);
        public abstract String read(LuaState args);
        public abstract void close();
    }

    private class ReadHandle extends Handle {
        public FileInputStream in;

        public ReadHandle(File file, boolean binary, boolean append) {
            super();

            try {
                in = new FileInputStream(file);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        public int write(LuaState args) {
            return 0;
        }

        public String read(LuaState args) {
            try {
                byte[] buffer = new byte[args.toInteger(2)];
                int readCount = in.read(buffer);
                
                return new String(buffer, 0, readCount, StandardCharsets.US_ASCII);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            
            return "";
        }

        public void close() {
            try {
                in.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class WriteHandle extends Handle {
        public FileOutputStream out;

        public WriteHandle(File file, boolean binary, boolean append) {
            super();

            try {
                out = new FileOutputStream(file, append);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        public int write(LuaState args) {
            byte[] bytes = args.toString(2).getBytes(StandardCharsets.US_ASCII);

            try {
                out.write(bytes);
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            return bytes.length;
        }

        public String read(LuaState args) {
            return null;
        }

        public void close() {
            try {
                out.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void playSound() {
        Player player = players[ThreadLocalRandom.current().nextInt(0, players.length)];
        player.reset();
        player.play();
    }

    private File getFsFile(String path) {
        return new File(realPath + path);
    }

    private File getFsFile(LuaState args) {
        return getFsFile(args.toString(1));
    }
}
