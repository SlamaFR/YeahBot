package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.PrivateChannels;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.EventWaiter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private void embed(Guild guild, TextChannel textChannel, Message message, User user) {

        if (guild == null) return;

        message.delete().queue();
        List<Message> messages = new ArrayList<>();

        List<String> attributes = Arrays.asList(
                "title",
                "description",
                "color",
                "field",
                "author",
                "image",
                "thumbnail",
                "footer"
        );

        MessageEmbed waitingEmbed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "building_embed"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "embed_builder_description"))
                .addField(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "embed_builder_directions_list"),
                        LanguageUtil.getString(guild, Bundle.STRINGS, "embed_builder_directions_list"),
                        true
                )
                .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "waiting_for_response_embed"), null)
                .build();

        EmbedBuilder embed = new EmbedBuilder();

        textChannel.sendMessage(waitingEmbed).queue(waiting -> {
            textChannel.sendMessage(new EmbedBuilder().addBlankField(false).build()).queue(msg -> new EventWaiter(
                    GuildMessageReceivedEvent.class,
                    e -> true,
                    (e, ew) -> {
                        try {
                            String[] args = e.getMessage().getContentRaw().split(":", 2);
                            String attribute = args[0];

                            if (attribute.equalsIgnoreCase("cancel")) {
                                textChannel.deleteMessages(Arrays.asList(e.getMessage(), waiting, msg)).queue();
                                ew.close();
                                return;
                            } else if (attribute.equalsIgnoreCase("finish")) {
                                textChannel.deleteMessages(Arrays.asList(e.getMessage(), waiting, msg)).queue();
                                ew.close();
                                if (!embed.isEmpty()) textChannel.sendMessage(embed.build()).queue();
                                return;
                            }

                            e.getMessage().delete().queue();
                            String arguments = String.join("", Arrays.copyOfRange(args, 1, args.length));

                            if (!attributes.contains(attribute)) return;

                            EmbedBuilder builder = new EmbedBuilder(handleResponse(attribute, arguments, embed));
                            if (builder.isEmpty()) builder.addBlankField(false);
                            msg.editMessage(builder.build()).queue();

                        } catch (IndexOutOfBoundsException ex) {
                            //TODO: ignore
                        }
                    }, false));
        });

    }

    private EmbedBuilder handleResponse(String attribute, String arguments, EmbedBuilder embed) throws IndexOutOfBoundsException {
        String[] args = arguments.split("\\|\\|");

        switch (attribute.toLowerCase()) {
            case "title":
                embed.setTitle(args[0]);
                break;
            case "description":
                embed.setDescription(args[0]);
                break;
            case "color":
                try {
                    embed.setColor((Color) ColorUtil.class.getField(args[0].toUpperCase()).get(ColorUtil.class));
                } catch (NoSuchFieldException | IllegalAccessException ignored) {
                }
                break;
            case "field":
                if (args.length >= 3) {
                    if (args[2].equalsIgnoreCase("y")) {
                        embed.addField(args[0], args[1], true);
                        break;
                    } else if (args[2].equalsIgnoreCase("n")) {
                        embed.addField(args[0], args[1], false);
                        break;
                    }
                }
                break;
            case "author":
                if (args.length >= 1) {
                    String author = args[0];
                    if (args.length >= 2) {

                        String url;
                        try {
                            new URL(args[1]);
                            url = args[1];
                        } catch (MalformedURLException e) {
                            url = null;
                        }

                        if (args.length == 3) {

                            String iconUrl;
                            try {
                                new URL(args[2]);
                                iconUrl = args[2];
                            } catch (MalformedURLException e) {
                                iconUrl = null;
                            }

                            embed.setAuthor(author, url, iconUrl);
                        } else {
                            embed.setAuthor(author, url);
                        }
                    } else {
                        embed.setAuthor(author);
                    }
                }
                break;
            case "image":
                embed.setImage(args[0]);
                break;
            case "thumbnail":
                embed.setThumbnail(args[0]);
                break;
            case "footer":
                if (args.length >= 1) {
                    String footer = args[0];
                    String url;
                    try {
                        new URL(args[1]);
                        url = args[1];
                    } catch (MalformedURLException | IndexOutOfBoundsException e) {
                        url = null;
                    }
                    embed.setFooter(footer, url);
                }
                break;
            default:
                return embed;
        }

        return embed;
    }

}
