package vm.computer.components;

import vm.computer.ComponentBase;
import vm.computer.Glyph;
import vm.computer.Machine;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;

public class GPU extends ComponentBase {
    public int
        width,
        height,
        GlyphWIDTHMulWidth,
        GlyphHEIGHTMulHeight,
        GlyphHEIGHTMulWidthMulHeight;
    private int[] buffer;
    private Pixel[][] pixels;
    private Machine.ScreenWidget screenWidget;
    private PixelWriter pixelWriter;
    private int background, foreground;
    
    public GPU(Machine.ScreenWidget screenWidget) {
        super("gpu");
        this.screenWidget = screenWidget;

        set("set", new ThreeArgFunction() {
            public LuaValue call(LuaValue x, LuaValue y, LuaValue text) {
                x.checkint();
                y.checkint();
                text.checkjstring();

                int javaX = x.toint() - 1, javaY = y.toint() - 1;
                String javaText = text.tojstring();

                for (int i = 0; i < javaText.length(); i++) {
                    rawSet(javaX, javaY, javaText.codePointAt(i));

                    javaX++;
                }

                update();

                return LuaValue.NIL;
            }
        });

        set("setResolution", new TwoArgFunction() {
            public LuaValue call(LuaValue width, LuaValue height) {
                width.checkint();
                height.checkint();

                rawSetResolution(width.toint(), height.toint());
                update();

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
            public Varargs invoke(Varargs varargs) {
                varargs.checkint(1);
                varargs.checkint(2);
                varargs.checkint(3);
                varargs.checkint(4);
                varargs.checkjstring(5);

                rawFill(
                    varargs.toint(1) - 1,
                    varargs.toint(2) - 1,
                    varargs.toint(3),
                    varargs.toint(4),
                    varargs.tojstring(5).codePointAt(0)
                );

                update();

                return LuaValue.NIL;
            }
        });

        set("setBackground", new OneArgFunction() {
            public LuaValue call(LuaValue color) {
                color.checkint();

                background = 0xFF000000 | color.toint();

                return LuaValue.NIL;
            }
        });

        set("setForeground", new OneArgFunction() {
            public LuaValue call(LuaValue color) {
                color.checkint();

                foreground = 0xFF000000 | color.toint();

                return LuaValue.NIL;
            }
        });

        set("getBackground", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(background);
            }
        });

        set("getForeground", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(foreground);
            }
        });
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
        screenWidget.imageView.setImage(writableImage);

        pixels = new Pixel[height][width];
        buffer = new int[width * height * Glyph.WIDTH * Glyph.HEIGHT];

        flush();
        
        screenWidget.applyScale(100);
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
