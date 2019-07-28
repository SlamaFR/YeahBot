package fr.slama.yeahbot.blub;

import com.google.common.collect.Sets;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandMap;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.listeners.SelectionListener;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.utils.Checks;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created on 2019-07-26.
 */
public class SetupWizard {

    private static final String REFRESH_EMOTE = "\uD83D\uDD04";
    private static final String SUBMIT_EMOTE = "✅";

    private static final Set<Permission> NECESSARY_PERMISSION = Sets.newHashSet(
            Permission.MESSAGE_MANAGE, Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_READ,
            Permission.MESSAGE_HISTORY, Permission.VOICE_SPEAK, Permission.VOICE_CONNECT, Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_EXT_EMOJI
    );
    private static final Set<Permission> MODERATION_PERMISSION = Sets.newHashSet(
            Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS, Permission.MANAGE_CHANNEL,
            Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS
    );
    private static final Set<Permission> MUSIC_PERMISSION = Sets.newHashSet(
            Permission.VOICE_SPEAK, Permission.VOICE_CONNECT, Permission.MESSAGE_ADD_REACTION
    );
    private static final Set<Permission> UTIL_PERMISSION = Sets.newHashSet(
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_CHANNEL
    );
    private static final Set<Permission> FUN_PERMISSION = Sets.newHashSet(
            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE
    );
    private static final Set<Permission> MISCELLANEOUS_PERMISSION = Sets.newHashSet(
            Permission.MESSAGE_ADD_REACTION, Permission.MANAGE_CHANNEL, Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY
    );

    private final Guild guild;
    private final TextChannel textChannel;
    private final Member member;
    private final Settings settings;

    private long permissionMessage = 0;

    public SetupWizard(TextChannel textChannel, Member member) {
        this.guild = member.getGuild();
        this.textChannel = textChannel;
        this.member = member;
        settings = RedisData.getSettings(guild);

        Checks.check(Command.CommandPermission.SERVER_OWNER.test(member), "Member must be owner!");
    }

