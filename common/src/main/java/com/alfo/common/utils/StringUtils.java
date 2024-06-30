package com.alfo.common.utils;

public class StringUtils {

    //全部字母（大小写英文，数字）
    public static final char[] letters = getLetters();

    private static char[] getLetters() {
        char[] result = new char[62];
        int index = 0;
        for (int i = 'A'; i <= 'Z'; i++) {
            result[index++] = (char) i;
            result[index++] = (char) (i + 32);
        }
        for (int i = 0; i <= 9; i++) {
            result[index++] = (char)(i + '0');
        }
        return result;
    }
}
