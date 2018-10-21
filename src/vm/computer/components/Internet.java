package vm.computer.components;

import org.json.JSONObject;
import vm.computer.Machine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class Internet extends ComponentBase {
	public static final int defaultReadLength = 1024;
	
	public Internet(Machine machine, String address) {
		super(machine, address, "internet");
	}

	@Override
	public void pushProxyFields() {
		super.pushProxyFields();
		
		machine.lua.pushJavaFunction(requestArgs -> {
			try {
				HttpURLConnection connection = (HttpURLConnection) new URL(requestArgs.checkString(1)).openConnection();

				connection.setDoInput(true);
				
				// Подрубаем хедеры
				if (!requestArgs.isNoneOrNil(3) && requestArgs.isTable(3)) {
					requestArgs.toJavaObject(3, Map.class).forEach((key, value) -> {
						System.out.println("Setting header: "+ key.toString() + " : " + value.toString());
						connection.setRequestProperty(key.toString(), value.toString());
					});
				}
				
				// Подрубаем метод запроса
				if (!requestArgs.isNoneOrNil(2)) {
					String postData = requestArgs.checkString(2);

					connection.setRequestMethod("POST");

					connection.setDoOutput(true);
					connection.setReadTimeout(1000);

					OutputStream outputStream = connection.getOutputStream();
					outputStream.write(postData.getBytes());
					outputStream.close();
				}
				else {
					connection.setRequestMethod("GET");
				}

				// Подрубаем читалку
				InputStream inputStream = connection.getInputStream();
				
				machine.lua.newTable();
				
				// Чтение из http request
				machine.lua.pushJavaFunction(readArgs -> {
					try {
//						System.out.println(", available: " + inputStream.available());
						byte[] buffer = new byte[readArgs.isNoneOrNil(1) ? defaultReadLength : (int) Math.min(defaultReadLength, readArgs.checkNumber(1))];
						int readCount = inputStream.read(buffer);
//						System.out.println("Buffer size: " + buffer.length + ", readCount: " + readCount);
						
						if (readCount > 0) {
							byte[] result = new byte[readCount];
							for (int i = 0; i < readCount; i++)
								result[i] = buffer[i];

							machine.lua.pushByteArray(result);
							return 1;
						}
						else{
							machine.lua.pushNil();
							return 1;
						}
					}
					catch (IOException e) {
						return pushIOExcetion();	
					}
				});
				machine.lua.setField(-2, "read");

				// Закрытие http request
				machine.lua.pushJavaFunction(readArgs -> {
					try {
						inputStream.close();
						connection.disconnect();
						
						machine.lua.pushBoolean(true);
						return 1;
					}
					catch (IOException e) {
						e.printStackTrace();
					}

					machine.lua.pushBoolean(false);
					return 1;
				});
				machine.lua.setField(-2, "close");

				// Закрытие http request
				machine.lua.pushJavaFunction(readArgs -> {
					try {
						// Пушим код и текст
						machine.lua.pushInteger(connection.getResponseCode());
						machine.lua.pushString(connection.getResponseMessage());

						// Пушим поля заголовка вида Map<String, List<String>>
						machine.lua.newTable();
						int tableIndex = machine.lua.getTop();
						
						connection.getHeaderFields().forEach((key, list) -> {
//							System.out.println("Map key: " + key);
//							for (int i = 0; i < list.size(); i++) {
//								System.out.println("Map List: " + i + ", " + list.get(i));
//							}
							
							// В душе не ебу, с хуев ли тут может быть нулл, но бывает. Видимо, это тонкая задумка разрабов
							if (key != null) {
								// Ключ мапы
								machine.lua.pushString(key);

								// Валуя мапы (тот самый List<String>)
								machine.lua.newTable();
								int listIndex = machine.lua.getTop();

								for (int i = 0; i < list.size(); i++) {
									machine.lua.pushInteger(i + 1);
									machine.lua.pushString(list.get(i));
									//Впездываем ключи листа
									machine.lua.setTable(listIndex);
								}

								// Впездываем валую
								machine.lua.setTable(tableIndex);	
							}
						});
						
						return 3;
					}
					catch (IOException e) {
						e.printStackTrace();
						return pushIOExcetion();
					}
				});
				machine.lua.setField(-2, "response");
				
				return 1;
			}
//			catch (MalformedURLException e) {
//				machine.lua.pushNil();
//				machine.lua.pushString("malformed URL");
//				
//				return 2;
//			}
//			catch (IOException e) {
//				return pushIOExcetion();
//			}
			catch (Exception e) {
				e.printStackTrace();
				return 0;
			}
		});
		machine.lua.setField(-2, "request");
	}

	private int pushIOExcetion() {
		machine.lua.pushNil();
		machine.lua.pushString("IOException blyad");
		return 2;
	}
	
	@Override
	public JSONObject toJSONObject() {
		return super.toJSONObject();
	}
}
