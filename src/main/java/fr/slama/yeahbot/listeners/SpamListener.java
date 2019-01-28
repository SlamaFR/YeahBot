package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.managers.ReportsManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.tasks.SpamTask;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.SpamType;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 27/09/2018.
 */
public class SpamListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        super.onGuildMessageReceived(event);
        if (event.getAuthor().isBot()) return;

        if (!SpamTask.idSpamMap.containsKey(event.getGuild().getIdLong()))
            SpamTask.idSpamMap.put(event.getGuild().getIdLong(), new HashMap<>());
        if (!SpamTask.idSpamCapsMap.containsKey(event.getGuild().getIdLong()))
            SpamTask.idSpamCapsMap.put(event.getGuild().getIdLong(), new HashMap<>());
        if (!SpamTask.idSpamEmotesMap.containsKey(event.getGuild().getIdLong()))
            SpamTask.idSpamEmotesMap.put(event.getGuild().getIdLong(), new HashMap<>());

        Settings settings = RedisData.getSettings(event.getGuild());
        if (settings.spamIgnoredChannels.contains(event.getChannel().getIdLong())) return;

        if (settings.detectingCapsSpam || settings.detectingEmojisSpam) {

            char[] chars = event.getMessage().getContentRaw().toCharArray();
            int upperChar = 0;
            int emojis = 0;

            for (char character : chars) {
                if (Character.isUpperCase(character)) upperChar++;
            }

            final Pattern pattern = Pattern.compile(EmoteUtil.EMOJI_REGEX);
            final Matcher matcher = pattern.matcher(event.getMessage().getContentRaw());
            while (matcher.find()) emojis++;

            if (settings.detectingCapsSpam) {
                double capsPercentage = upperChar * 100D / chars.length;
                if (capsPercentage >= 50D && chars.length > 15) {
                    SpamTask.idSpamCapsMap.get(event.getGuild().getIdLong()).merge(event.getAuthor().getIdLong(), 1, Integer::sum);
                }
                if (SpamTask.idSpamCapsMap.get(event.getGuild().getIdLong()).containsKey(event.getAuthor().getIdLong()) &&
                        SpamTask.idSpamCapsMap
                                .get(event.getGuild().getIdLong())
                                .get(event.getAuthor().getIdLong()) >= settings.timeScaleSpamTrigger) {
                    SpamTask.idSpamCapsMap.get(event.getGuild().getIdLong()).remove(event.getAuthor().getIdLong());
                    ReportsManager.reportSpam(event.getMember(), event.getMessage(), event.getChannel(), SpamType.CAPS);
                    return;
                }
            }

            if (settings.detectingEmojisSpam) {
                double emojisPercentage = emojis * 100D / chars.length;

                if (emojis > 5 && emojisPercentage >= 25D) {
                    SpamTask.idSpamEmotesMap.get(event.getGuild().getIdLong()).merge(event.getAuthor().getIdLong(), 1, Integer::sum);
                }
                if (SpamTask.idSpamEmotesMap.get(event.getGuild().getIdLong()).containsKey(event.getAuthor().getIdLong()) &&
                        SpamTask.idSpamEmotesMap
                                .get(event.getGuild().getIdLong())
                                .get(event.getAuthor().getIdLong()) >= settings.timeScaleSpamTrigger / 2) {
                    SpamTask.idSpamEmotesMap.get(event.getGuild().getIdLong()).remove(event.getAuthor().getIdLong());
                    ReportsManager.reportSpam(event.getMember(), event.getMessage(), event.getChannel(), SpamType.EMOJIS);
                    return;
                }
            }

        }

        if (settings.detectingFlood) {
            SpamTask.idSpamMap.get(event.getGuild().getIdLong()).merge(event.getAuthor().getIdLong(), 1, Integer::sum);

            if (SpamTask.idSpamMap.get(event.getGuild().getIdLong()).get(event.getAuthor().getIdLong()) >= settings.timeScaleSpamTrigger) {
                SpamTask.idSpamMap.get(event.getGuild().getIdLong()).remove(event.getAuthor().getIdLong());
                ReportsManager.reportSpam(event.getMember(), event.getMessage(), event.getChannel(), SpamType.FLOOD);
            }
        }


    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        super.onGuildMessageReactionAdd(event);
        if (event.getUser().isBot()) return;
        Settings settings = RedisData.getSettings(event.getGuild());
        if (!settings.detectingReactionsSpam) return;

        if (!SpamTask.idSpamReactionMap.containsKey(event.getGuild().getIdLong()))
            SpamTask.idSpamReactionMap.put(event.getGuild().getIdLong(), new HashMap<>());

        event.getReaction().getUsers().queue(users -> {
            if (users.size() < 2) {
                SpamTask.idSpamReactionMap.get(event.getGuild().getIdLong()).merge(event.getUser().getIdLong(), 1, Integer::sum);
            }
        });

        if (SpamTask.idSpamReactionMap.get(event.getGuild().getIdLong()).containsKey(event.getUser().getIdLong()) &&
                SpamTask.idSpamReactionMap.get(event.getGuild().getIdLong()).get(event.getUser().getIdLong()) >= 7) {
            SpamTask.idSpamReactionMap.get(event.getGuild().getIdLong()).remove(event.getUser().getIdLong());
            ReportsManager.reportSpam(event.getMember(), event.getChannel().getMessageById(event.getMessageId()).complete(), event.getChannel(), SpamType.REACTIONS);
        }

    }

}
