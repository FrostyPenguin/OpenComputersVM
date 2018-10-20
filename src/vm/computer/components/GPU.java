package vm.computer.components;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import li.cil.repack.com.naef.jnlua.LuaRuntimeException;
import org.json.JSONObject;
import vm.computer.Glyph;
import vm.computer.LuaUtils;
import vm.computer.Machine;

public class GPU extends ComponentBase {
	public static class Color {
		static final Color
			BLACK = new Color(0x000000),
			WHITE = new Color(0xFFFFFF),
			BLUE = new Color(0x0000FF);
		
		int value;
		boolean isPaletteIndex;

		Color(int value) {
			this(value, false);
		}

		Color(int value, boolean isPaletteIndex) {
			this.value = 0xFF000000 | value;
			this.isPaletteIndex = isPaletteIndex;
		}
	}
	
	private class Pixel {
		Color background, foreground;
		int code;

		Pixel(Color background, Color foreground, int code) {
			this.background = background;
			this.foreground = foreground;
			this.code = code;
		}
	}

	// Поток-дрочер, обновляющий пиксельную инфу по запросу
	public class UpdaterThread extends Thread {
		// По сути лимитировщик FPS
		public int waitDelay = 16;
		public int[] buffer;
		
		private boolean needUpdate = false;

		public void update() {
			update(0, 0, width - 1, height - 1);
		}

		public void update(int fromX, int fromY, int toX, int toY) {
			synchronized (this) {
				int bufferIndex = fromY * (Glyph.WIDTH * Glyph.HEIGHT * width) + fromX * (Glyph.WIDTH),
					glyphIndex, background, foreground;

				for (int y = fromY; y <= toY; y++) {
					for (int x = fromX; x <= toX; x++) {
						background = pixels[y][x].background.isPaletteIndex ? palette[pixels[y][x].background.value] : pixels[y][x].background.value;
						foreground = pixels[y][x].foreground.isPaletteIndex ? palette[pixels[y][x].foreground.value] : pixels[y][x].foreground.value;

						glyphIndex = 0;
						for (int j = 0; j < Glyph.HEIGHT; j++) {
							for (int i = 0; i < Glyph.WIDTH; i++) {
								buffer[bufferIndex + i] = Glyph.map[pixels[y][x].code].microPixels[glyphIndex] ? foreground : background;

								glyphIndex++;
							}

							bufferIndex += GlyphWIDTHMulWidth;
						}

						bufferIndex += Glyph.WIDTH - GlyphHEIGHTMulWidthMulHeight;
					}

					bufferIndex += GlyphHEIGHTMulWidthMulHeight - GlyphWIDTHMulWidth;
				}

				needUpdate = true;
			}
		}

		@Override
		public void run() {
			synchronized (this) {
				while (true) {
					try {
						wait(waitDelay);

						if (needUpdate) {
							pixelWriter.setPixels(
								0,
								0,
								GlyphWIDTHMulWidth,
								GlyphHEIGHTMulHeight,
								PixelFormat.getIntArgbInstance(),
								buffer,
								0,
								GlyphWIDTHMulWidth
							);

							needUpdate = false;
						}
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
					catch (ThreadDeath e) {
						break;
					}
				}
			}
		}
	}

	public int
		width,
		height,
		GlyphWIDTHMulWidth,
		GlyphHEIGHTMulHeight,
		GlyphHEIGHTMulWidthMulHeight;
	public UpdaterThread updaterThread = new UpdaterThread();

	private Color background, foreground;
	private int[] palette = new int[16];
	private Pixel[][] pixels;
	private PixelWriter pixelWriter;

	public GPU(Machine machine, String address) {
		super(machine, address,"gpu");

		updaterThread.start();
	}

