package emulator.computer;

import javafx.scene.input.KeyCode;

import java.util.HashMap;

public class KeyMap {
    private static HashMap<KeyCode, Key> map = new HashMap<>();

    public static Key get(KeyCode keyCode) {
        return map.containsKey(keyCode) ? map.get(keyCode) : map.get(KeyCode.SPACE);
    }

    public static void initialize() {
        map.put(KeyCode.DIGIT1, new Key(49, 2, 33));
        map.put(KeyCode.DIGIT2, new Key(50, 3, 64));
        map.put(KeyCode.DIGIT3, new Key(51, 4, 35));
        map.put(KeyCode.DIGIT4, new Key(52, 5, 36));
        map.put(KeyCode.DIGIT5, new Key(53, 6, 37));
        map.put(KeyCode.DIGIT6, new Key(54, 7, 94));
        map.put(KeyCode.DIGIT7, new Key(55, 8, 38));
        map.put(KeyCode.DIGIT8, new Key(56, 9, 42));
        map.put(KeyCode.DIGIT9, new Key(57, 10, 40));
        map.put(KeyCode.DIGIT0, new Key(48, 11, 41));

        map.put(KeyCode.A, new Key(97, 30, 65));
        map.put(KeyCode.B, new Key(98, 48, 66));
        map.put(KeyCode.C, new Key(99, 46, 67));
        map.put(KeyCode.D, new Key(100, 32, 68));
        map.put(KeyCode.E, new Key(101, 18, 69));
        map.put(KeyCode.F, new Key(102, 33, 70));
        map.put(KeyCode.G, new Key(103, 34, 71));
        map.put(KeyCode.H, new Key(104, 35, 72));
        map.put(KeyCode.I, new Key(105, 23, 73));
        map.put(KeyCode.J, new Key(106, 36, 74));
        map.put(KeyCode.K, new Key(107, 37, 75));
        map.put(KeyCode.L, new Key(108, 38, 76));
        map.put(KeyCode.M, new Key(109, 50, 77));
        map.put(KeyCode.N, new Key(110, 49, 78));
        map.put(KeyCode.O, new Key(111, 24, 79));
        map.put(KeyCode.P, new Key(112, 25, 80));
        map.put(KeyCode.Q, new Key(113, 16, 81));
        map.put(KeyCode.R, new Key(114, 19, 82));
        map.put(KeyCode.S, new Key(115, 31, 83));
        map.put(KeyCode.T, new Key(116, 20, 84));
        map.put(KeyCode.U, new Key(117, 22, 85));
        map.put(KeyCode.V, new Key(118, 47, 86));
        map.put(KeyCode.W, new Key(119, 17, 87));
        map.put(KeyCode.X, new Key(120, 45, 88));
        map.put(KeyCode.Y, new Key(121, 21, 89));
        map.put(KeyCode.Z, new Key(122, 44, 90));
        
        map.put(KeyCode.ENTER, new Key(13, 28));
        map.put(KeyCode.BACK_SPACE, new Key(127, 14));
        map.put(KeyCode.SPACE, new Key(32, 57));

        map.put(KeyCode.DOWN, new Key(63233, 208));
        map.put(KeyCode.BACK_SLASH, new Key(92, 43));
        map.put(KeyCode.DELETE, new Key(63272, 211));
        map.put(KeyCode.END, new Key(63275, 207, 63275));
    }
}
