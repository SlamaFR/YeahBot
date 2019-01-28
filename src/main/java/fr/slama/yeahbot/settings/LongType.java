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
public @interface LongType {

    Type type();

    enum Type {
        CHANNEL,
        ROLE
    }

}
