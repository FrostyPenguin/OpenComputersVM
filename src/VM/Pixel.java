package VM;

public class Pixel {
    public int code;
    public Color background, foreground;

    public Pixel(Color background, Color foreground, int code) {
        this.background = background;
        this.foreground = foreground;
        this.code = code;
    }
}