	@Override
	public void pushProxy() {
		super.pushProxy();

		machine.lua.pushJavaFunction(args -> {
			int
				x = args.checkInteger(1) - 1,
				y = args.checkInteger(2) - 1;

			checkCoordinatesAndThrow(x, y);
			
			// Пушим символ, цвет текста и цвет фона
			machine.lua.pushString(new String(Character.toChars(pixels[y][x].code)));
			machine.lua.pushInteger(pixels[y][x].foreground.isPaletteIndex ? palette[pixels[y][x].foreground.value] : pixels[y][x].foreground.value);
			machine.lua.pushInteger(pixels[y][x].background.isPaletteIndex ? palette[pixels[y][x].background.value] :
				pixels[y][x].background.value);
			
			// Если цвета из палитры, то пушим еще и индексы палитры, либо нил для каждого из цветов
			if (pixels[y][x].foreground.isPaletteIndex)
				machine.lua.pushInteger(pixels[y][x].foreground.value);
			else
				machine.lua.pushNil();
			
			if (pixels[y][x].background.isPaletteIndex)
				machine.lua.pushInteger(pixels[y][x].background.value);
			else
				machine.lua.pushNil();
			
			return 5;
		});
		machine.lua.setField(-2, "get");
		
		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushBoolean(rawCopy(
				args.checkInteger(1) - 1,
				args.checkInteger(2) - 1,
				args.checkInteger(3),
				args.checkInteger(4),
				args.checkInteger(5),
				args.checkInteger(6)
			));

			return 1;
		});
		machine.lua.setField(-2, "copy");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(160);
			machine.lua.pushInteger(50);

