package org.nerve.boot.util;

import static org.nerve.boot.Const.EMPTY;

public class StringUtil {
    /**
     转半角的函数(DBC case)
     全角空格为12288，半角空格为32
     其他字符半角(33-126)与全角(65281-65374)的对应关系是：均相差65248

     * @param input 任意字符串
     * @return 半角字符串
     */
    public static String ToDBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 12288) {
                //全角空格为12288，半角空格为32
                c[i] = (char) 32;
                continue;
            }
            if (c[i] > 65280 && c[i] < 65375)
                //其他字符半角(33-126)与全角(65281-65374)的对应关系是：均相差65248
                c[i] = (char) (c[i] - 65248);
        }
        return new String(c);
    }

    /**
     * 清除 HTML 标签
     * @param input
     * @return
     */
    public static String cleanHTML(String input){
        return input.replaceAll("<[^>]+>", EMPTY);
    }
}
