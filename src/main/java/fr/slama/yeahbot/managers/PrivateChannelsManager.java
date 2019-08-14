package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.blub.TaskScheduler;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Channels;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 15/11/2018.
 */
public class PrivateChannelsManager extends ListenerAdapter {

    private static Map<Long, TaskScheduler> cancellingTasks = new HashMap<>();

    private static void deleteChannel(Guild guild, VoiceChannel voiceChannel) {
        Channels channels = RedisData.getPrivateChannels(guild);

        if (channels.getChannels().contains(voiceChannel.getIdLong())) {
            voiceChannel.delete().queue(channel -> {
                channels.getChannels().remove(voiceChannel.getIdLong());
                RedisData.setPrivateChannels(guild, channels);
            });
        }
    }

    public static void createChannel(Message message) {
        Guild guild = message.getGuild();
        TextChannel textChannel = message.getTextChannel();
        Member member = message.getMember();
        Channels channels = RedisData.getPrivateChannels(guild);

        long permissionOverride = Permission.getRaw(Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL);
        int number = channels.getChannels().size() + 1;
        guild.createVoiceChannel(LanguageUtil.getArguedString(guild, Bundle.CAPTION, "private_channel_name", number))
                .addPermissionOverride(guild.getPublicRole(), 0L, permissionOverride)
                .addPermissionOverride(guild.getSelfMember(), permissionOverride, 0L)
                .addPermissionOverride(member, permissionOverride, 0L).queue(channel -> {

            message.getMentionedMembers().forEach(m -> channel.createPermissionOverride(m).setAllow(permissionOverride).queue());
            channels.getChannels().add(channel.getIdLong());
            RedisData.setPrivateChannels(guild, channels);

            cancellingTasks.put(channel.getIdLong(), TaskScheduler.scheduleDelayed(() -> {
                deleteChannel(guild, channel);
                textChannel.sendMessage(
                        new EmbedBuilder()
                                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "private_channel"))
                                .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "idle_private_channel_deleted", number))
                                .setColor(ColorUtil.YELLOW)
                                .build()
                ).queue();
            }, 60 * 1000));

            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.GREEN)
                    .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "private_channel_created"))
                    .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "private_channel_identifier", channels.getChannels().size()))
                    .build()).queue();

        });
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        deleteChannel(event.getGuild(), event.getChannelLeft());
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        deleteChannel(event.getGuild(), event.getChannelLeft());
        checkActivity(event.getChannelJoined());
        super.onGuildVoiceMove(event);
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        checkActivity(event.getChannelJoined());
        super.onGuildVoiceJoin(event);
    }

    private void checkActivity(VoiceChannel channel) {
        if (cancellingTasks.containsKey(channel.getIdLong()))
            cancellingTasks.remove(channel.getIdLong()).stop();
    }

}
