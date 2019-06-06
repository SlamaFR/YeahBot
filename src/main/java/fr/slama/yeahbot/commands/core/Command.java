package fr.slama.yeahbot.commands.core;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.managers.PremiumManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

/**
 * Created on 15/03/2018.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    String name();

    String[] aliases() default {};

    boolean displayInHelp() default true;

    CommandCategory category() default CommandCategory.MISCELLANEOUS;

    CommandPermission permission() default CommandPermission.EVERYONE;

    Permission[] discordPermission() default {Permission.UNKNOWN};

    CommandExecutor executor() default CommandExecutor.ALL;

    enum CommandCategory {

        MUSIC("music", "\uD83C\uDFA7"),
        UTIL("util", "\uD83D\uDEE0"),
        MODERATION("moderation", "\uD83D\uDEE1"),
        FUN("fun", "\uD83C\uDF89 "),
        MISCELLANEOUS("miscellaneous", "⚙");

        private String name, emote;

        CommandCategory(String name, String emote) {
            this.name = name;
            this.emote = emote;
        }

        public String getName() {
            return name;
        }

        public String getEmote() {
            return emote;
        }

    }

    enum CommandPermission {

        EVERYONE {
            @Override
            public boolean test(Member member) {
                return true;
            }
        },
        DJ {
            @Override
            public boolean test(Member member) {
                return true;
            }
        },
        PLAYLIST_OWNER {
            @Override
            public boolean test(Member member) {
                return true;
            }
        },
        PREMIUM {
            @Override
            public boolean test(Member member) {
                return PremiumManager.isPremium(member.getUser().getId()) || OWNER.test(member);
            }
        },
        STAFF {
            @Override
            public boolean test(Member member) {
                return member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR) ||
                        member.hasPermission(Permission.MANAGE_SERVER) || OWNER.test(member);
            }
        },
        SERVER_OWNER {
            @Override
            public boolean test(Member member) {
                return OWNER.test(member) || member.isOwner();
            }
        },
        OWNER {
            @Override
            public boolean test(Member member) {
                if (YeahBot.getConfig() != null)
                    return Arrays.stream(YeahBot.getConfig().owners).anyMatch(id -> member.getUser().getId().equals(id));
                return false;
            }
        };

        public abstract boolean test(Member member);

    }

    enum CommandExecutor {

        ALL, USER, CONSOLE

    }

}
