package emulator.computer.components;

import emulator.Color;
import emulator.Glyph;
import emulator.Pixel;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;

public class GPU extends ComponentBase {
    private int
        width,
        height,
        GlyphWIDTHMul3,
        GlyphWIDTHMulWidth,
        GlyphHEIGHTMulHeight;
    private byte[] buffer;
    private Pixel[][] pixels;
    private ImageView imageView;
    private PixelWriter pixelWriter;
    private Color
        background = Color.BLACK,
        foreground = Color.WHITE;
    
    private void rawSetResolution(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;

        GlyphWIDTHMul3 = Glyph.WIDTH * 3;
        GlyphWIDTHMulWidth = Glyph.WIDTH * width;
        GlyphHEIGHTMulHeight = Glyph.HEIGHT * height;

        WritableImage writableImage = new WritableImage(GlyphWIDTHMulWidth, GlyphHEIGHTMulHeight);
        pixelWriter = writableImage.getPixelWriter();
        imageView.setImage(writableImage);

        pixels = new Pixel[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = new Pixel(Color.BLACK, Color.WHITE, 32);
            }
        }

        buffer = new byte[width * height * GlyphWIDTHMul3 * Glyph.HEIGHT];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) 0;
        }

        imageView.setFitWidth(GlyphWIDTHMulWidth);
        imageView.setFitHeight(GlyphHEIGHTMulHeight);
    }
    
    private int getIndex(int x, int y) {
        return y * width * GlyphWIDTHMul3 * Glyph.HEIGHT + x * GlyphWIDTHMul3;
    }

    private void update() {
        int lineStep = width * GlyphWIDTHMul3;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int bufferIndex = getIndex(x, y);
                int glyphIndex = 0;

                for (int j = 0; j < Glyph.HEIGHT; j++) {
                    for (int i = 0; i < GlyphWIDTHMul3; i += 3) {
                        if (Glyph.map[pixels[y][x].code].pixels[glyphIndex]) {
                            buffer[bufferIndex + i] = pixels[y][x].foreground.red;
                            buffer[bufferIndex + i + 1] = pixels[y][x].foreground.green;
                            buffer[bufferIndex + i + 2] = pixels[y][x].foreground.blue;
                        }
                        else {
                            buffer[bufferIndex + i] = pixels[y][x].background.red;
                            buffer[bufferIndex + i + 1] = pixels[y][x].background.green;
                            buffer[bufferIndex + i + 2] = pixels[y][x].background.blue;
                        }

                        glyphIndex++;
                    }

                    bufferIndex += lineStep;
                }
            }
        }

        pixelWriter.setPixels(
            0, 
            0,
            GlyphWIDTHMulWidth,
            GlyphHEIGHTMulHeight,
            PixelFormat.getByteRgbInstance(),
            buffer, 
            0, 
            GlyphWIDTHMulWidth * 3
        );
    }

    private void rawSet(int x, int y, Color background, Color foreground, int code) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            pixels[y][x].background = background;
            pixels[y][x].foreground = foreground;
            pixels[y][x].code = code;
        }
        else {
            System.out.println("Invalid position, cyka: " + x + " x " + y);
        }
    }
    
    private void rawFill(int x, int y, int width, int height, Color background, Color foreground, int symbol) {
        for (int j = y; j < y + height; j++) {
            for (int i = x; i < x + width; i++) {
                rawSet(i, j, background, foreground, symbol);
            }
        }
    }
    
    public GPU(ImageView imageView, int width, int height) {
        super("gpu");
        this.imageView = imageView;
        
        set("set", new ThreeArgFunction() {
            public LuaValue call(LuaValue x, LuaValue y, LuaValue text) {
                x.checknumber();
                y.checknumber();
                text.checkjstring();
                
                int javaX = x.toint() - 1, javaY = y.toint() - 1;
                String javaText = text.tojstring();

                for (int i = 0; i < text.length(); i++) {
                    pixels[javaY][javaX].code = javaText.codePointAt(i);
                    pixels[javaY][javaX].foreground = foreground;

                    javaX++;
                }
                
                update();

                return LuaValue.NIL;
            }
        });
        
        set("setResolution", new TwoArgFunction() {
            public LuaValue call(LuaValue width, LuaValue height) {
                width.checknumber();
                height.checknumber();
                
                rawSetResolution(width.toint(), height.toint());
                
                return LuaValue.NIL;
            }
        });

        set("getResolution", new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                return LuaValue.varargsOf(new LuaValue[] {
                    LuaValue.valueOf(width),
                    LuaValue.valueOf(height)
                });
            }
        });

        set("fill", new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                args.arg(1).checknumber();
                args.arg(2).checknumber();
                args.arg(3).checknumber();
                args.arg(4).checknumber();
                args.arg(5).checkjstring();
                
                rawFill(
                    args.arg(1).toint() - 1,
                    args.arg(2).toint() - 1,
                    args.arg(3).toint(),
                    args.arg(4).toint(),
                    background,
                    foreground,
                    args.arg(5).tojstring().codePointAt(0)
                );
                
                update();
                
                return LuaValue.NIL;
            }
        });
        
        set("setBackground", new OneArgFunction() {
            public LuaValue call(LuaValue color) {
                color.checknumber();
                
                background = new Color(color.toint());

                return LuaValue.NIL;
            }
        });

        set("setForeground", new OneArgFunction() {
            public LuaValue call(LuaValue color) {
                color.checknumber();
                
                foreground = new Color(color.toint());
                
                return LuaValue.NIL;
            }
        });

        set("getBackground", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(background.toInteger());
            }
        });

        set("getForeground", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(foreground.toInteger());
            }
        });

        rawSetResolution(width, height);
        update();
    }
}
