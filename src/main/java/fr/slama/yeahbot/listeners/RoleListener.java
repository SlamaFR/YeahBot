package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.managers.SanctionManager;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created on 01/10/2018.
 */
public class RoleListener extends ListenerAdapter {

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        super.onGuildMemberRoleRemove(event);
        SanctionManager.unregisterMute(null, event.getMember());
    }
}
