package VM;

public class Pixel {
    public int code;
    public int background, foreground;

    public Pixel(int background, int foreground, int code) {
        this.background = background;
        this.foreground = foreground;
        this.code = code;
    }
}