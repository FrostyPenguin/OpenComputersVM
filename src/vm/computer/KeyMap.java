package vm.computer;

import javafx.scene.input.KeyCode;

import java.util.HashMap;

public class KeyMap {
    public static class OCKey {
        int ascii, unicode;

        public OCKey(int ascii) {
            this.ascii = ascii;
            this.unicode = 0;
        }

        public OCKey(int unicode,int ascii) {
            this.unicode = unicode;
            this.ascii = ascii;
        }
    }
    
	private static final HashMap<KeyCode, OCKey> map = new HashMap<>();

	public static OCKey get(KeyCode keyCode) {
		if (map.containsKey(keyCode)) {
			return map.get(keyCode);
		}
		else {
			System.out.println("Map keyCode doesn't exists: keyCode name: " + keyCode.getName());
			return map.get(KeyCode.AT);
		}
	}

	public static void initialize() {
        map.put(KeyCode.DIGIT1, new OCKey(2));
        map.put(KeyCode.DIGIT2, new OCKey(3));
        map.put(KeyCode.DIGIT3, new OCKey(4));
        map.put(KeyCode.DIGIT4, new OCKey(5));
        map.put(KeyCode.DIGIT5, new OCKey(6));
        map.put(KeyCode.DIGIT6, new OCKey(7));
        map.put(KeyCode.DIGIT7, new OCKey(8));
        map.put(KeyCode.DIGIT8, new OCKey(9));
        map.put(KeyCode.DIGIT9, new OCKey(10));
        map.put(KeyCode.DIGIT0, new OCKey(11));

        map.put(KeyCode.A, new OCKey(30));
        map.put(KeyCode.B, new OCKey(8));
        map.put(KeyCode.C, new OCKey(6));
        map.put(KeyCode.D, new OCKey(32));
        map.put(KeyCode.E, new OCKey(18));
        map.put(KeyCode.F, new OCKey(33));
        map.put(KeyCode.G, new OCKey(34));
        map.put(KeyCode.H, new OCKey(35));
        map.put(KeyCode.I, new OCKey(23));
        map.put(KeyCode.J, new OCKey(36));
        map.put(KeyCode.K, new OCKey(37));
        map.put(KeyCode.L, new OCKey(38));
        map.put(KeyCode.M, new OCKey(50));
        map.put(KeyCode.N, new OCKey(49));
        map.put(KeyCode.O, new OCKey(24));
        map.put(KeyCode.P, new OCKey(25));
        map.put(KeyCode.Q, new OCKey(16));
        map.put(KeyCode.R, new OCKey(19));
        map.put(KeyCode.S, new OCKey(31));
        map.put(KeyCode.T, new OCKey(20));
        map.put(KeyCode.U, new OCKey(22));
        map.put(KeyCode.V, new OCKey(47));
        map.put(KeyCode.W, new OCKey(17));
        map.put(KeyCode.X, new OCKey(45));
        map.put(KeyCode.Y, new OCKey(21));
        map.put(KeyCode.Z, new OCKey(44));

        map.put(KeyCode.F1, new OCKey(59));
        map.put(KeyCode.F2, new OCKey(60));
        map.put(KeyCode.F3, new OCKey(61));
        map.put(KeyCode.F4, new OCKey(62));
        map.put(KeyCode.F5, new OCKey(63));
        map.put(KeyCode.F6, new OCKey(64));
        map.put(KeyCode.F7, new OCKey(65));
        map.put(KeyCode.F8, new OCKey(66));
        map.put(KeyCode.F9, new OCKey(67));
        map.put(KeyCode.F10, new OCKey(68));
        map.put(KeyCode.F11, new OCKey(87));
        map.put(KeyCode.F12, new OCKey(88));

        map.put(KeyCode.NUMPAD0, new OCKey(82));
        map.put(KeyCode.NUMPAD1, new OCKey(79));
        map.put(KeyCode.NUMPAD2, new OCKey(80));
        map.put(KeyCode.NUMPAD3, new OCKey(81));
        map.put(KeyCode.NUMPAD4, new OCKey(75));
        map.put(KeyCode.NUMPAD5, new OCKey(76));
        map.put(KeyCode.NUMPAD6, new OCKey(77));
        map.put(KeyCode.NUMPAD7, new OCKey(71));
        map.put(KeyCode.NUMPAD8, new OCKey(72));
        map.put(KeyCode.NUMPAD9, new OCKey(73));
        map.put(KeyCode.DECIMAL, new OCKey(83));
        map.put(KeyCode.ADD, new OCKey(78));
        map.put(KeyCode.SUBTRACT, new OCKey(74));
        map.put(KeyCode.MULTIPLY, new OCKey(55));
        map.put(KeyCode.DIVIDE, new OCKey(181));
        map.put(KeyCode.CLEAR, new OCKey(69));

        map.put(KeyCode.OPEN_BRACKET, new OCKey(26));
        map.put(KeyCode.CLOSE_BRACKET, new OCKey(27));
        map.put(KeyCode.SEMICOLON, new OCKey(39));
        map.put(KeyCode.QUOTE, new OCKey(40));
        map.put(KeyCode.ENTER, new OCKey(28));
        map.put(KeyCode.BACK_SPACE, new OCKey(14));
        map.put(KeyCode.SPACE, new OCKey(57));
        map.put(KeyCode.CONTROL, new OCKey(0, 29));
        map.put(KeyCode.SHIFT, new OCKey(0, 42));
        map.put(KeyCode.WINDOWS, new OCKey(0, 219));
        map.put(KeyCode.ALT, new OCKey(0, 56));
        map.put(KeyCode.CAPS, new OCKey(0, 58));
        map.put(KeyCode.TAB, new OCKey(15));
        map.put(KeyCode.ESCAPE, new OCKey(1));
        map.put(KeyCode.COMMA, new OCKey(51));
        map.put(KeyCode.PERIOD, new OCKey(52));
        map.put(KeyCode.SLASH, new OCKey(53));
        map.put(KeyCode.BACK_QUOTE, new OCKey(41));
        map.put(KeyCode.BACK_SLASH, new OCKey(43));
        map.put(KeyCode.INSERT, new OCKey(210));
        map.put(KeyCode.PAGE_UP, new OCKey(201));
        map.put(KeyCode.PAGE_DOWN, new OCKey(209));
        map.put(KeyCode.HOME, new OCKey(199));
        map.put(KeyCode.DELETE, new OCKey(211));
        map.put(KeyCode.END, new OCKey(207));
        map.put(KeyCode.UP, new OCKey(200));
        map.put(KeyCode.DOWN, new OCKey(208));
        map.put(KeyCode.LEFT, new OCKey(203));
        map.put(KeyCode.RIGHT, new OCKey(205));

        // Dupes
        map.put(KeyCode.HELP, new OCKey(210));
        map.put(KeyCode.COMMAND, new OCKey(219));
	}
}
