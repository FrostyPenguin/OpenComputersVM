package emulator.computer;

public class Key {
    public int unicode, ascii, upper;
    
    public Key(int unicode, int ascii, int upper) {
        this.unicode = unicode;
        this.ascii = ascii;
        this.upper = upper;
    }

    public Key(int unicode, int ascii) {
        this(unicode, ascii, unicode);
    }
}
