package fr.slama.yeahbot.tasks;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Mutes;
import fr.slama.yeahbot.utilities.GuildUtil;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created on 01/10/2018.
 */
public class UnmuteTask extends TimerTask {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public void run() {

        for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

            Mutes mutes = RedisData.getMutes(guild);
            List<Long> toRemove = new ArrayList<>();

            for (Long userId : mutes.getMutesMap().keySet()) {

                long timeout = mutes.getMutesMap().get(userId).getTimeout();

                try {
                    long now = System.currentTimeMillis();
                    if (now > timeout) {
                        guild.removeRoleFromMember(guild.getMemberById(userId), GuildUtil.getMutedRole(guild, null, false))
                                .queue();
                        toRemove.add(userId);
                        LOGGER.info("Unmuted " + userId);
                    } else {
                        TaskScheduler.scheduleDelayed(() -> {
                            guild.removeRoleFromMember(guild.getMemberById(userId), GuildUtil.getMutedRole(guild, null, false))
                                    .queue();
                            mutes.getMutesMap().remove(userId);
                            RedisData.setMutes(guild, mutes);
                        }, timeout - now);
                        LOGGER.info("Scheduled unmute of " + userId);
                    }
                } catch (Exception e) {
                    continue;
                }

            }

            mutes.getMutesMap().keySet().removeAll(toRemove);
            RedisData.setMutes(guild, mutes);

        }

    }

}
