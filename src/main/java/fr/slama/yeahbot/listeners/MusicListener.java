package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.music.MusicManager;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created on 22/09/2018.
 */
public class MusicListener extends ListenerAdapter {

    private final MusicManager manager = YeahBot.getInstance().getMusicManager();

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {

        if (!event.getMember().equals(event.getGuild().getSelfMember()) &&
                event.getChannelLeft().equals(event.getGuild().getSelfMember().getVoiceState().getChannel()) &&
                event.getChannelLeft().getMembers().stream().filter(m -> !m.getUser().isBot()).toArray().length <= 1) {
            leave(event.getChannelLeft());
        }
        super.onGuildVoiceLeave(event);

    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {

        if (!event.getMember().equals(event.getGuild().getSelfMember()) &&
                event.getChannelLeft().equals(event.getGuild().getSelfMember().getVoiceState().getChannel()) &&
                event.getChannelLeft().getMembers().stream().filter(m -> !m.getUser().isBot()).toArray().length <= 1) {
            leave(event.getChannelLeft());
        }
        super.onGuildVoiceMove(event);
    }

    private void leave(VoiceChannel channel) {
        channel.getGuild().getAudioManager().closeAudioConnection();
        manager.getPlayer(channel.getGuild()).getTextChannel().sendMessage(
                LanguageUtil.getArguedString(channel.getGuild(), Bundle.STRINGS, "alone_in_channel", channel.getName())
        ).queue();
        manager.getPlayer(channel.getGuild()).getTrackScheduler().stop();
    }

}
