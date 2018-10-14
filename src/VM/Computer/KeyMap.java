package VM.Computer;

import javafx.scene.input.KeyCode;

import java.util.HashMap;

public class KeyMap {
    private static HashMap<KeyCode, Integer> map = new HashMap<>();

    public static int get(KeyCode keyCode) {
        if (map.containsKey(keyCode)) {
            return map.get(keyCode);
        }
        else {
            System.out.println("Map keyCode doesn't exists: keyCode name: " + keyCode.getName());
            return 35;
        }
    }

    public static void initialize() {
        map.put(KeyCode.DIGIT1, 2);
        map.put(KeyCode.DIGIT2, 3);
        map.put(KeyCode.DIGIT3, 4);
        map.put(KeyCode.DIGIT4, 5);
        map.put(KeyCode.DIGIT5, 6);
        map.put(KeyCode.DIGIT6, 7);
        map.put(KeyCode.DIGIT7, 8);
        map.put(KeyCode.DIGIT8, 9);
        map.put(KeyCode.DIGIT9, 10);
        map.put(KeyCode.DIGIT0, 11);

        map.put(KeyCode.A, 0);
        map.put(KeyCode.B, 8);
        map.put(KeyCode.C, 6);
        map.put(KeyCode.D, 32);
        map.put(KeyCode.E, 18);
        map.put(KeyCode.F, 33);
        map.put(KeyCode.G, 34);
        map.put(KeyCode.H, 35);
        map.put(KeyCode.I, 23);
        map.put(KeyCode.J, 36);
        map.put(KeyCode.K, 37);
        map.put(KeyCode.L, 38);
        map.put(KeyCode.M, 50);
        map.put(KeyCode.N, 49);
        map.put(KeyCode.O, 24);
        map.put(KeyCode.P, 25);
        map.put(KeyCode.Q, 16);
        map.put(KeyCode.R, 19);
        map.put(KeyCode.S, 31);
        map.put(KeyCode.T, 20);
        map.put(KeyCode.U, 22);
        map.put(KeyCode.V, 47);
        map.put(KeyCode.W, 17);
        map.put(KeyCode.X, 45);
        map.put(KeyCode.Y, 21);
        map.put(KeyCode.Z, 44);
        
        map.put(KeyCode.F1, 59);
        map.put(KeyCode.F2, 60);
        map.put(KeyCode.F3, 61);
        map.put(KeyCode.F4, 62);
        map.put(KeyCode.F5, 63);
        map.put(KeyCode.F6, 64);
        map.put(KeyCode.F7, 65);
        map.put(KeyCode.F8, 66);
        map.put(KeyCode.F9, 67);
        map.put(KeyCode.F10, 68);
        map.put(KeyCode.F11, 87);
        map.put(KeyCode.F12, 88);

        map.put(KeyCode.NUMPAD0, 82);
        map.put(KeyCode.NUMPAD1, 79);
        map.put(KeyCode.NUMPAD2, 80);
        map.put(KeyCode.NUMPAD3, 81);
        map.put(KeyCode.NUMPAD4, 75);
        map.put(KeyCode.NUMPAD5, 76);
        map.put(KeyCode.NUMPAD6, 77);
        map.put(KeyCode.NUMPAD7, 71);
        map.put(KeyCode.NUMPAD8, 72);
        map.put(KeyCode.NUMPAD9, 73);
        map.put(KeyCode.DECIMAL, 83);
        map.put(KeyCode.ADD, 78);
        map.put(KeyCode.SUBTRACT, 74);
        map.put(KeyCode.MULTIPLY, 55);
        map.put(KeyCode.DIVIDE, 181);
        map.put(KeyCode.CLEAR, 69);

        map.put(KeyCode.OPEN_BRACKET, 26);
        map.put(KeyCode.CLOSE_BRACKET, 27);
        map.put(KeyCode.SEMICOLON, 39);
        map.put(KeyCode.QUOTE, 40);
        map.put(KeyCode.ENTER, 28);
        map.put(KeyCode.BACK_SPACE, 14);
        map.put(KeyCode.SPACE, 57);
        map.put(KeyCode.CONTROL, 29);
        map.put(KeyCode.SHIFT, 42);
        map.put(KeyCode.WINDOWS, 219);
        map.put(KeyCode.ALT, 56);
        map.put(KeyCode.CAPS, 58);
        map.put(KeyCode.TAB, 15);
        map.put(KeyCode.ESCAPE, 1);
        map.put(KeyCode.COMMA, 51);
        map.put(KeyCode.PERIOD, 52);
        map.put(KeyCode.SLASH, 53);
        map.put(KeyCode.BACK_QUOTE, 41);
        map.put(KeyCode.BACK_SLASH, 43);
        map.put(KeyCode.INSERT, 210);
        map.put(KeyCode.PAGE_UP, 201);
        map.put(KeyCode.PAGE_DOWN, 209);
        map.put(KeyCode.HOME, 199);
        map.put(KeyCode.DELETE, 211);
        map.put(KeyCode.END, 207);
        map.put(KeyCode.UP, 200);
        map.put(KeyCode.DOWN, 208);
        map.put(KeyCode.LEFT, 203);
        map.put(KeyCode.RIGHT, 205);
        
        // Dupes
        map.put(KeyCode.HELP, 210);
        map.put(KeyCode.COMMAND, 219);
    }
}
