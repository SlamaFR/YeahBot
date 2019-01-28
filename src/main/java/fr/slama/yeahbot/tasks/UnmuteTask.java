package fr.slama.yeahbot.tasks;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Mutes;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Created on 01/10/2018.
 */
public class UnmuteTask extends TimerTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run() {

        for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

            Mutes mutes = RedisData.getMutes(guild);
            List<Long> toRemove = new ArrayList<>();

            for (Long userId : mutes.getMutesMap().keySet()) {

                long timeout = mutes.getMutesMap().get(userId);

                try {
                    long now = System.currentTimeMillis();
                    if (now > timeout) {
                        guild.getController()
                                .removeRolesFromMember(guild.getMemberById(userId), GuildUtil.getMutedRole(guild, false))
                                .queue();
                        toRemove.add(userId);
                        logger.info("Unmuted " + userId);
                    } else {
                        TaskScheduler.async(() -> {
                            guild.getController()
                                    .removeRolesFromMember(guild.getMemberById(userId), GuildUtil.getMutedRole(guild, false))
                                    .queue();
                            mutes.getMutesMap().remove(userId);
                            RedisData.setMutes(guild, mutes);
                        }, timeout - now);
                        logger.info("Scheduled unmute of " + userId);
                    }
                } catch (Exception ignored) {
                    break;
                }

            }

            mutes.getMutesMap().keySet().removeAll(toRemove);
            RedisData.setMutes(guild, mutes);

        }

    }

}
