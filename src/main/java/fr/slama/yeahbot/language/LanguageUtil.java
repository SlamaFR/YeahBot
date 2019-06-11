package fr.slama.yeahbot.language;

import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.utilities.ArrayUtil;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created on 11/05/2018.
 */
public class LanguageUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanguageUtil.class);

    public static String getString(String language, Bundle bundle, String key) {

        String lang = language;

        if (lang == null || lang.isEmpty()) lang = Language.ENGLISH;

        if (!Language.languages.contains(lang)) lang = Language.ENGLISH;

        ResourceBundle resourceBundle;
        try {
            resourceBundle = ResourceBundle.getBundle(bundle.getName(), new Locale(lang));
        } catch (MissingResourceException e) {
            LOGGER.error("[FATAL] Missing {} bundle!", bundle);
            return lang.equals(Language.ENGLISH) ? Language.MISSING : getString(Language.ENGLISH, bundle, key);
        }

        try {
            String value = resourceBundle.getString(key);
            return new String(value.getBytes(ISO_8859_1), UTF_8);
        } catch (MissingResourceException e) {
            if (bundle.equals(Bundle.DESCRIPTION) || bundle.equals(Bundle.ARGUMENTS) || bundle.equals(Bundle.ARGUMENTS_DESCRIPTION))
                return lang.equals(Language.ENGLISH) ? "" : getString(Language.ENGLISH, bundle, key);
            LOGGER.error("[FATAL] Missing {} key in {} bundle!", key, bundle);
            return lang.equals(Language.ENGLISH) ? Language.MISSING : getString(Language.ENGLISH, bundle, key);
        }

    }

    public static String getString(Guild guild, Bundle bundle, String key) {
        if (guild == null) return getString(Language.ENGLISH, bundle, key);
        return getString(RedisData.getSettings(guild).locale, bundle, key);
    }

    public static String getArguedString(String language, Bundle bundle, String key, Object... arguments) {

        String value = getString(language, bundle, key);

        for (Object o : arguments) {
            if (o.getClass().isArray()) {
                arguments = ArrayUtil.removeLast(arguments);
                for (Object o1 : ((Object[]) o)) arguments = ArrayUtil.appendTo(arguments, o1);
            }
        }

        if (value.contains("{")) return MessageFormat.format(value.replace("'", "''"), (Object[]) arguments);
        else return MessageFormat.format(value, arguments);

    }

    public static String getArguedString(Guild guild, Bundle bundle, String key, Object... arguments) {
        if (guild == null) return getArguedString(Language.ENGLISH, bundle, key, arguments);
        return getArguedString(RedisData.getSettings(guild).locale, bundle, key, arguments);
    }

    public static String getState(Guild guild, boolean bool) {
        return getString(guild, Bundle.CAPTION, bool ? "state_enabled" : "state_disabled");
    }

    public static String getTimeUnit(Guild guild, TimeUnit unit, long time) {
        return LanguageUtil.getString(guild, Bundle.UNIT, unit.toString().toLowerCase() + (time > 1 ? "s" : ""));
    }

    public static String getTimeExpiration(Guild guild, long time, TimeUnit unit) {
        return LanguageUtil.getArguedString(guild, Bundle.CAPTION, "custom_time_expiration", time, getTimeUnit(guild, unit, time));
    }

    public static String getLink(Guild guild, String url) {
        return String.format("[%s](%s)", LanguageUtil.getString(guild, Bundle.CAPTION, "click_here"), url);
    }

}
