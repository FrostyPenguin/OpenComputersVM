package vm.computer.components;

import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import li.cil.repack.com.naef.jnlua.LuaState;
import org.json.JSONObject;
import vm.computer.Glyph;

public class GPU extends ComponentBase {
    public int
        width,
        height,
        GlyphWIDTHMulWidth,
        GlyphHEIGHTMulHeight,
        GlyphHEIGHTMulWidthMulHeight;

    private ImageView screenImageView;
    private GridPane screenGridPane;
    private int[] buffer;
    private Pixel[][] pixels;
    private PixelWriter pixelWriter;
    private int background, foreground;

    public GPU(LuaState lua, String address, GridPane screenGridPane, ImageView screenImageView) {
        super(lua, address,"gpu");
        
        this.screenGridPane = screenGridPane;
        this.screenImageView = screenImageView;
    }

    @Override
    public void pushProxy() {
        super.pushProxy();

        lua.pushJavaFunction(args -> {
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
        lua.setField(-2, "set");

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            args.checkInteger(2);
            args.checkInteger(3);
            args.checkInteger(4);
            args.checkString(5);
            
            rawFill(
                args.toInteger(1) - 1,
                args.toInteger(2) - 1,
                args.toInteger(3),
                args.toInteger(4),
                args.toString(5).codePointAt(0)
            );

            update();

            return 0;
        });
        lua.setField(-2, "fill");

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);
            args.checkInteger(2);

            rawSetResolution(args.toInteger(1), args.toInteger(2));
            update();

            return 0;
        });
        lua.setField(-2, "setResolution");

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);

            background = 0xFF000000 | args.toInteger(1);

            return 0;
        });
        lua.setField(-2, "setBackground");

        lua.pushJavaFunction(args -> {
            args.checkInteger(1);

            foreground = 0xFF000000 | args.toInteger(1);

            return 0;
        });
        lua.setField(-2, "setForeground");
        
        lua.pushJavaFunction(args -> {
            lua.pushInteger(width);
            lua.pushInteger(height);

            return 2;
        });
        lua.setField(-2, "getResolution");

        lua.pushJavaFunction(args -> {
            lua.pushInteger(background);

            return 1;
        });
        lua.setField(-2, "getBackground");

        lua.pushJavaFunction(args -> {
            lua.pushInteger(foreground);

            return 1;
        });
        lua.setField(-2, "getForeground");
    }

    @Override
    public JSONObject toJSONObject() {
        return super.toJSONObject().put("width", width).put("height", height);
    }

    class Pixel {
        int code, background, foreground;

        Pixel(int background, int foreground, int code) {
            this.background = background;
            this.foreground = foreground;
            this.code = code;
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
        background = 0xFF000000;
        foreground = 0xFFFFFFFF;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = new Pixel(background, foreground, 32);
            }
        }
    }

    public void update() {
        int bufferIndex = 0, glyphIndex;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                glyphIndex = 0;

                for (int j = 0; j < Glyph.HEIGHT; j++) {
                    for (int i = 0; i < Glyph.WIDTH; i++) {
                        buffer[bufferIndex + i] = Glyph.map[pixels[y][x].code].pixels[glyphIndex] ? pixels[y][x].foreground : pixels[y][x].background;

                        glyphIndex++;
                    }

                    bufferIndex += GlyphWIDTHMulWidth;
                }

                bufferIndex += Glyph.WIDTH - GlyphHEIGHTMulWidthMulHeight;
            }

            bufferIndex += GlyphHEIGHTMulWidthMulHeight - GlyphWIDTHMulWidth;
        }

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
    }

    public void rawSet(int x, int y, int code) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            pixels[y][x].background = background;
            pixels[y][x].foreground = foreground;
            pixels[y][x].code = code;
        }
//        else {
//            System.out.println("Invalid position, cyka: " + x + " x " + y);
//        }
    }

    public void rawText(int x, int y, String text) {
        for (int i = 0; i < text.length(); i++) {
            rawSet(x, y, text.codePointAt(i));
            x++;
        }
    }

    public void rawFill(int x, int y, int width, int height, int symbol) {
        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                rawSet(i, j, symbol);
            }
        }
    }

    public void rawError(String text) {
        System.out.println(text);

        String[] lines = text.split("\n");

        background = 0xFF0000FF;
        foreground = 0xFFFFFFFF;

        rawFill(0, 0, width, height, 32);

        int y = height / 2 - lines.length / 2;
        for (int i = 0; i < lines.length; i++) {
            rawText(width / 2 - lines[i].length() / 2, y + i, lines[i]);
        }
    }
}
