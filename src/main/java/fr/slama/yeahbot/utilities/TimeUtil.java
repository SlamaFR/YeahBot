package fr.slama.yeahbot.utilities;

/**
 * Created on 13/11/2018.
 */
public class TimeUtil {

    public static String toTime(long millis) {
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60));

        return hour == 0 ? String.format("%02d:%02d", minute, second) : String.format("%02d:%02d:%02d", hour, minute, second);
    }

}
