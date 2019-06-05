package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.managers.MusicManager;
import fr.slama.yeahbot.utilities.ColorUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Created on 22/09/2018.
 */
public class MusicListener extends ListenerAdapter {

    private final MusicManager manager = YeahBot.getInstance().getMusicManager();

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        check(event);
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        check(event);
        super.onGuildVoiceMove(event);
    }

    private void check(GuildVoiceUpdateEvent event) {
        VoiceChannel channel = event.getChannelLeft();
        TextChannel textChannel = manager.getPlayer(channel.getGuild()).getTextChannel();

        if (!event.getGuild().getAudioManager().isConnected()) return;

        if (event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong() ||
                (event.getChannelLeft().equals(event.getGuild().getSelfMember().getVoiceState().getChannel()) &&
                        event.getChannelLeft().getMembers().stream().filter(m -> !m.getUser().isBot()).toArray().length < 1)) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(channel.getGuild(), Bundle.CAPTION, "audio_player"))
                            .setDescription(LanguageUtil.getString(channel.getGuild(), Bundle.STRINGS, "stopping_music"))
                            .setColor(ColorUtil.ORANGE)
                            .build()
            ).queue();
            manager.getPlayer(channel.getGuild()).getTrackScheduler().stop();
        }
    }
}
