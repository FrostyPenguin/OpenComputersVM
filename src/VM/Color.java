package VM;

public class Color {
    public byte red, green, blue;
    
    public static Color
        BLACK = new Color(0x000000),
        RED = new Color(0xFF0000),
        GREEN = new Color(0x00FF00),
        BLUE = new Color(0x0000FF),
        WHITE = new Color(0xFFFFFF);
    
    public Color(int rgb) {
        red = (byte) ((rgb >> 16) & 0xFF);
        green = (byte) ((rgb >> 8) & 0xFF);
        blue = (byte) (rgb & 0xFF);
    }
    
    public int toInteger() {
        return (red << 16) | (green << 8) | blue;
    }
}
