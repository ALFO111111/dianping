package com.alfo.common.utils.constants;

import com.alfo.common.utils.StringUtils;

import java.util.Random;

public class UserConstants {

    public static final String USER_NAME = "user_" + getRandomStrLen8();


    private static String getRandomStrLen8() {
        char[] letters = StringUtils.letters;
        int lettersLen = letters.length;
        StringBuilder result = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 8; i++) {
            result.append(letters[r.nextInt(lettersLen)]);
        }
        return result.toString();
    }

}
