package fr.slama.yeahbot.utilities;

import fr.slama.yeahbot.language.Bundle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

/**
 * Created on 2019-06-14.
 */
public class StringUtil {

    public static String replaceLast(String find, String replace, String string) {
        return string.replaceFirst("(?s)(.*)" + find, "$1" + replace);
    }

    public static ResponseStringBuilder response(Guild guild) {
        return new ResponseStringBuilder(guild);
    }

    public static final class ResponseStringBuilder {

        private final Guild guild;
        private String username = "";
        private boolean canCancel = false;
        private boolean canReset = false;

        private ResponseStringBuilder(Guild guild) {
            this.guild = guild;
        }

        public ResponseStringBuilder of(Member member) {
            this.username = member.getEffectiveName();
            return this;
        }

        public ResponseStringBuilder of(User user) {
            this.username = user.getName();
            return this;
        }

        public ResponseStringBuilder canCancel(boolean bool) {
            this.canCancel = bool;
            return this;
        }

        public ResponseStringBuilder canReset(boolean bool) {
            this.canReset = bool;
            return this;
        }

        public String build() {
            StringBuilder builder = new StringBuilder();
            if (!username.isEmpty()) {
                builder.append(LanguageUtil.getArguedString(guild, Bundle.CAPTION, "waiting_for_response_of", username));
            } else {
                builder.append(LanguageUtil.getString(guild, Bundle.CAPTION, "waiting_for_response"));
            }
            if (canCancel) {
                if (canReset) {
                    builder.append(" ").append(LanguageUtil.getString(guild, Bundle.CAPTION, "can_cancel_and_reset"));
                } else {
                    builder.append(" ").append(LanguageUtil.getString(guild, Bundle.CAPTION, "can_cancel"));
                }
            }
            return builder.toString();
        }

    }

}
