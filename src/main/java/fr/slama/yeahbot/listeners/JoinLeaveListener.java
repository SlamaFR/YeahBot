package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Created on 04/12/2018.
 */
public class JoinLeaveListener extends ListenerAdapter {

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        if (event.getGuild().getDefaultChannel() != null) {
            YeahBot.getInstance().getSetupManager().startWizard(event.getGuild().getDefaultChannel(), event.getGuild().getOwner());
        }
        super.onGuildJoin(event);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Settings settings = RedisData.getSettings(guild);

        if (!settings.sayWelcome) return;

        String message = settings.welcomeMessage;

        if (message.isEmpty())
            message = LanguageUtil.getString(guild, Bundle.STRINGS, "welcome_message");

        GuildUtil.getWelcomeChannel(guild).sendMessage(
                message.replace("$guild", guild.getName())
                        .replace("$user", event.getMember().getEffectiveName())
                        .replace("$count", String.valueOf(guild.getMembers().size()))
        ).queue();
        super.onGuildMemberJoin(event);
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        Guild guild = event.getGuild();
        Settings settings = RedisData.getSettings(guild);

        if (!settings.sayGoodbye) return;

        String message = settings.goodbyeMessage;

        if (message.isEmpty())
            message = LanguageUtil.getString(guild, Bundle.STRINGS, "goodbye_message");

        GuildUtil.getWelcomeChannel(guild).sendMessage(
                    message.replace("$guild", guild.getName())
                            .replace("$user", event.getMember().getEffectiveName())
                            .replace("$count", String.valueOf(guild.getMembers().size()))
            ).queue();
        super.onGuildMemberLeave(event);
    }
}
