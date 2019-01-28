package fr.slama.yeahbot.language;

import fr.slama.yeahbot.redis.RedisData;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created on 11/05/2018.
 */
public class LanguageUtil {

    private static final Logger logger = LoggerFactory.getLogger(LanguageUtil.class);

    public static String getString(String language, Bundle bundle, String key) {

        if (language == null || language.isEmpty()) language = Language.ENGLISH;

        if (!Language.languages.contains(language)) language = Language.ENGLISH;

        ResourceBundle resourceBundle;
        try {
            resourceBundle = ResourceBundle.getBundle(bundle.getName(), new Locale(language));
        } catch (MissingResourceException e) {
            logger.error("[FATAL] Missing " + bundle + " bundle!");
            return language.equals(Language.ENGLISH) ? Language.MISSING : getString(Language.ENGLISH, bundle, key);
        }

        try {
            String value = resourceBundle.getString(key);
            return new String(value.getBytes(ISO_8859_1), UTF_8);
        } catch (MissingResourceException e) {
            if (bundle.equals(Bundle.DESCRIPTION) || bundle.equals(Bundle.ARGUMENTS) || bundle.equals(Bundle.ARGUMENTS_DESCRIPTION))
                return language.equals(Language.ENGLISH) ? "" : getString(Language.ENGLISH, bundle, key);
            logger.error("[FATAL] Missing " + key + " key in " + bundle + " bundle!");
            return language.equals(Language.ENGLISH) ? Language.MISSING : getString(Language.ENGLISH, bundle, key);
        }

    }

    public static String getString(Guild guild, Bundle bundle, String key) {
        if (guild == null) return getString(Language.ENGLISH, bundle, key);
        return getString(RedisData.getSettings(guild).locale, bundle, key);
    }

    public static String getArguedString(String language, Bundle bundle, String key, Object... arguments) {

        String value = getString(language, bundle, key);

        if (value.contains("{")) return MessageFormat.format(value.replace("'", "''"), (Object[]) arguments);
        else return MessageFormat.format(value, arguments);

    }

    public static String getArguedString(Guild guild, Bundle bundle, String key, Object... arguments) {
        if (guild == null) return getArguedString(Language.ENGLISH, bundle, key, arguments);
        return getArguedString(RedisData.getSettings(guild).locale, bundle, key, arguments);
    }

    public static String getState(State state, Guild guild, Object object) {
        switch (state) {
            case BOOLEAN:
                if ((boolean) object) return getString(guild, Bundle.CAPTION, "state_enabled");
                return getString(guild, Bundle.CAPTION, "state_disabled");
            default:
                return null;
        }
    }

    public enum State {

        BOOLEAN

    }

}
