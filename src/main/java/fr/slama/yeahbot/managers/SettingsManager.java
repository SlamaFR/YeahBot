package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.settings.AvailableVariables;
import fr.slama.yeahbot.settings.IgnoreSetting;
import fr.slama.yeahbot.settings.LongType;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.blub.EventWaiter;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created on 20/12/2018.
 */
public class SettingsManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(SettingsManager.class);
    private final static Consumer<? super Object> SUCCESS = s -> {
    };
    private final static Consumer<? super Throwable> FAILURE = f -> {
    };

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
        else if (field.getType().equals(long.class) && field.isAnnotationPresent(LongType.class)) {
            LongType type = field.getAnnotation(LongType.class);
            setLong(field, type.type(), guild, textChannel, user);
        } else {
            LOGGER.error("[FATAL] Impossible to set \"" + field.getName() + "\" setting!");
            sendErrorEmbed(textChannel);
        }
    }

    private static void setLong(Field field, LongType.Type type, Guild guild, TextChannel textChannel, User user) throws IllegalAccessException {
        Settings settings = RedisData.getSettings(guild);
        String value;
        Predicate<GuildMessageReceivedEvent> condition;

        switch (type) {
            case CHANNEL:
                if (guild.getTextChannelById(field.getLong(settings)) != null) {
                    value = getLongValue(guild.getTextChannelById(field.getLong(settings)));
                } else {
                    value = LanguageUtil.getString(guild, Bundle.CAPTION, "none");
                }
                condition = e -> getCondition(e, textChannel, user, !e.getMessage().getMentionedChannels().isEmpty());
                break;
            case ROLE:
                if (guild.getRoleById(field.getLong(settings)) != null) {
                    value = getLongValue(guild.getRoleById(field.getLong(settings)));
                } else {
                    value = LanguageUtil.getString(guild, Bundle.CAPTION, "none");
                }
                condition = e -> getCondition(e, textChannel, user, !e.getMessage().getMentionedRoles().isEmpty());
                break;
            default:
                return;
        }

        textChannel.sendMessage(
                new EmbedBuilder()
                        .addField(
                                LanguageUtil.getString(guild, Bundle.SETTINGS, getSettingKey(field)),
                                LanguageUtil.getArguedString(guild, Bundle.STRINGS, "current_value", value),
                                false
                        )
                        .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "waiting_for_response"), null)
                        .build()
        ).queue(message -> new EventWaiter.Builder(GuildMessageReceivedEvent.class, condition, (e, ew) -> {
            String newValue = LanguageUtil.getString(guild, Bundle.CAPTION, "none");

            try {
                if (e.getMessage().getContentRaw().equalsIgnoreCase("cancel")) {
                    cancelAction(e, guild, textChannel, message);
                    return;
                } else if (e.getMessage().getContentRaw().equalsIgnoreCase("reset")) {
                    field.setLong(settings, 0);
                } else switch (type) {
                    case CHANNEL:
                        newValue = getLongValue(e.getMessage().getMentionedChannels().get(0));
                        field.setLong(settings, e.getMessage().getMentionedChannels().get(0).getIdLong());
                        break;
                    case ROLE:
                        newValue = getLongValue(e.getMessage().getMentionedRoles().get(0));
                        field.setLong(settings, e.getMessage().getMentionedRoles().get(0).getIdLong());
                        break;
                    default:
                        return;
                }
            } catch (Exception e1) {
                sendErrorEmbed(textChannel);
                return;
            }

            RedisData.setSettings(guild, settings);
            textChannel.deleteMessages(Arrays.asList(message, e.getMessage())).queue(SUCCESS, FAILURE);
            sendUpdateEmbed(textChannel, field, value, newValue);
        }).build(), FAILURE);
    }

    private static void setBoolean(Field field, Guild guild, TextChannel textChannel, User user) throws IllegalAccessException {
        Settings settings = RedisData.getSettings(guild);

        final String oldValue = LanguageUtil.getState(guild, field.getBoolean(settings));

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
                message.addReaction(YeahBot.getInstance().getShardManager().getEmoteById(emote)).queue(SUCCESS, FAILURE);
            new EventWaiter.Builder(MessageReactionAddEvent.class, e -> e.getUser().getIdLong() == user.getIdLong() &&
                    e.getMessageIdLong() == message.getIdLong() &&
                    choices.contains(e.getReactionEmote().getId()),
                    (e, ew) -> {
                        String newValue;
                        try {
                            if (e.getReactionEmote().getId().equals(EmoteUtil.NO_EMOTE)) {
                                field.set(settings, false);
                                newValue = LanguageUtil.getState(guild, false);
                            } else if (e.getReactionEmote().getId().equals(EmoteUtil.YES_EMOTE)) {
                                field.set(settings, true);
                                newValue = LanguageUtil.getState(guild, true);
                            } else {
                                return;
                            }
                        } catch (IllegalAccessException ignored) {
                            sendErrorEmbed(textChannel);
                            return;
                        }
                        RedisData.setSettings(guild, settings);
                        message.delete().queue(SUCCESS, FAILURE);
                        sendUpdateEmbed(textChannel, field, oldValue, newValue);
                    })
                    .timeout(30, TimeUnit.SECONDS)
                    .timeoutAction(() -> message.delete().queue(SUCCESS, FAILURE))
                    .build();
        }, FAILURE);
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

        String oldValue = value;
        textChannel.sendMessage(builder.build()).queue(message -> new EventWaiter.Builder(GuildMessageReceivedEvent.class,
                e -> getCondition(e, textChannel, user),
                (e, ew) -> {
                    String newValue = e.getMessage().getContentRaw();
                    try {
                        if (newValue.equalsIgnoreCase("cancel")) {
                            textChannel.deleteMessages(Arrays.asList(message, e.getMessage())).queue(SUCCESS, FAILURE);
                            textChannel.sendMessage(
                                    LanguageUtil.getString(guild, Bundle.STRINGS, "action_cancelled")
                            ).queue(SUCCESS, FAILURE);
                            return;
                        }

                        if (newValue.equalsIgnoreCase("reset")) {
                            newValue = LanguageUtil.getString(guild, Bundle.STRINGS, getSettingKey(field));
                            field.set(settings, "");
                        } else {
                            field.set(settings, newValue);
                        }
                    } catch (IllegalAccessException e1) {
                        sendErrorEmbed(textChannel);
                        return;
                    }
                    RedisData.setSettings(guild, settings);
                    textChannel.deleteMessages(Arrays.asList(message, e.getMessage())).queue(SUCCESS, FAILURE);
                    sendUpdateEmbed(textChannel, field, oldValue, newValue);
                }).build(), FAILURE);
    }

    private static String getLongValue(TextChannel m) {
        return String.format("%s - %s", m.getName(), m.getAsMention());
    }

    private static String getLongValue(Role m) {
        return String.format("%s - %s", m.getName(), m.getAsMention());
    }

    private static void sendUpdateEmbed(TextChannel textChannel, Field field, String oldValue, String newValue) {
        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "success"))
                        .setDescription(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "setting_updated"))
                        .addField(
                                LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "old_value"),
                                String.format("```\n%s\n```", oldValue),
                                false
                        )
                        .addField(
                                LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "new_value"),
                                String.format("```\n%s\n```", newValue),
                                false
                        )
                        .setFooter(LanguageUtil.getString(textChannel.getGuild(), Bundle.SETTINGS, getSettingKey(field)), null)
                        .setColor(ColorUtil.GREEN)
                        .build()
        ).queue(SUCCESS, FAILURE);
    }

    public static void sendErrorEmbed(TextChannel textChannel) {
        textChannel.sendMessage(
                new EmbedBuilder()
                        .setColor(ColorUtil.RED)
                        .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "error"))
                        .setDescription(LanguageUtil.getString(textChannel.getGuild(), Bundle.ERROR, "something_went_wrong"))
                        .build()
        ).queue(SUCCESS, FAILURE);
    }

    private static void cancelAction(GuildMessageReceivedEvent e, Guild guild, TextChannel textChannel, Message message) {
        textChannel.deleteMessages(Arrays.asList(message, e.getMessage())).queue(SUCCESS, FAILURE);
        textChannel.sendMessage(
                LanguageUtil.getString(guild, Bundle.STRINGS, "action_cancelled")
        ).queue(SUCCESS, FAILURE);
    }

    private static boolean getCondition(GuildMessageReceivedEvent e, TextChannel textChannel, User user) {
        return getCondition(e, textChannel, user, true);
    }

    private static boolean getCondition(GuildMessageReceivedEvent e, TextChannel textChannel, User user, boolean customCondition) {
        boolean inCorrectChannel = e.getChannel().getIdLong() == textChannel.getIdLong();
        boolean fromCorrectUser = e.getAuthor().getIdLong() == user.getIdLong();
        boolean isMessageCorrect = (customCondition ||
                e.getMessage().getContentRaw().equalsIgnoreCase("cancel") ||
                e.getMessage().getContentRaw().equalsIgnoreCase("reset"));
        return inCorrectChannel && fromCorrectUser && isMessageCorrect;
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
