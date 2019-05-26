package fr.slama.yeahbot.utilities;

import java.util.Arrays;

/**
 * Created on 2019-05-26.
 */
public class ArrayUtil {

    public static <T> T[] appendTo(T[] array, T element) {
        T[] result = Arrays.copyOf(array, array.length + 1);
        result[result.length - 1] = element;
        return result;
    }

    public static <T> T[] removeLast(T[] array) {
        return Arrays.copyOf(array, array.length - 1);
    }

}
