package fr.slama.yeahbot.tasks;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.PrivateChannels;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimerTask;

/**
 * Created on 15/11/2018.
 */
public class PrivateChannelsTask extends TimerTask {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run() {

        for (Guild guild : YeahBot.getInstance().getShardManager().getGuilds()) {

            PrivateChannels channels = RedisData.getPrivateChannels(guild);

            for (long id : channels.getChannels()) {
                if (guild.getVoiceChannelById(id) == null)
                    channels.getChannels().remove(id);
                logger.info("Private channel " + id + " expired");
            }

        }

    }

}
