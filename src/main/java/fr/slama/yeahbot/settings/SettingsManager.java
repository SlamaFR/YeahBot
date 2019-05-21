package fr.slama.yeahbot.settings;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.LanguageUtil;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.EventWaiter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Created on 20/12/2018.
 */
public class SettingsManager {

    public static List<Field> getFields(Guild guild) {
        Settings settings = RedisData.getSettings(guild);
        List<Field> list = new ArrayList<>();
        for (Field field : settings.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(IgnoreSetting.class)) list.add(field);
        }
        return list;
    }

    public static void editField(Guild guild, TextChannel textChannel, User user, Field field) throws IllegalAccessException {
        if (field.getType().equals(boolean.class)) setBoolean(field, guild, textChannel, user);
        else if (field.getType().equals(String.class)) setString(field, guild, textChannel, user);
        else if (field.getType().equals(long.class)) {
            if (field.isAnnotationPresent(LongType.class)) {
                LongType type = field.getAnnotation(LongType.class);
                setLong(field, type.type(), guild, textChannel, user);
            } else {
                textChannel.sendMessage(
                        new EmbedBuilder()
                                .setColor(ColorUtil.RED)
                                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                                .setDescription(LanguageUtil.getString(guild, Bundle.ERROR, "something_went_wrong"))
                                .build()
                ).queue();
            }
        }
    }

    private static void setLong(Field field, LongType.Type type, Guild guild, TextChannel textChannel, User user) throws IllegalAccessException {
        Settings settings = RedisData.getSettings(guild);
        String value;
        Predicate<GuildMessageReceivedEvent> condition;
        switch (type) {
            case CHANNEL:
                if (guild.getTextChannelById(field.getLong(settings)) != null)
                    value = guild.getTextChannelById(field.getLong(settings)).getName() + " - " + guild.getTextChannelById(field.getLong(settings)).getAsMention();
                else value = LanguageUtil.getString(guild, Bundle.CAPTION, "none");
                condition = e -> e.getChannel().getIdLong() == textChannel.getIdLong() &&
                        e.getAuthor().getIdLong() == user.getIdLong() &&
                        (!e.getMessage().getMentionedChannels().isEmpty() ||
                                e.getMessage().getContentRaw().equalsIgnoreCase("cancel") ||
                                e.getMessage().getContentRaw().equalsIgnoreCase("reset"));
                break;
            case ROLE:
                if (guild.getRoleById(field.getLong(settings)) != null)
                    value = guild.getRoleById(field.getLong(settings)).getName() + " - " + guild.getRoleById(field.getLong(settings)).getAsMention();
                else value = LanguageUtil.getString(guild, Bundle.CAPTION, "none");
                condition = e -> e.getChannel().getIdLong() == textChannel.getIdLong() &&
                        e.getAuthor().getIdLong() == user.getIdLong() &&
                        (!e.getMessage().getMentionedRoles().isEmpty() ||
                                e.getMessage().getContentRaw().equalsIgnoreCase("cancel") ||
                                e.getMessage().getContentRaw().equalsIgnoreCase("reset"));
                break;
            default:
                return;
        }
        textChannel.sendMessage(
                new EmbedBuilder()
                        .addField(LanguageUtil.getString(guild, Bundle.SETTINGS, getSettingKey(field)),
                                LanguageUtil.getArguedString(guild, Bundle.STRINGS, "current_value", value),
                                false)
                        .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "waiting_for_response"), null)
                        .build()
        ).queue(message -> new EventWaiter(GuildMessageReceivedEvent.class,
                condition,
                e -> {
                    try {
                        if (e.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                            message.delete().queue();
                            e.getMessage().delete().queue();
                            textChannel.sendMessage(
                                    LanguageUtil.getString(guild, Bundle.STRINGS, "action_cancelled")
                            ).queue();
                            return;
                        } else if (e.getMessage().getContentRaw().equalsIgnoreCase("reset"))
                            field.setLong(settings, 0);
                        switch (type) {
                            case CHANNEL:
                                field.setLong(settings, e.getMessage().getMentionedChannels().get(0).getIdLong());
                                break;
                            case ROLE:
                                field.setLong(settings, e.getMessage().getMentionedRoles().get(0).getIdLong());
                                break;
                        }
                    } catch (IllegalAccessException e1) {
                        textChannel.sendMessage(
                                new EmbedBuilder()
                                        .setColor(ColorUtil.RED)
                                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                                        .setDescription(LanguageUtil.getString(guild, Bundle.ERROR, "something_went_wrong"))
                                        .build()
                        ).queue();
                        return;
                    }
                    RedisData.setSettings(guild, settings);
                    message.delete().queue();
                    e.getMessage().delete().queue();
                    textChannel.sendMessage(
                            LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                    ).queue();
                }));
    }

    private static void setBoolean(Field field, Guild guild, TextChannel textChannel, User user) throws IllegalAccessException {
        Settings settings = RedisData.getSettings(guild);
        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.SETTINGS, getSettingKey(field)))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "current_value",
                                LanguageUtil.getState(guild, field.getBoolean(settings))))
                        .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "thirty_seconds_expiration"), null)
                        .build()
        ).queue(message -> {
            List<String> choices = EmoteUtil.getQuestionEmotes();
            for (String emote : choices)
                message.addReaction(YeahBot.getInstance().getShardManager().getEmoteById(emote)).queue();
            new EventWaiter(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == user.getIdLong() &&
                            e.getMessageIdLong() == message.getIdLong() &&
                            choices.contains(e.getReactionEmote().getId()),
                    e -> {
                        try {
                            if (e.getReactionEmote().getId().equals(EmoteUtil.NO_EMOTE)) {
                                field.set(settings, false);
                            } else if (e.getReactionEmote().getId().equals(EmoteUtil.YES_EMOTE)) {
                                field.set(settings, true);
                            }
                        } catch (IllegalAccessException ignored) {
                        }
                        RedisData.setSettings(guild, settings);
                        message.delete().queue();
                        textChannel.sendMessage(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                        ).queue();
                    }, 30, TimeUnit.SECONDS, () -> message.delete().queue());
        });
    }

    private static void setString(Field field, Guild guild, TextChannel textChannel, User user) throws IllegalAccessException {
        Settings settings = RedisData.getSettings(guild);
        String value = (String) field.get(settings);
        if (value.isEmpty()) value = LanguageUtil.getString(guild, Bundle.STRINGS, getSettingKey(field));

        EmbedBuilder builder = new EmbedBuilder()
                .addField(LanguageUtil.getString(guild, Bundle.SETTINGS, getSettingKey(field)),
                        LanguageUtil.getArguedString(guild, Bundle.STRINGS, "current_value", value),
                        false)
                .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "waiting_for_response"), null);

        if (field.isAnnotationPresent(AvailableVariables.class)) {
            StringBuilder stringBuilder = new StringBuilder();

            for (AvailableVariables.Variables variables : field.getAnnotation(AvailableVariables.class).variables()) {
                if (stringBuilder.length() > 0) stringBuilder.append("\n");
                stringBuilder.append(String.format("● `%s` %s", variables.getVar(), LanguageUtil.getString(guild, Bundle.DESCRIPTION, variables.getKey())));
            }

            builder.addField(
                    LanguageUtil.getString(guild, Bundle.CAPTION, "available_variables"),
                    stringBuilder.toString(),
                    false
            );
        }

        textChannel.sendMessage(builder.build()).queue(message -> {
            new EventWaiter(GuildMessageReceivedEvent.class,
                    e -> e.getGuild().getIdLong() == guild.getIdLong() &&
                            e.getChannel().getIdLong() == textChannel.getIdLong() &&
                            e.getAuthor().getIdLong() == user.getIdLong(),
                    e -> {
                        String newValue = e.getMessage().getContentRaw();
                        try {
                            if (newValue.equalsIgnoreCase("cancel")) {
                                message.delete().queue();
                                e.getMessage().delete().queue();
                                textChannel.sendMessage(
                                        LanguageUtil.getString(guild, Bundle.STRINGS, "action_cancelled")
                                ).queue();
                                return;
                            } else if (newValue.equalsIgnoreCase("reset")) field.set(settings, "");
                            else field.set(settings, newValue);
                        } catch (IllegalAccessException e1) {
                            textChannel.sendMessage(
                                    new EmbedBuilder()
                                            .setColor(ColorUtil.RED)
                                            .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                                            .setDescription(LanguageUtil.getString(guild, Bundle.ERROR, "something_went_wrong"))
                                            .build()
                            ).queue();
                            return;
                        }
                        RedisData.setSettings(guild, settings);
                        message.delete().queue();
                        e.getMessage().delete().queue();
                        textChannel.sendMessage(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "setting_updated")
                        ).queue();
                    });
        });
    }

    public static String getSettingKey(Field field) {
        String fieldName = field.getName();
        StringBuilder builder = new StringBuilder();
        for (char c : fieldName.toCharArray()) {
            if (Character.isUpperCase(c)) builder.append("_");
            builder.append(c);
        }
        return builder.toString().toLowerCase();
    }

}
