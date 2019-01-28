package fr.slama.yeahbot.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 11/05/2018.
 */
public class Language {

    public static final String FRENCH = "fr";
    public static final String ENGLISH = "en";

    public static final String MISSING = "** \\*Missing value\\* **";

    public static final List<String> languages = new ArrayList<>(
            Arrays.asList(FRENCH, ENGLISH)
    );

}
