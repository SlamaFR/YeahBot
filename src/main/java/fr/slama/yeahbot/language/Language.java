package fr.slama.yeahbot.language;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created on 11/05/2018.
 */
public enum Language {

    FRENCH("fr", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
    ENGLISH("en", "English", "\uD83C\uDDEC\uD83C\uDDE7");

    public static final String MISSING = "** \\*Missing value\\* **";

    private final String code;
    private final String name;
    private final String emote;

    Language(String code, String name, String emote) {
        this.code = code;
        this.name = name;
        this.emote = emote;
    }

    public static boolean has(String lang) {
        return Arrays.stream(values()).anyMatch(l -> l.getCode().equals(lang));
    }

    public static String[] codeValues() {
        return Arrays.stream(values()).map(Language::getCode).toArray(String[]::new);
    }

    public static Language fromEmote(String emote) {
        Optional<Language> match = Arrays.stream(values()).filter(l -> emote.equals(l.getEmote())).findFirst();
        return match.orElse(ENGLISH);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getEmote() {
        return emote;
    }
}
