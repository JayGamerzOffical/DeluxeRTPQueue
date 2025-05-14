package org.JayGamerz.utils;


import org.bukkit.ChatColor;

public class SmallTextConverter {


    public static String toSmallCaps(String input) {
        StringBuilder sb = new StringBuilder ( );
        ChatColor currentColor = null;

        for (int i = 0; i < input.length ( ); i++) {
            char c = input.charAt (i);

            // Detect color codes
            if ( c == ChatColor.COLOR_CHAR && i + 1 < input.length ( ) ) {
                currentColor = ChatColor.getByChar (input.charAt (i + 1));
                sb.append (c).append (input.charAt (i + 1));
                i++; // skip next char
                continue;
            }

            // Convert letters to small caps
            if ( Character.isLetter (c) ) {
                sb.append (toSmall (c));
            } else {
                sb.append (c);
            }
        }

        return sb.toString ( );
    }

    private static char toSmall(char c) {
        switch (Character.toLowerCase (c)) {
            case 'a':
                return 'ᴀ';
            case 'b':
                return 'ʙ';
            case 'c':
                return 'ᴄ';
            case 'd':
                return 'ᴅ';
            case 'e':
                return 'ᴇ';
            case 'f':
                return 'ғ';
            case 'g':
                return 'ɢ';
            case 'h':
                return 'ʜ';
            case 'i':
                return 'ɪ';
            case 'j':
                return 'ᴊ';
            case 'k':
                return 'ᴋ';
            case 'l':
                return 'ʟ';
            case 'm':
                return 'ᴍ';
            case 'n':
                return 'ɴ';
            case 'o':
                return 'ᴏ';
            case 'p':
                return 'ᴘ';
            case 'q':
                return 'ǫ';
            case 'r':
                return 'ʀ';
            case 's':
                return 's'; // no small variant
            case 't':
                return 'ᴛ';
            case 'u':
                return 'ᴜ';
            case 'v':
                return 'ᴠ';
            case 'w':
                return 'ᴡ';
            case 'x':
                return 'x'; // no small variant
            case 'y':
                return 'ʏ';
            case 'z':
                return 'ᴢ';
            default:
                return c;
        }
    }
}