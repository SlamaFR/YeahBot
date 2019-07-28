package fr.slama.yeahbot.utilities;

/**
 * Created on 2019-06-14.
 */
public class StringUtil {

    public static String replaceLast(String find, String replace, String string) {
        return string.replaceFirst("(?s)(.*)" + find, "$1" + replace);
    }

}
