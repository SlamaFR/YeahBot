package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.managers.MusicManager;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.*;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created on 22/09/2018.
 */
public class MusicListener extends ListenerAdapter {

    private final MusicManager manager = YeahBot.getInstance().getMusicManager();
    private final Map<Guild, TaskScheduler> tasks = new HashMap<>();

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        check(event);
        checkDeafen(event);
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        check(event);
        checkDeafen(event);
        super.onGuildVoiceMove(event);
    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {
        checkDeafen(event);
        super.onGuildVoiceDeafen(event);
    }

    private void checkDeafen(GenericGuildVoiceEvent event) {
        Guild guild = event.getGuild();
        VoiceChannel channel = event.getVoiceState().getChannel();

        if (!event.getGuild().getAudioManager().isConnected()) return;
        if (channel != event.getGuild().getSelfMember().getVoiceState().getChannel()) return;

        List<Member> members = channel.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());
        List<Member> deafMembers = members.stream().filter(m -> m.getVoiceState().isDeafened()).collect(Collectors.toList());

        if (deafMembers.size() < members.size()) {
            resume(guild);
        } else {
            pause(guild);
        }
    }

    private void check(GuildVoiceUpdateEvent event) {
        VoiceChannel channel = event.getChannelLeft();
        TextChannel textChannel = manager.getPlayer(channel.getGuild()).getTextChannel();

        if (!event.getGuild().getAudioManager().isConnected()) return;

        List<Member> members = channel.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());

        boolean botLeft = event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong();
        boolean inCurrentChannel = event.getChannelLeft().equals(event.getGuild().getSelfMember().getVoiceState().getChannel());
        boolean botAlone = members.isEmpty();

        if (botLeft || (botAlone && inCurrentChannel)) {
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

    private void pause(Guild guild) {
        if (tasks.containsKey(guild)) tasks.remove(guild).stop();
        manager.getPlayer(guild).pause(false);
    }

    private void resume(Guild guild) {
        tasks.put(guild, TaskScheduler.scheduleDelayed(() -> {
            manager.getPlayer(guild).resume(false);
        }, 1000));
    }
}
