package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.managers.MusicManager;
import fr.slama.yeahbot.music.MusicPlayer;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
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
    private final Map<Guild, TaskScheduler> leavingTasks = new HashMap<>();
    private final Map<Guild, Long> leavingMessages = new HashMap<>();

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        check(event);
        if (!isBot(event)) checkDeafen(event);
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        check(event);
        checkDeafen(event);
        super.onGuildVoiceJoin(event);
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

    private boolean isBot(GenericGuildVoiceEvent event) {
        return event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong();
    }

    private void checkDeafen(GenericGuildVoiceEvent event) {
        if (!event.getGuild().getAudioManager().isConnected()) return;

        Guild guild = event.getGuild();
        VoiceChannel channel = event.getVoiceState().getChannel();
        MusicPlayer player = manager.getPlayer(guild);

        if (channel != event.getGuild().getSelfMember().getVoiceState().getChannel()) return;
        if (player.getTrackScheduler().isUserPaused()) return;

        List<Member> members = channel.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());
        List<Member> deafMembers = members.stream().filter(m -> m.getVoiceState().isDeafened()).collect(Collectors.toList());

        if (deafMembers.size() < members.size()) {
            player.getTrackScheduler().resume(false);
        } else {
            player.getTrackScheduler().pause(false);
        }
    }

    private void check(GenericGuildVoiceEvent event) {
        assert event.getGuild().getAudioManager() != null;
        if (!event.getGuild().getAudioManager().isConnected()) return;

        VoiceChannel channel = event.getGuild().getSelfMember().getVoiceState().getChannel();
        TextChannel textChannel = manager.getPlayer(channel.getGuild()).getTextChannel();

        List<Member> members = channel.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());

        boolean botLeft = event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong();
        boolean inCurrentChannel = channel.equals(event.getGuild().getSelfMember().getVoiceState().getChannel());
        boolean botAlone = members.isEmpty();

        if (botLeft) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(channel.getGuild(), Bundle.CAPTION, "audio_player"))
                            .setDescription(LanguageUtil.getString(channel.getGuild(), Bundle.STRINGS, "stopping_music"))
                            .setColor(ColorUtil.ORANGE)
                            .build()
            ).queue();
            manager.getPlayer(channel.getGuild()).getTrackScheduler().stop();
        } else if (inCurrentChannel) {
            if (botAlone) {
                waitForLeaving(textChannel);
            } else {
                cancelLeaving(textChannel);
            }
        }
    }

    private void waitForLeaving(TextChannel textChannel) {
        Guild guild = textChannel.getGuild();
        MessageEmbed embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "audio_player"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "waiting_for_leaving"))
                .setColor(ColorUtil.YELLOW)
                .build();

        textChannel.sendMessage(embed).queue(message -> leavingMessages.put(guild, message.getIdLong()));

        manager.getPlayer(guild).getTrackScheduler().pause(false);
        leavingTasks.put(guild, TaskScheduler.scheduleDelayed(() -> {
            if (leavingMessages.containsKey(guild)) tell(textChannel, leavingMessages.get(guild));
            manager.getPlayer(guild).getTrackScheduler().stop();
            manager.getPlayer(guild).getTrackScheduler().resume(false);
        }, 60 * 1000));
    }

    private void cancelLeaving(TextChannel textChannel) {
        Guild guild = textChannel.getGuild();

        if (leavingTasks.containsKey(guild)) leavingTasks.remove(guild).stop();
        if (leavingMessages.containsKey(guild))
            textChannel.getMessageById(leavingMessages.remove(guild)).queue(message -> message.delete().queue());
        manager.getPlayer(guild).getTrackScheduler().resume(false);
    }

    private void tell(TextChannel textChannel, long messageId) {
        Guild guild = textChannel.getGuild();
        MessageEmbed embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "audio_player"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "stopping_music"))
                .setColor(ColorUtil.ORANGE)
                .build();

        if (textChannel.getLatestMessageIdLong() == messageId) {
            textChannel.getMessageById(messageId).queue(message -> message.editMessage(embed).queue(), t -> {
            });
        } else {
            textChannel.getMessageById(messageId).queue(message -> message.delete().queue(), t -> {
            });
            textChannel.sendMessage(embed).queue();
        }
    }
}
