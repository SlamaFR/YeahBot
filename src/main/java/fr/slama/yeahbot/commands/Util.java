package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.PrivateChannels;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.EventWaiter;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 09/09/2018.
 */
public class Util {

    private final static List<String> colors = Arrays.stream(ColorUtil.class.getDeclaredFields())
            .map(f -> f.getName().toLowerCase())
            .collect(Collectors.toList());

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
                        .setColor(ColorUtil.GREEN)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "private_channel_created"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "private_channel_identifier", channels.getChannels().size()))
                        .build()).queue();

            });


        } else {
            textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.ERROR, "max_private_voice_channel_reached")).queue();
        }

    }

    @Command(name = "embed",
            discordPermission = {Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE},
            category = Command.CommandCategory.UTIL,
            permission = Command.CommandPermission.STAFF,
            executor = Command.CommandExecutor.USER)
    private void embed(Guild guild, TextChannel textChannel, Member member, Message message) {

        if (guild == null) return;

        message.delete().queue();

        MessageEmbed waitingEmbed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "building_embed"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "embed_builder_description"))
                .addField(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "embed_builder_directions_list"),
                        LanguageUtil.getString(guild, Bundle.STRINGS, "embed_builder_directions_list"),
                        true
                )
                .addField(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "embed_builder_colors_list"),
                        String.format("`%s`", String.join(", ", colors)),
                        true
                )
                .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "waiting_for_response_embed"), null)
                .build();

        EmbedBuilder embed = new EmbedBuilder();

        textChannel.sendMessage(waitingEmbed).queue(waiting -> textChannel.sendMessage(new EmbedBuilder()
                .setDescription(EmbedBuilder.ZERO_WIDTH_SPACE)
                .build()).queue(msg -> new EventWaiter.Builder(GuildMessageReceivedEvent.class,
                e -> e.getAuthor().getIdLong() == member.getUser().getIdLong(),
                (e, ew) -> {
                    String[] args = e.getMessage().getContentRaw().split(":", 2);
                    if (args.length >= 1) {
                        String attribute = args[0];

                        if (attribute.equalsIgnoreCase("cancel")) {
                            textChannel.deleteMessages(Arrays.asList(e.getMessage(), waiting, msg)).queue();
                            ew.close();
                            return;
                        } else if (e.getMessage().getContentRaw().toLowerCase().startsWith("finish")) {
                            if (!e.getMessage().getMentionedChannels().isEmpty()) {
                                if (!embed.isEmpty())
                                    e.getMessage().getMentionedChannels().get(0).sendMessage(embed.setAuthor(
                                            member.getEffectiveName(), null, member.getUser().getAvatarUrl()
                                    ).build()).queue();
                            } else {
                                if (!embed.isEmpty()) textChannel.sendMessage(embed.setAuthor(
                                        member.getEffectiveName(), null, member.getUser().getAvatarUrl()
                                ).build()).queue();
                            }
                            textChannel.deleteMessages(Arrays.asList(e.getMessage(), waiting, msg)).queue();
                            ew.close();
                            return;
                        }

                        if (args.length >= 2) {
                            String arguments = String.join("", Arrays.copyOfRange(args, 1, args.length));

                            EmbedBuilder builder = new EmbedBuilder(handleResponse(attribute, arguments, embed));
                            if (builder.isEmpty()) builder.setDescription(EmbedBuilder.ZERO_WIDTH_SPACE);
                            msg.editMessage(builder.build()).queue();
                        }
                    }
                    e.getMessage().delete().queue();
                })
                .autoClose(false)
                .build()));
    }

    private EmbedBuilder handleResponse(String attribute, String arguments, EmbedBuilder embed) {
        String[] args = arguments.split("\\|\\|");

        try {
            switch (attribute.toLowerCase()) {
                case "title":
                    embed.setTitle(args[0]);
                    return embed;
                case "description":
                case "desc":
                    embed.setDescription(args[0]);
                    return embed;
                case "color":
                    return embed.setColor((Color) ColorUtil.class.getField(args[0].toUpperCase()).get(ColorUtil.class));
                case "field":
                    if (args.length >= 3) {
                        if (args[2].equalsIgnoreCase("y")) {
                            embed.addField(args[0], args[1], true);
                            return embed;
                        } else if (args[2].equalsIgnoreCase("n")) {
                            embed.addField(args[0], args[1], false);
                            return embed;
                        }
                    }
                    return embed;
                case "image":
                    embed.setImage(args[0]);
                    return embed;
                case "thumbnail":
                case "thumb":
                    embed.setThumbnail(args[0]);
                    return embed;
                case "footer":
                    embed.setFooter(args[0], null);
                    return embed;
                default:
                    return embed;
            }
        } catch (Exception e) {
            return embed;
        }
    }

}
