package emulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Glyph {
    public static final int WIDTH = 8, HEIGHT = 16;
    public static Glyph[] map = new Glyph[119639];
    
    public int width;
    public boolean[] pixels;

    public Glyph(int width) {
        this.width = width;
        this.pixels = new boolean[this.width * HEIGHT];
    }

    public static void loadMap() {
        try {
            for (int i = 0; i < map.length; i++) {
                map[i] = new Glyph(8);
            }
            
            BufferedReader br = new BufferedReader(new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("emulator/font.hex")));
            
            String line;
            while ((line = br.readLine()) != null) {
                int indexOfDots = line.indexOf(58);
                int code = Integer.parseInt(line.substring(0, indexOfDots), 16);
                String pixelData = line.substring(indexOfDots + 1);
                
                Glyph glyph = new Glyph(pixelData.length() / 2 / HEIGHT * WIDTH);
                
                int index = 7;
                for (int i = 0; i < pixelData.length(); i += 2) {
                    int glyphByte = Integer.parseInt(pixelData.substring(i, i + 2), 16);
                    for (int j = 0; j < 8; j++) {
                        glyph.pixels[index - j] = (glyphByte & 1) == 1;
                        glyphByte >>= 1;
                    }
                    
                    index += 8;
                }

                map[code] = glyph;
                
//                if (code == 1055) {
//                    System.out.println("line: " + line + ", code: " + code + ", width: " + glyph.width);
//
//                    index = 0;
//                    for (int y = 0; y < HEIGHT; y++) {
//                        for (int x = 0; x < glyph.width; x++) {
//                            System.out.print((glyph.pixels[index] ? 1 : 0) + " ");
//                            index++;
//                        }
//                        System.out.print("\n");
//                    }
//                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
