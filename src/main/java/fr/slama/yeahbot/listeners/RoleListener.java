package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.managers.SanctionManager;
import fr.slama.yeahbot.utilities.GuildUtil;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Created on 01/10/2018.
 */
public class RoleListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        super.onGuildMemberRoleRemove(event);

        if (event.getRoles().contains(GuildUtil.getMutedRole(event.getGuild(), null, false)))
            SanctionManager.unregisterMute(null, event.getMember());
    }
}
