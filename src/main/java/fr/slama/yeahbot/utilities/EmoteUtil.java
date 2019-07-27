package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.music.PlayerSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 13/11/2018.
 */
public class EmoteUtil {

    /*
    Constants
     */
    public static final String NO_REACTION = "514172078153400335";
    public static final String YES_REACTION = "514172065788461068";

    public static final String YES = "<:yes:514172065788461068>";
    public static final String NO = "<:no:514172078153400335>";
    public static final String GREEN_DOT = "<:green_dot:604164103833124885>";
    public static final String ORANGE_DOT = "<:orange_dot:604164105116450836> ";
    public static final String RED_DOT = "<:red_dot:604164105523167243>";

    public static final String PREVIOUS = "⬅";
    public static final String NEXT = "➡";
    public static final String EMOJI_REGEX = "(?:[\\u2700-\\u27bf]|" +
            "(?:[\\ud83c\\udde6-\\ud83c\\uddff]){2}|" +
            "[\\ud800\\udc00-\\uDBFF\\uDFFF]|[\\u2600-\\u26FF])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe23\\u20d0-\\u20f0]|[\\ud83c\\udffb-\\ud83c\\udfff])?" +
            "(?:\\u200d(?:[^\\ud800-\\udfff]|" +
            "(?:[\\ud83c\\udde6-\\ud83c\\uddff]){2}|" +
            "[\\ud800\\udc00-\\uDBFF\\uDFFF]|[\\u2600-\\u26FF])[\\ufe0e\\ufe0f]?(?:[\\u0300-\\u036f\\ufe20-\\ufe23\\u20d0-\\u20f0]|[\\ud83c\\udffb-\\ud83c\\udfff])?)*|" +
            "[\\u0023-\\u0039]\\ufe0f?\\u20e3|\\u3299|\\u3297|\\u303d|\\u3030|\\u24c2|[\\ud83c\\udd70-\\ud83c\\udd71]|[\\ud83c\\udd7e-\\ud83c\\udd7f]|\\ud83c\\udd8e|[\\ud83c\\udd91-\\ud83c\\udd9a]|[\\ud83c\\udde6-\\ud83c\\uddff]|[\\ud83c\\ude01-\\ud83c\\ude02]|\\ud83c\\ude1a|\\ud83c\\ude2f|[\\ud83c\\ude32-\\ud83c\\ude3a]|[\\ud83c\\ude50-\\ud83c\\ude51]|\\u203c|\\u2049|[\\u25aa-\\u25ab]|\\u25b6|\\u25c0|[\\u25fb-\\u25fe]|\\u00a9|\\u00ae|\\u2122|\\u2139|\\ud83c\\udc04|[\\u2600-\\u26FF]|\\u2b05|\\u2b06|\\u2b07|\\u2b1b|\\u2b1c|\\u2b50|\\u2b55|\\u231a|\\u231b|\\u2328|\\u23cf|[\\u23e9-\\u23f3]|[\\u23f8-\\u23fa]|\\ud83c\\udccf|\\u2934|\\u2935|[\\u2190-\\u21ff]";
    private static final String[] NUMBERS = new String[]{
            "\u0031\u20E3",
            "\u0032\u20E3",
            "\u0033\u20E3",
            "\u0034\u20E3",
            "\u0035\u20E3",
            "\u0036\u20E3",
            "\u0037\u20E3",
            "\u0038\u20E3",
            "\u0039\u20E3",
            "\uD83D\uDD1F"
    };

    /*
    Getters
     */
    public static String getVolumeEmote(int volume) {
        if (volume == 0) return "\uD83D\uDD07";
        if (volume < 33) return "\uD83D\uDD08";
        if (volume < 66) return "\uD83D\uDD09";
        return "\uD83D\uDD0A";
    }

    public static String getSequenceEmote(PlayerSequence sequence) {
        switch (sequence) {
            case NORMAL:
                return "⏭";
            case QUEUE_LOOP:
                return "\uD83D\uDD01";
            case SHUFFLE_QUEUE_LOOP:
                return "<:shuffle_loop:536348500762427401>";
            case LOOP:
                return "\uD83D\uDD02";
            case SHUFFLE:
                return "\uD83D\uDD00";
            default:
                return "";
        }
    }

    public static List<String> getQuestionEmotes() {
        return Arrays.asList(NO_REACTION, YES_REACTION);
    }

    public static ArrayList<String> getNumbers(int count) {
        return new ArrayList<>(Arrays.asList(NUMBERS).subList(0, count));
    }

}