    public void start() {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_start_title"))
                .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "setup_start_summary", SUBMIT_EMOTE))
                .setColor(ColorUtil.WHITE);
        addFooter(builder);
        textChannel.sendMessage(builder.build()).queue(message -> {
            message.addReaction(SUBMIT_EMOTE).queue();
            new EventWaiter.Builder(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == member.getUser().getIdLong() && e.getMessageIdLong() == message.getIdLong(),
                    (e, ew) -> {
                        if (e.getReactionEmote().getName().equals(SUBMIT_EMOTE)) {
                            ew.close();
                            message.delete().queue(m -> step1());
                        }
                    })
                    .timeout(30, TimeUnit.SECONDS)
                    .timeoutAction(() -> message.delete().queue())
                    .build();
        });
    }

    private void save() {
        RedisData.setSettings(guild, settings);
    }

    private void addFooter(EmbedBuilder builder) {
        builder.setFooter(LanguageUtil.getArguedString(guild, Bundle.CAPTION, "waiting_for_response_of",
                member.getEffectiveName()), member.getUser().getAvatarUrl());
    }

    /* STEP 1: Language */

    private String getLanguageLine(Language language) {
        return String.format("%s %s", language.getEmote(), language.getName());
    }

    private String getLanguageList() {
        StringBuilder builder = new StringBuilder();
        for (Language language : Language.values()) {
            if (builder.length() > 1) builder.append("\n");
            builder.append(getLanguageLine(language));
        }
        return builder.toString();
    }

    private void step1() {
        List<String> emotes = Arrays.stream(Language.values()).map(Language::getEmote).collect(Collectors.toList());
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_step_1_title"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "setup_step_1_summary"))
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "languages_list"), getLanguageList(), false)
                .setColor(ColorUtil.WHITE);
        addFooter(embed);

        textChannel.sendMessage(embed.build()).queue(message -> new SelectionListener(message, member.getUser(), -1, emotes, r -> {
            Language language = Language.fromEmote(r.get(0));
            settings.locale = language.getCode();
            message.delete().queue(s -> {
                save();
                step2();
            });
        }, false));
    }

    /* STEP 2: Permissions check */

    private byte test(Permission permission) {
        if (guild.getSelfMember().hasPermission(permission)) {
            return 2;
        } else {
            if (NECESSARY_PERMISSION.contains(permission)) return 0;
            return 1;
        }
    }

    private boolean test(Set<Permission> permissions) {
        for (Permission permission : permissions) if (test(permission) < 1) return false;
        return true;
    }

    private String getDot(byte state) {
        switch (state) {
            case 2:
                return EmoteUtil.GREEN_DOT;
            case 1:
                return EmoteUtil.ORANGE_DOT;
            default:
                return EmoteUtil.RED_DOT;
        }
    }

    private String getMark(boolean state) {
        return state ? EmoteUtil.YES : EmoteUtil.NO;
    }

    private String getPermissionLine(Permission permission) {
        return String.format("%s %s", getDot(test(permission)), LanguageUtil.getString(guild, Bundle.PERMISSION, permission.toString().toLowerCase()));
    }

    private String getPermissionList(Set<Permission> permissions) {
        StringBuilder builder = new StringBuilder();
        for (Permission permission : permissions) {
            if (builder.length() > 1) builder.append("\n");
            builder.append(getPermissionLine(permission));
        }
        return builder.toString();
    }

    private MessageEmbed.Field getPermissionField(Set<Permission> permissions, String title) {
        return new MessageEmbed.Field(
                String.format("%s %s", getMark(test(permissions)), title), getPermissionList(permissions), true
        );
    }

    private void sendPermissionMessage(Consumer<Message> result) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_step_2_title"))
                .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "setup_step_2_summary",
                        EmoteUtil.GREEN_DOT, EmoteUtil.ORANGE_DOT, EmoteUtil.RED_DOT, EmbedBuilder.ZERO_WIDTH_SPACE))
                .addField(getPermissionField(MODERATION_PERMISSION, LanguageUtil.getString(guild, Bundle.CATEGORY, "moderation")))
                .addField(getPermissionField(UTIL_PERMISSION, LanguageUtil.getString(guild, Bundle.CATEGORY, "util")))
                .addField(getPermissionField(MUSIC_PERMISSION, LanguageUtil.getString(guild, Bundle.CATEGORY, "music")))
                .addField(getPermissionField(MISCELLANEOUS_PERMISSION, LanguageUtil.getString(guild, Bundle.CATEGORY, "miscellaneous")))
                .addField(getPermissionField(FUN_PERMISSION, LanguageUtil.getString(guild, Bundle.CATEGORY, "fun")))
                .setColor(test(NECESSARY_PERMISSION) ? ColorUtil.WHITE : ColorUtil.RED);
        addFooter(embed);

        if (permissionMessage > 0) {
            textChannel.getMessageById(permissionMessage).queue(message -> {
                message.editMessage(embed.build()).queue();
            }, throwable -> textChannel.sendMessage(embed.build()).queue(message -> {
                permissionMessage = message.getIdLong();
                result.accept(message);
            }));
        } else {
            textChannel.sendMessage(embed.build()).queue(message -> {
                permissionMessage = message.getIdLong();
                result.accept(message);
            });
        }
    }

    private void sendPermissionMessage() {
        sendPermissionMessage(null);
    }

    private void addReactions(Message message, boolean now) {
        if (test(NECESSARY_PERMISSION)) {
            message.addReaction(SUBMIT_EMOTE).queue();
        } else {
            TaskScheduler.scheduleDelayed(() -> message.addReaction(REFRESH_EMOTE).queue(), now ? 0 : 10 * 1000L);
        }
    }

    private void step2() {
        sendPermissionMessage(message -> {
            addReactions(message, true);
            new EventWaiter.Builder(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == member.getUser().getIdLong() && e.getMessageIdLong() == message.getIdLong(),
                    (e, ew) -> {
                        switch (e.getReactionEmote().getName()) {
                            case REFRESH_EMOTE:
                                try {
                                    message.clearReactions().queue(r -> addReactions(message, false));
                                } catch (InsufficientPermissionException ex) {
                                    e.getReaction().removeReaction().queue(r -> addReactions(message, false));
                                }
                                sendPermissionMessage();
                                break;
                            case SUBMIT_EMOTE:
                                ew.close();
                                message.delete().queue(s -> {
                                    save();
                                    step3();
                                });
                                break;
                            default:
                        }
                    })
                    .autoClose(false)
                    .build();
        });
    }

    /* STEP 3: Protections */

    private void step3() {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_step_3_title"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "setup_step_3_summary"))
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "protections_list"),
                        LanguageUtil.getString(guild, Bundle.STRINGS, "protections_list"), true)
                .setColor(ColorUtil.WHITE);
        addFooter(embed);
        textChannel.sendMessage(embed.build()).queue(message -> new SelectionListener(message, member.getUser(),
                -1, SelectionListener.get(6), r -> {
            for (String s : r) {
                switch (s.charAt(0) - '\u0030') {
                    case 1:
                        settings.detectingFlood = true;
                        break;
                    case 2:
                        settings.detectingCapsSpam = true;
                        break;
                    case 3:
                        settings.detectingEmojisSpam = true;
                        break;
                    case 4:
                        settings.detectingReactionsSpam = true;
                        break;
                    case 5:
                        settings.detectingSwearing = true;
                        break;
                    case 6:
                        settings.detectingAdvertising = true;
                        break;
                    default:
                }
            }
            save();
            end();
        }, true));
    }

    private void end() {
        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_finish"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "setup_finish",
                                CommandMap.getPrefix(guild)))
                        .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "one_more_thing"),
                                LanguageUtil.getString(guild, Bundle.STRINGS, "setup_finish_more"), false)
                        .setColor(ColorUtil.GREEN)
                        .build()
        ).queue();
    }

}
