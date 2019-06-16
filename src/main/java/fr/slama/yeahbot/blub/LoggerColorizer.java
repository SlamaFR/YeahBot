package fr.slama.yeahbot.blub;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ANSIConstants;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

/**
 * Created on 23/09/2018.
 */
public class LoggerColorizer extends ForegroundCompositeConverterBase<ILoggingEvent> {


    @Override
    protected String getForegroundColorCode(ILoggingEvent iLoggingEvent) {
        Level level = iLoggingEvent.getLevel();
        switch (level.toInt()) {
            case Level.ERROR_INT:
                return ANSIConstants.BOLD + ANSIConstants.RED_FG;
            case Level.WARN_INT:
                return ANSIConstants.YELLOW_FG;
            case Level.INFO_INT:
                return ANSIConstants.BLUE_FG;
            case Level.DEBUG_INT:
                return ANSIConstants.GREEN_FG;
            case Level.TRACE_INT:
                return ANSIConstants.CYAN_FG;
            default:
                return ANSIConstants.DEFAULT_FG;
        }
    }

}
