package fr.slama.yeahbot.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created on 21/12/2018.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AvailableVariables {

    Variables[] variables();

    enum Variables {

        GUILD("$guild", "var_guild"),
        COUNT("$count", "var_count"),
        USER("$user", "var_user");

        private final String var;
        private final String key;

        Variables(String var, String key) {
            this.var = var;
            this.key = key;
        }

        public String getVar() {
            return var;
        }

        public String getKey() {
            return key;
        }
    }

}
