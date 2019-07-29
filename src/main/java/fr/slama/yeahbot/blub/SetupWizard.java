package fr.slama.yeahbot.blub;

import com.google.common.collect.Sets;
import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandMap;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.listeners.SelectionListener;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.*;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
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
    private final Consumer<? super Object> SUCCESS = s -> {
    };
    private final Consumer<Throwable> FAIL = s -> {
    };

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
    private final EventWaiter eventWaiter;

    private SelectionListener selectionListener;
    private Message lastMessage;
    private long permissionMessage = 0;
    private boolean running = true;

    public SetupWizard(TextChannel textChannel, Member member) {
        this.guild = member.getGuild();
        this.textChannel = textChannel;
        this.member = member;
        this.settings = RedisData.getSettings(guild);
        this.eventWaiter = new EventWaiter.Builder(GuildMessageReceivedEvent.class,
                e -> e.getMember().equals(member) && e.getMessage().getTextChannel().equals(textChannel),
                (e, ew) -> {
                    if (e.getMessage().getContentRaw().equals("cancel")) {
                        ew.close();
                        end(true);
                    }
                })
                .autoClose(false)
                .build();

        Checks.check(Command.CommandPermission.SERVER_OWNER.test(member), "Member must be owner!");
    }

    public void start() {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_start_title"))
                .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "setup_start_summary", SUBMIT_EMOTE))
                .setColor(ColorUtil.WHITE);
        addFooter(builder);
        textChannel.sendMessage(builder.build()).queue(message -> {
            lastMessage = message;
            message.addReaction(SUBMIT_EMOTE).queue(SUCCESS, FAIL);
            new EventWaiter.Builder(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == member.getUser().getIdLong() && e.getMessageIdLong() == message.getIdLong(),
                    (e, ew) -> {
                        if (SUBMIT_EMOTE.equals(e.getReactionEmote().getName())) {
                            ew.close();
                            message.delete().queue(m -> step1(), FAIL);
                        }
                    })
                    .timeout(1, TimeUnit.MINUTES)
                    .timeoutAction(() -> message.delete().queue(SUCCESS, FAIL))
                    .build();
        }, FAIL);
    }

    private void save() {
        RedisData.setSettings(guild, settings);
    }

    private void addFooter(EmbedBuilder builder) {
        builder.setFooter(StringUtil.response(guild).of(member).canCancel(true).build(),
                member.getUser().getAvatarUrl());
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
        if (!running) return;
        List<String> emotes = Arrays.stream(Language.values()).map(Language::getEmote).collect(Collectors.toList());
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_step_1_title"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "setup_step_1_summary"))
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "languages_list"), getLanguageList(), false)
                .setColor(ColorUtil.WHITE);
        addFooter(embed);

        textChannel.sendMessage(embed.build()).queue(message -> {
            lastMessage = message;
            selectionListener = new SelectionListener(message, member.getUser(), -1, emotes, r -> {
                if (!r.isEmpty()) {
                    Language language = Language.fromEmote(r.get(0));
                    settings.locale = language.getCode();
                }
                message.delete().queue(s -> {
                    save();
                    step2();
                }, FAIL);
            }, false);
            new EventWaiter.Builder(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == member.getUser().getIdLong() && e.getMessageIdLong() == message.getIdLong(),
                    (e, ew) -> {
                        if (SUBMIT_EMOTE.equals(e.getReactionEmote().getName())) {
                            ew.close();
                            message.delete().queue(m -> step1(), FAIL);
                        }
                    })
                    .timeout(30, TimeUnit.SECONDS)
                    .timeoutAction(() -> message.delete().queue(SUCCESS, FAIL))
                    .build();
        }, FAIL);
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
                lastMessage = message;
                message.editMessage(embed.build()).queue(SUCCESS, FAIL);
            }, throwable -> textChannel.sendMessage(embed.build()).queue(message -> {
                permissionMessage = message.getIdLong();
                result.accept(message);
            }, FAIL));
        } else {
            textChannel.sendMessage(embed.build()).queue(message -> {
                lastMessage = message;
                permissionMessage = message.getIdLong();
                result.accept(message);
            }, FAIL);
        }
    }

    private void sendPermissionMessage() {
        sendPermissionMessage(null);
    }

    private void addReactions(Message message, boolean now) {
        if (test(NECESSARY_PERMISSION)) {
            message.addReaction(SUBMIT_EMOTE).queue(SUCCESS, FAIL);
        } else {
            TaskScheduler.scheduleDelayed(() -> message.addReaction(REFRESH_EMOTE).queue(SUCCESS, FAIL), now ? 0 : 10 * 1000L);
        }
    }

    private void step2() {
        if (!running) return;
        sendPermissionMessage(message -> {
            lastMessage = message;
            addReactions(message, true);
            new EventWaiter.Builder(MessageReactionAddEvent.class,
                    e -> e.getUser().getIdLong() == member.getUser().getIdLong() && e.getMessageIdLong() == message.getIdLong(),
                    (e, ew) -> {
                        switch (e.getReactionEmote().getName()) {
                            case REFRESH_EMOTE:
                                try {
                                    message.clearReactions().queue(r -> addReactions(message, false), FAIL);
                                } catch (InsufficientPermissionException ex) {
                                    e.getReaction().removeReaction().queue(r -> addReactions(message, false), FAIL);
                                }
                                sendPermissionMessage();
                                break;
                            case SUBMIT_EMOTE:
                                ew.close();
                                message.delete().queue(s -> {
                                    save();
                                    step3();
                                }, FAIL);
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
        if (!running) return;
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_step_3_title"))
                .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "setup_step_3_summary"))
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "protections_list"),
                        LanguageUtil.getString(guild, Bundle.STRINGS, "protections_list"), true)
                .setColor(ColorUtil.WHITE);
        addFooter(embed);
        textChannel.sendMessage(embed.build()).queue(message -> {
            lastMessage = message;
            selectionListener = new SelectionListener(message,
                    member.getUser(), -1, SelectionListener.get(6), r -> {
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
                end(false);
            }, true);
        }, FAIL);
    }

    private void end(boolean forced) {
        if (!running) return;
        running = false;
        YeahBot.getInstance().getSetupManager().deleteWizard(guild);
        if (selectionListener != null) selectionListener.close();
        if (lastMessage != null) lastMessage.delete().queue(SUCCESS, FAIL);
        eventWaiter.close();
        if (forced) {
            textChannel.sendMessage(
                    MessageUtil.getSuccessEmbed(guild, LanguageUtil.getArguedString(guild, Bundle.STRINGS, "setup_ended",
                            CommandMap.getPrefix(guild)))
            ).queue();
        } else {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "setup_finish"))
                            .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "setup_finish",
                                    CommandMap.getPrefix(guild)))
                            .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "one_more_thing"),
                                    LanguageUtil.getString(guild, Bundle.STRINGS, "setup_finish_more"), false)
                            .setColor(ColorUtil.GREEN)
                            .build()
            ).queue(SUCCESS, FAIL);
        }
    }

}
