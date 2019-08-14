package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.managers.MusicManager;
import fr.slama.yeahbot.music.MusicPlayer;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created on 22/09/2018.
 */
public class MusicListener extends ListenerAdapter {

    private final MusicManager manager = YeahBot.getInstance().getMusicManager();
    private final Map<Long, TaskScheduler> leavingTasks = new HashMap<>();
    private final Map<Long, Long> leavingMessages = new HashMap<>();

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        if (!isBot(event)) {
            check(event.getChannelLeft(), event);
            checkDeafen(event.getChannelLeft(), event);
        } else if (manager.getPlayer(event.getGuild()).getTextChannel() != null) {
            leave(manager.getPlayer(event.getGuild()).getTextChannel());
        }
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        check(event.getChannelJoined(), event);
        checkDeafen(event.getChannelJoined(), event);
        super.onGuildVoiceJoin(event);
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        check(event.getChannelJoined(), event);
        check(event.getChannelLeft(), event);
        checkDeafen(event.getChannelJoined(), event);
        checkDeafen(event.getChannelLeft(), event);
        super.onGuildVoiceMove(event);
    }

    @Override
    public void onGuildVoiceDeafen(@NotNull GuildVoiceDeafenEvent event) {
        checkDeafen(event.getVoiceState().getChannel(), event);
        super.onGuildVoiceDeafen(event);
    }

    private boolean isBot(GenericGuildVoiceEvent event) {
        return event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong();
    }

    private void checkDeafen(VoiceChannel channel, GenericGuildVoiceEvent event) {
        if (!event.getGuild().getAudioManager().isConnected()) return;

        Guild guild = event.getGuild();
        MusicPlayer player = manager.getPlayer(guild);

        if (channel.getIdLong() != event.getGuild().getSelfMember().getVoiceState().getChannel().getIdLong()) return;
        if (player.getTrackScheduler().isUserPaused()) return;

        List<Member> members = channel.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());
        List<Member> deafMembers = members.stream().filter(m -> m.getVoiceState().isDeafened()).collect(Collectors.toList());

        if (deafMembers.size() < members.size()) {
            player.getTrackScheduler().resume(false);
        } else {
            player.getTrackScheduler().pause(false);
        }
    }

    private void check(VoiceChannel channel, GenericGuildVoiceEvent event) {
        if (!event.getGuild().getAudioManager().isConnected()) return;

        VoiceChannel botChannel = event.getGuild().getSelfMember().getVoiceState().getChannel();
        TextChannel textChannel = manager.getPlayer(channel.getGuild()).getTextChannel();

        if (botChannel == null) return;

        List<Member> members = channel.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());

        boolean botLeft = event.getMember().getUser().getIdLong() == event.getJDA().getSelfUser().getIdLong();
        boolean inCurrentChannel = channel.getIdLong() == botChannel.getIdLong();
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

        textChannel.sendMessage(embed).queue(message -> leavingMessages.put(guild.getIdLong(), message.getIdLong()));

        manager.getPlayer(guild).getTrackScheduler().pause(false);
        leavingTasks.put(guild.getIdLong(), TaskScheduler.scheduleDelayed(() -> leave(textChannel), 60 * 1000));
    }

    private void cancelLeaving(TextChannel textChannel) {
        Guild guild = textChannel.getGuild();

        if (leavingTasks.containsKey(guild.getIdLong())) leavingTasks.remove(guild.getIdLong()).stop();
        if (leavingMessages.containsKey(guild.getIdLong()))
            textChannel.retrieveMessageById(leavingMessages.remove(guild.getIdLong())).queue(message -> message.delete().queue());
        manager.getPlayer(guild).getTrackScheduler().resume(false);
    }

    private void tell(TextChannel textChannel, long messageId) {
        Guild guild = textChannel.getGuild();
        MessageEmbed embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "audio_player"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "stopping_music"))
                .setColor(ColorUtil.ORANGE)
                .build();

        if (textChannel.hasLatestMessage() && textChannel.getLatestMessageIdLong() == messageId) {
            textChannel.retrieveMessageById(messageId).queue(message -> message.editMessage(embed).queue(), t -> {
            });
        } else {
            textChannel.retrieveMessageById(messageId).queue(message -> message.delete().queue(), t -> {
            });
            textChannel.sendMessage(embed).queue();
        }
    }

    public void leave(TextChannel textChannel) {
        Guild guild = textChannel.getGuild();
        tell(textChannel, leavingMessages.containsKey(guild.getIdLong()) ? leavingMessages.remove(guild.getIdLong()) : 0);
        if (leavingTasks.containsKey(guild.getIdLong())) leavingTasks.remove(guild.getIdLong()).stop();
        manager.getPlayer(guild).getTrackScheduler().resume(false);
        manager.getPlayer(guild).getTrackScheduler().stop();
    }

    public Map<Long, TaskScheduler> getLeavingTasks() {
        return leavingTasks;
    }
}
