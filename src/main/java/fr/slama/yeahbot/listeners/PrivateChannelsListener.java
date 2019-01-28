package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.PrivateChannels;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created on 15/11/2018.
 */
public class PrivateChannelsListener extends ListenerAdapter {

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        deleteChannel(event.getGuild(), event.getChannelLeft());
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        deleteChannel(event.getGuild(), event.getChannelLeft());
        super.onGuildVoiceMove(event);
    }

    private void deleteChannel(Guild guild, VoiceChannel voiceChannel) {

        PrivateChannels channels = RedisData.getPrivateChannels(guild);

        if (channels.getChannels().contains(voiceChannel.getIdLong())) {
            voiceChannel.delete().queue(channel -> {
                channels.getChannels().remove(voiceChannel.getIdLong());
                RedisData.setPrivateChannels(guild, channels);
            });
        }

    }

}
