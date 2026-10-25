package com.createconnection.morse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MorseCode {
    private static final Map<Character, String> CHAR_TO_MORSE = new HashMap<>();

    static {
        // Latin
        CHAR_TO_MORSE.put('A', ".-");
        CHAR_TO_MORSE.put('B', "-...");
        CHAR_TO_MORSE.put('C', "-.-.");
        CHAR_TO_MORSE.put('D', "-..");
        CHAR_TO_MORSE.put('E', ".");
        CHAR_TO_MORSE.put('F', "..-.");
        CHAR_TO_MORSE.put('G', "--.");
        CHAR_TO_MORSE.put('H', "....");
        CHAR_TO_MORSE.put('I', "..");
        CHAR_TO_MORSE.put('J', ".---");
        CHAR_TO_MORSE.put('K', "-.-");
        CHAR_TO_MORSE.put('L', ".-..");
        CHAR_TO_MORSE.put('M', "--");
        CHAR_TO_MORSE.put('N', "-.");
        CHAR_TO_MORSE.put('O', "---");
        CHAR_TO_MORSE.put('P', ".--.");
        CHAR_TO_MORSE.put('Q', "--.-");
        CHAR_TO_MORSE.put('R', ".-.");
        CHAR_TO_MORSE.put('S', "...");
        CHAR_TO_MORSE.put('T', "-");
        CHAR_TO_MORSE.put('U', "..-");
        CHAR_TO_MORSE.put('V', "...-");
        CHAR_TO_MORSE.put('W', ".--");
        CHAR_TO_MORSE.put('X', "-..-");
        CHAR_TO_MORSE.put('Y', "-.--");
        CHAR_TO_MORSE.put('Z', "--..");

        // Russian Cyrillic
        CHAR_TO_MORSE.put('А', ".-");
        CHAR_TO_MORSE.put('Б', "-...");
        CHAR_TO_MORSE.put('В', ".--");
        CHAR_TO_MORSE.put('Г', "--.");
        CHAR_TO_MORSE.put('Д', "-..");
        CHAR_TO_MORSE.put('Е', ".");
        CHAR_TO_MORSE.put('Ё', ".");
        CHAR_TO_MORSE.put('Ж', "...-");
        CHAR_TO_MORSE.put('З', "--..");
        CHAR_TO_MORSE.put('И', "..");
        CHAR_TO_MORSE.put('Й', ".---");
        CHAR_TO_MORSE.put('К', "-.-");
        CHAR_TO_MORSE.put('Л', ".-..");
        CHAR_TO_MORSE.put('М', "--");
        CHAR_TO_MORSE.put('Н', "-.");
        CHAR_TO_MORSE.put('О', "---");
        CHAR_TO_MORSE.put('П', ".--.");
        CHAR_TO_MORSE.put('Р', ".-.");
        CHAR_TO_MORSE.put('С', "...");
        CHAR_TO_MORSE.put('Т', "-");
        CHAR_TO_MORSE.put('У', "..-");
        CHAR_TO_MORSE.put('Ф', "..-.");
        CHAR_TO_MORSE.put('Х', "....");
        CHAR_TO_MORSE.put('Ц', "-.-.");
        CHAR_TO_MORSE.put('Ч', "---.");
        CHAR_TO_MORSE.put('Ш', "----");
        CHAR_TO_MORSE.put('Щ', "--.-");
        CHAR_TO_MORSE.put('Ъ', "--.--");
        CHAR_TO_MORSE.put('Ы', "-.--");
        CHAR_TO_MORSE.put('Ь', "-..-");
        CHAR_TO_MORSE.put('Э', "..-..");
        CHAR_TO_MORSE.put('Ю', "..--");
        CHAR_TO_MORSE.put('Я', ".-.-");

        // Digits & punctuation
        CHAR_TO_MORSE.put('0', "-----");
        CHAR_TO_MORSE.put('1', ".----");
        CHAR_TO_MORSE.put('2', "..---");
        CHAR_TO_MORSE.put('3', "...--");
        CHAR_TO_MORSE.put('4', "....-");
        CHAR_TO_MORSE.put('5', ".....");
        CHAR_TO_MORSE.put('6', "-....");
        CHAR_TO_MORSE.put('7', "--...");
        CHAR_TO_MORSE.put('8', "---..");
        CHAR_TO_MORSE.put('9', "----.");
        CHAR_TO_MORSE.put('?', "..--..");
        CHAR_TO_MORSE.put('!', "-.-.--");
        CHAR_TO_MORSE.put('.', ".");
        CHAR_TO_MORSE.put('-', "-");
        CHAR_TO_MORSE.put('•', ".");
        CHAR_TO_MORSE.put('—', "-");
    }

    private MorseCode() {}

    public static String encode(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String upper = text.trim().toUpperCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (Character.isWhitespace(c) || c == '/') {
                if (sb.length() > 0 && !sb.toString().endsWith("/ ")) {
                    if (sb.charAt(sb.length() - 1) != ' ') {
                        sb.append(' ');
                    }
                    sb.append("/ ");
                }
                continue;
            }
            String m = CHAR_TO_MORSE.get(c);
            if (m != null) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                sb.append(m);
            }
        }
        return sb.toString().trim();
    }

    public static String formatPretty(String morse) {
        return morse.replace('.', '•').replace('-', '—');
    }

    public static boolean[] toTimeline(String text) {
        String morse = encode(text);
        if (morse.isEmpty()) {
            return new boolean[0];
        }
        List<Boolean> list = new ArrayList<>();
        for (int i = 0; i < morse.length(); i++) {
            char c = morse.charAt(i);
            if (c == '.') {
                // Dot: 2 ticks ON, 2 ticks OFF
                list.add(true);
                list.add(true);
                list.add(false);
                list.add(false);
            } else if (c == '-') {
                // Dash: 6 ticks ON, 2 ticks OFF
                for (int k = 0; k < 6; k++) list.add(true);
                list.add(false);
                list.add(false);
            } else if (c == ' ') {
                // Letter gap: 4 extra ticks OFF
                for (int k = 0; k < 4; k++) list.add(false);
            } else if (c == '/') {
                // Word gap: 6 extra ticks OFF
                for (int k = 0; k < 6; k++) list.add(false);
            }
        }
        boolean[] arr = new boolean[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
