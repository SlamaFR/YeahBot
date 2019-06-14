package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Mutes;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created on 01/10/2018.
 */
public class RoleListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        super.onGuildMemberRoleRemove(event);

        for (Role role : event.getRoles()) {
            if (role.equals(GuildUtil.getMutedRole(event.getGuild(), false))) {
                try {
                    if (event.getGuild().getDefaultChannel() != null)
                        event.getGuild().getDefaultChannel().sendMessage(new EmbedBuilder()
                                .setColor(ColorUtil.GREEN)
                                .setTitle(LanguageUtil.getString(event.getGuild(), Bundle.CAPTION, "sanction_cancellation"))
                                .setDescription(LanguageUtil.getArguedString(event.getGuild(), Bundle.STRINGS, "user_unmuted", event.getMember().getAsMention()))
                                .build()).queue();

                    Mutes mutes = RedisData.getMutes(event.getGuild());
                    mutes.getMutesMap().remove(event.getUser().getIdLong());
                    RedisData.setMutes(event.getGuild(), mutes);
                } catch (InsufficientPermissionException ignored) {
                }
                return;
            }
        }

    }
}
