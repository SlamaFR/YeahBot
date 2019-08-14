package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.managers.SanctionManager;
import fr.slama.yeahbot.utilities.GuildUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

/**
 * Created on 01/10/2018.
 */
public class RoleListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        super.onGuildMemberRoleRemove(event);

        if (event.getRoles().contains(GuildUtil.getMutedRole(event.getGuild(), false)))
            SanctionManager.unregisterMute(null, event.getMember());
    }

    @Override
    public void onTextChannelCreate(@Nonnull TextChannelCreateEvent event) {
        super.onTextChannelCreate(event);

        Role role = GuildUtil.getMutedRole(event.getGuild(), true);

        if (role == null) return;

        event.getChannel().createPermissionOverride(role)
                .setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION)
                .queue();
    }
}
