package vm.computer.components;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import li.cil.repack.com.naef.jnlua.LuaRuntimeException;
import org.json.JSONObject;
import vm.computer.Glyph;
import vm.computer.LuaUtils;
import vm.computer.Machine;

public class GPU extends ComponentBase {
	static class Color {
		public static final Color
			BLACK = new Color(0x000000),
			WHITE = new Color(0xFFFFFF),
			BLUE = new Color(0x0000FF);
		
		public int value;
		public boolean isPaletteIndex;

		public Color(int value) {
			this(value, false);
		}

		public Color(int value, boolean isPaletteIndex) {
			this.value = 0xFF000000 | value;
			this.isPaletteIndex = isPaletteIndex;
		}
	}
	
	class Pixel {
		Color background, foreground;
		int code;

		Pixel(Color background, Color foreground, int code) {
			this.background = background;
			this.foreground = foreground;
			this.code = code;
		}
	}

	public int
		width,
		height,
		GlyphWIDTHMulWidth,
		GlyphHEIGHTMulHeight,
		GlyphHEIGHTMulWidthMulHeight;

	private Color background, foreground;
	private int[] palette = new int[16];
	private int[] buffer;
	private Pixel[][] pixels;
	private ImageView screenImageView;
	private GridPane screenGridPane;
	private PixelWriter pixelWriter;
	
	public GPU(Machine machine, String address, GridPane screenGridPane, ImageView screenImageView) {
		super(machine, address,"gpu");
		
		this.screenGridPane = screenGridPane;
		this.screenImageView = screenImageView;
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
			update();

			return 0;
		});
		machine.lua.setField(-2, "setResolution");

		machine.lua.pushJavaFunction(args -> {
			args.checkInteger(1);
			args.checkInteger(2);
			args.checkString(3);

			int
				x = args.toInteger(1) - 1,
				y = args.toInteger(2) - 1;
			String text = args.toString(3);

			for (int i = 0; i < text.length(); i++) {
				rawSet(x, y, text.codePointAt(i));
				x++;
			}

			update();

			return 0;
		});
		machine.lua.setField(-2, "set");

		machine.lua.pushJavaFunction(args -> {
			rawFill(
				args.checkInteger(1) - 1,
				args.checkInteger(2) - 1,
				args.checkInteger(3),
				args.checkInteger(4),
				args.checkString(5).codePointAt(0)
			);

			update();

			return 0;
		});
		machine.lua.setField(-2, "fill");

		machine.lua.pushJavaFunction(args -> {
			Color oldColor = background;
			background = new Color(args.checkInteger(1), args.isNoneOrNil(2) ? false : args.checkBoolean(2));
            
			return setColorPush(oldColor);
		});
		machine.lua.setField(-2, "setBackground");

		machine.lua.pushJavaFunction(args -> {
			Color oldColor = foreground;
			foreground = new Color(args.checkInteger(1), args.isNoneOrNil(2) ? false : args.checkBoolean(2));

			return setColorPush(oldColor);
		});
		machine.lua.setField(-2, "setForeground");

		machine.lua.pushJavaFunction(args -> {
			return getColorPush(background);
		});
		machine.lua.setField(-2, "getBackground");

		machine.lua.pushJavaFunction(args -> {
            return getColorPush(foreground);
		});
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
			Pixel[][] buffer = new Pixel[h][w];
			
			// Копипиздим
			int newX = 0, newY = 0;
			for (int j = y; j < y + h; j++) {
				for (int i = x; i < x + w; i++) {
					buffer[newY][newX] = pixels[j][i];
					
					newX++;
				}
				
				newX = 0;
				newY++;
			}
			
			// Вставляем страпон
			newX = 0;
			newY = 0;
			for (int j = y + ty; j <= y + ty + h; j++) {
				for (int i = x + tx; i <= x + tx + w; i++) {
					checkCoordinatesAndThrow(i, j);
					pixels[j][i] = buffer[newY][newX];

					newX++;
				}
				
				newX = 0;
				newY++;
			}
			
			update(); 
			
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
		screenImageView.setImage(writableImage);
		
		pixels = new Pixel[height][width];
		buffer = new int[width * height * Glyph.WIDTH * Glyph.HEIGHT];

		flush();
	}
	
	public void updateScreenImageSize() {
		double w = width * Glyph.WIDTH;
		double h = height * Glyph.HEIGHT;
		
		if (w >= h && w > screenGridPane.getWidth()) {
			screenImageView.setFitWidth(screenGridPane.getWidth());
			screenImageView.setFitHeight(screenGridPane.getHeight() * w / h);
		}
		else if (w < h && h > screenGridPane.getHeight()) {
			screenImageView.setFitWidth(screenGridPane.getWidth() / w / h);
			screenImageView.setFitHeight(screenGridPane.getHeight());
		}
		else {
			screenImageView.setFitWidth(w);
			screenImageView.setFitHeight(h);
		}
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

	public void update() {
		int bufferIndex = 0, glyphIndex, background, foreground;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				glyphIndex = 0;
				background = pixels[y][x].background.isPaletteIndex ? palette[pixels[y][x].background.value] : pixels[y][x].background.value;
				foreground = pixels[y][x].foreground.isPaletteIndex ? palette[pixels[y][x].foreground.value] : pixels[y][x].foreground.value;
				
				for (int j = 0; j < Glyph.HEIGHT; j++) {
					for (int i = 0; i < Glyph.WIDTH; i++) {
						buffer[bufferIndex + i] = Glyph.map[pixels[y][x].code].pixels[glyphIndex] ? foreground : background;

						glyphIndex++;
					}

					bufferIndex += GlyphWIDTHMulWidth;
				}

				bufferIndex += Glyph.WIDTH - GlyphHEIGHTMulWidthMulHeight;
			}

			bufferIndex += GlyphHEIGHTMulWidthMulHeight - GlyphWIDTHMulWidth;
		}

		Platform.runLater(() -> {
			pixelWriter.setPixels(
				0,
				0,
				GlyphWIDTHMulWidth,
				GlyphHEIGHTMulHeight,
				PixelFormat.getIntArgbPreInstance(),
				buffer,
				0,
				GlyphWIDTHMulWidth
			);
		});
	}

	public boolean checkCoordinates(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}
	
	public void checkCoordinatesAndThrow(int x, int y) {
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

	public void rawFill(int x, int y, int w, int h, int s) {
		for (int j = y; j < y + h; j++)
			for (int i = x; i < x + w; i++)
				if (checkCoordinates(i, j))
					rawSet(i, j, s);
	}

	public void rawError(String text) {
		System.out.println(text);

		String[] lines = text.split("\n");

		background = Color.BLUE;
		foreground = Color.WHITE;

		rawFill(0, 0, width, height, 32);

		int y = height / 2 - lines.length / 2;
		for (int i = 0; i < lines.length; i++) {
			rawText(width / 2 - lines[i].length() / 2, y + i, lines[i]);
		}
	}
}
