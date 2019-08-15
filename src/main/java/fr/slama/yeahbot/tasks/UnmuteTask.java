package fr.slama.yeahbot.tasks;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.Mute;
import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.managers.SanctionManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TimerTask;

/**
 * Created on 01/10/2018.
 */
public class UnmuteTask extends TimerTask {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public void run() {

        for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

            Map<Long, Mute> mutes = RedisData.getMutes(guild).getMutesMap();

            for (Long id : mutes.keySet()) {

                Member target = guild.getMemberById(id);

                if (target == null) continue;

                Mute mute = mutes.get(id);
                Member author = guild.getMemberById(mute.getAuthorId());
                String reason = String.format("%s (%s)", mute.getReason(),
                        LanguageUtil.getString(guild, Bundle.CAPTION, "expiration"));

                long now = System.currentTimeMillis();
                if (now > mute.getTimeout()) {
                    SanctionManager.unregisterMute(author, target, null, reason);
                } else {
                    TaskScheduler.scheduleDelayed(() ->
                                    SanctionManager.unregisterMute(author, target, null, reason)
                            , mute.getTimeout() - now);
                    LOGGER.info("Scheduled unmute of " + id);
                }
            }
        }
    }
}
