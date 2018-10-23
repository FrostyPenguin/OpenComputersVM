package vm.computer.components;

import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;
import vm.computer.Machine;
import vm.computer.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Filesystem extends ComponentBase {
	private static final int
		readBufferSize = 4096,
		spaceUsed = 0,
		spaceTotal = 12 * 1024 * 1024;
	private static final long soundNextDelay = 500;

	public String realPath;
	public String label;
	
	private boolean temporary;
	private long soundNextPlay = 0;
	private int soundIndex = 0;
	private Player[] players = new Player[7];
	private HashMap<Integer, Handle> handles = new HashMap<>();
	
	public Filesystem(Machine machine, String address, String label, String realPath, boolean temporary) {
		super(machine, address, "filesystem");

		this.realPath = realPath;
		this.label = label;
		this.temporary = temporary;

		// Создаем дохуяллион плееров для воспроизведения наших прекрасных звуков харда
		for (int i = 0; i < players.length; i++)
			players[i] = new Player("hdd_access" + i + ".mp3");
	}
	
	@Override
	public void pushProxyFields() {
		super.pushProxyFields();

		// Продрачивание по хендлу бладсикером
		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);
			args.checkString(2);
			args.checkInteger(3);
			
			int id = args.toInteger(1);
			if (handles.containsKey(id)) {
				playSound();

				try {
					RandomAccessFile randomAccessFile = handles.get(id).randomAccessFile;
					long value = args.toInteger(3);
					
					switch (args.toString(2)) {
						case "cur":
							randomAccessFile.seek(randomAccessFile.getFilePointer() + value);
							break;
						case "end":
							randomAccessFile.seek(randomAccessFile.length());
							break;
						default:
							randomAccessFile.seek(value);
							break;
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}

				machine.lua.pushBoolean(true);
				return 1;
			}
			else {
				return pushHandleNotExists();
			}
		});
		machine.lua.setField(-2, "seek");
		
		// Чтение из хендла
		machine.lua.pushJavaFunction(args -> {
			int id = args.checkInteger(1);
			if (handles.containsKey(id)) {
				playSound();

				byte[] result = handles.get(id).read(args.checkNumber(2));
				if (result.length > 0) {
					machine.lua.pushByteArray(result);
					return 1;
				}
				else {
					machine.lua.pushNil();
					return 1;
				}
			}
			else {
				return pushHandleNotExists();
			}
		});
		machine.lua.setField(-2, "read");
		
		// Запись в хендл
		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);
			args.checkString(2);

			int id = args.toInteger(1);
			if (handles.containsKey(id)) {
				playSound();
				
				byte[] bytes = args.checkByteArray(2);
				handles.get(id).write(bytes);
				machine.lua.pushInteger(bytes.length);
				
				return 1;
			}
			else {
				return pushHandleNotExists();
			}
		});
		machine.lua.setField(-2, "write");

		// Закрытие хендлов
		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);

			int id = args.toInteger(1);
			if (handles.containsKey(id)) {
				handles.get(id).close();
				handles.remove(id);
			}

			return 0;
		});
		machine.lua.setField(-2, "close");
		
		// Открытие хендля для чтения/записи
		machine.lua.pushJavaFunction(args -> {
			File file = getFsFile(args);

			boolean reading = true, binary = false, append = false;
			if (!args.isNoneOrNil(2)){
				String mode = machine.lua.checkString(2);
				reading = mode.contains("r");
				binary = mode.contains("b");
				append = mode.contains("a");
			}
			
			if (file.getParentFile().exists()) {
				if (!reading || file.exists()) {
					machine.lua.pushInteger(
						reading ?
						new ReadHandle(file, binary).id :
						new WriteHandle(file, binary, append).id
					);
					
					return 1;
				}
				else {
					return pushFileNotExists();
				}
			}
			else {
				return pushNilAndReason("parent directory doesn't exists");
			}
		});
		machine.lua.setField(-2, "open");

		// Получение лейбла
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushString(label);

			return 1;
		});
		machine.lua.setField(-2, "getLabel");

		// Установка лейбла
		machine.lua.pushJavaFunction(args -> {
			args.checkString(1);
			
			label = args.toString(1);
			
			machine.lua.pushBoolean(true);
			return 1;
		});
		machine.lua.setField(-2, "setLabel");

		// Таймштамп изменения файла
		machine.lua.pushJavaFunction(args -> {
			playSound();
			
			File file = getFsFile(args);
			if (file.exists()) {
				machine.lua.pushInteger((int) (file.lastModified() / 1000));
				
				return 1;
			}
			else {
				return pushFileNotExists();
			}
		});
		machine.lua.setField(-2, "lastModified");

		// Размер файла
		machine.lua.pushJavaFunction(args -> {
			playSound();

			File file = getFsFile(args);
			if (file.exists()) {
				machine.lua.pushInteger((int) file.length());

				return 1;
			}
			else {
				return pushFileNotExists();
			}
		});
		machine.lua.setField(-2, "size");

		// Создание директорий
		machine.lua.pushJavaFunction(args -> {
			playSound();

			machine.lua.pushBoolean(getFsFile(args).mkdirs());
			return 1;
		});
		machine.lua.setField(-2, "makeDirectory");
		
		// Удоление файла
		machine.lua.pushJavaFunction(args -> {
			playSound();

			File file = getFsFile(args);
			if (file.exists()) {
				machine.lua.pushBoolean(file.delete());
				return 1;
			}
			else {
				return pushFileNotExists();
			}
		});
		machine.lua.setField(-2, "remove");

		// Директория ли
		machine.lua.pushJavaFunction(args -> {
			playSound();
			
			machine.lua.pushBoolean(getFsFile(args).isDirectory());

			return 1;
		});
		machine.lua.setField(-2, "isDirectory");
		
		// Существование файла
		machine.lua.pushJavaFunction(args -> {
			playSound();

			machine.lua.pushBoolean(getFsFile(args).exists());
			
			return 1;
		});
		machine.lua.setField(-2, "exists");
	
		// Список файлов в директории
		machine.lua.pushJavaFunction(args -> {
			playSound();

			File file = getFsFile(args);
			if (file.exists()) {
				if (file.isDirectory()) {
					File[] list = file.listFiles();

					machine.lua.newTable();
					int tableIndex = machine.lua.getTop();
					
					for (int i = 0; i < list.length; i++) {
						machine.lua.pushInteger(i + 1);
						if (list[i].isDirectory())
							machine.lua.pushString(list[i].getName() + "/");
						else
							machine.lua.pushString(list[i].getName());
						machine.lua.setTable(tableIndex);
					}
					
					return 1;
				}
				else {
					return pushNilAndReason("path is not a directory");
				}
			}
			else {
				return pushFileNotExists();
			}
		});
		machine.lua.setField(-2, "list");
	
		// Заюзаное пространство
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(spaceUsed);
			return 1;
		});
		machine.lua.setField(-2, "spaceUsed");
	
		// Кол-во юзабельного пространства
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(spaceTotal);
			return 1;
		});
		machine.lua.setField(-2, "spaceTotal");

		// ))000
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(false);

			return 1;
		});
		machine.lua.setField(-2, "isReadOnly");
	}

	@Override
	public JSONObject toJSONObject() {
		return super.toJSONObject()
			.put("temporary", temporary)
			.put("label", label)
			.put("path", realPath);
	}

	private abstract class Handle {
		public int id;
		public RandomAccessFile randomAccessFile;
		
		public Handle(File file) {
			try {
				randomAccessFile = new RandomAccessFile(file, "rw");
			   
				do {
					id = ThreadLocalRandom.current().nextInt();
				} while(handles.containsKey(id));
				
				handles.put(id, this);
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		public abstract void write(byte[] data);
		public abstract byte[] read(double count);
		
		public void close() {
			try {
				randomAccessFile.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		};
	}

	private class ReadHandle extends Handle {
		public ReadHandle(File file, boolean binary) {
			super(file);
			
//            System.out.println("Reading file: " + file.getPath());
		}

		public byte[] read(double needToRead) {
			try {
				byte[] buffer = new byte[needToRead > randomAccessFile.length() ? (int) randomAccessFile.length() : (int) needToRead];
				int readCount = randomAccessFile.read(buffer);
				if (readCount > 0) {
					byte[] pizda = new byte[readCount];
					for (int i = 0; i < readCount; i++) {
						pizda[i] = buffer[i];
					}
					
					return pizda;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			return new byte[] {};
		}

		public void write(byte[] bytes) {}
	}
	
	private class WriteHandle extends Handle {
		public WriteHandle(File file, boolean binary, boolean append) {
			super(file);

//			System.out.println("Writing file: " + file.getPath());
			
			try {
				if (append)
					randomAccessFile.seek(randomAccessFile.length());
				else
					randomAccessFile.setLength(0);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		public byte[] read(double count) {
			return null;
		}

		public void write(byte[] bytes) {
			try {
				randomAccessFile.write(bytes);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private int pushNilAndReason(String reason) {
		machine.lua.pushNil();
		machine.lua.pushString(reason);

		return 2;
	}
	
	private int pushFileNotExists() {
		return pushNilAndReason("file not exists");
	}

	private int pushHandleNotExists() {
		return pushNilAndReason("handle id doesn't exists");
	}
	
	private void playSound() {
		long current = System.currentTimeMillis();
		if (soundNextPlay < current) {
			Player player = players[soundIndex];
			player.reset();
			player.play();

			soundNextPlay = current + soundNextDelay;
			soundIndex++;
			if (soundIndex >= players.length)
				soundIndex = 0;
		}
	}

	private File getFsFile(String path) {
		return new File(realPath, "/" + path);
	}

	private File getFsFile(LuaState args) {
		return getFsFile(args.checkString(1));
	}
}
