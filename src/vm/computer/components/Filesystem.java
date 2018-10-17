package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;
import vm.computer.Player;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public class Filesystem extends ComponentBase {
    private static final int
        spaceUsed = 0,
        spaceTotal = 12 * 1024 * 1024;

    public String
        realPath,
        label;

    private Player[] players = new Player[7];
    
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
                    for (int i = 0; i < list.length; i++) {
                        lua.pushInteger(i + 1);
                        lua.pushString(list[i].getName());
                        lua.setTable(1);
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
