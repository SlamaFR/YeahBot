package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.blub.SetupWizard;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2019-07-29.
 */
public class SetupManager {

    private final Map<Long, SetupWizard> wizardMap;

    public SetupManager() {
        this.wizardMap = new HashMap<>();
    }

    public boolean startWizard(TextChannel textChannel, Member member) {
        Guild guild = member.getGuild();
        if (wizardMap.containsKey(guild.getIdLong())) return false;
        SetupWizard setupWizard = new SetupWizard(textChannel, member);
        setupWizard.start();
        wizardMap.put(guild.getIdLong(), setupWizard);
        return true;
    }

    public void deleteWizard(Guild guild) {
        wizardMap.remove(guild.getIdLong());
    }

}