			return 2;
		});
		machine.lua.setField(-2, "maxResolution");

		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);
			args.checkInteger(2);

			rawSetResolution(args.toInteger(1), args.toInteger(2));
			updaterThread.update();

			return 0;
		});
		machine.lua.setField(-2, "setResolution");

		machine.lua.pushJavaFunction(args -> {
			int
				x = args.checkInteger(1) - 1,
				y = args.checkInteger(2) - 1;
			String text = args.checkString(3);
			int limit = Math.min(text.length(), width - x);
			
			for (int i = 0; i < limit; i++) {
				rawSet(x + i, y, text.codePointAt(i));
			}
			
//			updateEntireBuffer();
			updaterThread.update(x, y, x + limit - 1, y);
//			updateImageViewFromBuffer();

			return 0;
		});
		machine.lua.setField(-2, "set");

		machine.lua.pushJavaFunction(args -> {
			int x = args.checkInteger(1) - 1,
				y = args.checkInteger(2) - 1;
			
			rawFill(
				x,
				y,
				x + args.checkInteger(3) - 1,
				y + args.checkInteger(4) - 1,
				args.checkString(5).codePointAt(0)
			);

			return 0;
		});
		machine.lua.setField(-2, "fill");

		machine.lua.pushJavaFunction(args -> {
			Color oldColor = background;
			background = new Color(args.checkInteger(1), !args.isNoneOrNil(2) && args.checkBoolean(2));
            
			return setColorPush(oldColor);
		});
		machine.lua.setField(-2, "setBackground");

		machine.lua.pushJavaFunction(args -> {
			Color oldColor = foreground;
			foreground = new Color(args.checkInteger(1), !args.isNoneOrNil(2) && args.checkBoolean(2));

			return setColorPush(oldColor);
		});
		machine.lua.setField(-2, "setForeground");

		machine.lua.pushJavaFunction(args -> getColorPush(background));
		machine.lua.setField(-2, "getBackground");

		machine.lua.pushJavaFunction(args -> getColorPush(foreground));
		machine.lua.setField(-2, "getForeground");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(width);
			machine.lua.pushInteger(height);

			return 2;
		});
		machine.lua.setField(-2, "getResolution");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(width);
			machine.lua.pushInteger(height);

			return 2;
		});
		machine.lua.setField(-2, "getViewport");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushString(machine.screenComponent.address);
			return 1;
		});
		machine.lua.setField(-2, "getScreen");

		machine.lua.pushJavaFunction(args -> {
			machine.lua.pushInteger(8);
			return 1;
		});
		machine.lua.setField(-2, "getDepth");

		LuaUtils.pushBooleanFunction(machine.lua, "setViewport", true);
		LuaUtils.pushBooleanFunction(machine.lua, "setDepth", true);
		LuaUtils.pushBooleanFunction(machine.lua, "bind", true);
	}

	@Override
	public JSONObject toJSONObject() {
		return super.toJSONObject().put("width", width).put("height", height);
	}

	// Возвращение инфы о текущем цвете
    private int getColorPush(Color color) {
        if (color.isPaletteIndex) {
            machine.lua.pushInteger(color.value);
            machine.lua.pushBoolean(true);
            return 2;
        }
        
        machine.lua.pushInteger(color.value);
        return 1;
    }
	
    // Возвращение инфы о старом цвете до операции gpu.set
	private int setColorPush(Color color) {
		if (color.isPaletteIndex) {
			machine.lua.pushInteger(palette[color.value]);
			machine.lua.pushInteger(color.value);
			return 2;
		}
		
        machine.lua.pushInteger(color.value);
        return 1;
	}
	
	private int checkPaletteIndex(int index) {
		if (index < 0 || index > 15)
			throw new LuaRuntimeException("palette index is out of range [0; 15]");
		
		return index;
	}
	
	public boolean rawCopy(int x, int y, int w, int h, int tx, int ty) {
		if (x >= 0 && y >= 0 && x + w < width && y + h < height) {
//			Pixel[][] buffer = new Pixel[h][w];
//			
//			// Копипиздим
//			int newX = 0, newY = 0;
//			for (int j = y; j < y + h; j++) {
//				for (int i = x; i < x + w; i++) {
//					buffer[newY][newX] = pixels[j][i];
//					
//					newX++;
//				}
//				
//				newX = 0;
//				newY++;
//			}
//			
//			// Вставляем страпон
//			newX = 0;
//			newY = 0;
//			for (int j = y + ty; j <= y + ty + h; j++) {
//				for (int i = x + tx; i <= x + tx + w; i++) {
//					checkCoordinatesAndThrow(i, j);
//					pixels[j][i] = buffer[newY][newX];
//
//					newX++;
//				}
//				
//				newX = 0;
//				newY++;
//			}
//			
//			updateEntireBuffer(); 
			
			return true;
		}
		else {
			return false;
		}
	}
	
	public void rawSetResolution(int newWidth, int newHeight) {
		width = newWidth;
		height = newHeight;
		GlyphWIDTHMulWidth = width * Glyph.WIDTH;
		GlyphHEIGHTMulHeight = height * Glyph.HEIGHT;
		GlyphHEIGHTMulWidthMulHeight = GlyphWIDTHMulWidth * Glyph.HEIGHT;

		WritableImage writableImage = new WritableImage(GlyphWIDTHMulWidth, GlyphHEIGHTMulHeight);
		pixelWriter = writableImage.getPixelWriter();
		machine.screenImageView.setImage(writableImage);
		
		pixels = new Pixel[height][width];
		updaterThread.buffer = new int[width * height * Glyph.WIDTH * Glyph.HEIGHT];
		
		flush();
	}

	public void flush() {
		background = Color.BLACK;
		foreground = Color.WHITE;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				pixels[y][x] = new Pixel(background, foreground, 32);
			}
		}

		for (int i = 0; i < palette.length; i++) {
			palette[i] = 0xFF000000;
		}
	}

	private boolean checkCoordinates(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}
	
	private void checkCoordinatesAndThrow(int x, int y) {
		if (!checkCoordinates(x, y))
			throw new LuaRuntimeException("screen coordinate is out of range [" + width + "; " + height + "]: " + x + 
				", " + y);
	}
	
	public void rawSet(int x, int y, int code) {
		pixels[y][x].background = background;
		pixels[y][x].foreground = foreground;
		pixels[y][x].code = code;
	}

	public void rawText(int x, int y, String text) {
		for (int i = 0; i < text.length(); i++) {
			if (checkCoordinates(x, y))
				rawSet(x, y, text.codePointAt(i));
			
			x++;
		}
	}

	public void rawFill(int fromX, int fromY, int toX, int toY, int s) {
		fromX = Math.max(0, fromX);
		fromY = Math.max(0, fromY);
		toX = Math.min(width - 1, toX);
		toY = Math.min(height - 1, toY);
		
		for (int j = fromY; j <= toY; j++)
			for (int i = fromX; i <= toX; i++)
				rawSet(i, j, s);
	
		updaterThread.update(fromX, fromY, toX, toY);
	}

	public void rawError(String text) {
		System.out.println(text);

		String[] lines = text.split("\n");

		background = Color.BLUE;
		foreground = Color.WHITE;

		rawFill(0, 0, width - 1, height - 1, 32);

		int y = height / 2 - lines.length / 2;
		for (int i = 0; i < lines.length; i++) {
			rawText(width / 2 - lines[i].length() / 2, y + i, lines[i]);
		}
	}
}
