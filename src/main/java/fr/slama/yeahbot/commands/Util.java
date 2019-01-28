package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.PrivateChannels;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.awt.*;

/**
 * Created on 09/09/2018.
 */
public class Util {

    @Command(name = "pvchan",
            discordPermission = {Permission.MANAGE_PERMISSIONS, Permission.MANAGE_CHANNEL},
            category = Command.CommandCategory.UTIL,
            executor = Command.CommandExecutor.USER)
    private void privateVoiceChannel(Guild guild, TextChannel textChannel, Member member, Message message) {

        if (guild == null) return;

        PrivateChannels channels = RedisData.getPrivateChannels(guild);

        if (channels.getChannels().size() < 10) {

            long permissionOverride = Permission.getRaw(Permission.VOICE_CONNECT, Permission.VIEW_CHANNEL);

            guild.getController().createVoiceChannel(LanguageUtil.getArguedString(guild, Bundle.CAPTION, "private_channel_name", channels.getChannels().size() + 1))
                    .addPermissionOverride(guild.getPublicRole(), 0L, permissionOverride)
                    .addPermissionOverride(guild.getSelfMember(), permissionOverride, 0L)
                    .addPermissionOverride(member, permissionOverride, 0L).queue(channel -> {

                message.getMentionedMembers().forEach(m -> channel.createPermissionOverride(m).setAllow(permissionOverride).queue());
                channels.getChannels().add(channel.getIdLong());
                RedisData.setPrivateChannels(guild, channels);

                textChannel.sendMessage(new EmbedBuilder()
                        .setColor(new Color(46, 204, 113))
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "private_channel_created"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "private_channel_identifier", channels.getChannels().size()))
                        .build()).queue();

            });


        } else {
            textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.ERROR, "max_private_voice_channel_reached")).queue();
        }

    }

}
