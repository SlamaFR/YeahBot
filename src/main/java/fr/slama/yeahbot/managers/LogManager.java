package fr.slama.yeahbot.managers;

import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.GuildUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;

/**
 * Created on 2019-08-15.
 */
public class LogManager {

    public static void prune(Member member, TextChannel textChannel, int amount) {
        Guild guild = member.getGuild();
        TextChannel logChannel = GuildUtil.getLogChannel(guild, true);
        if (logChannel == null) return;

        EmbedBuilder builder = getEmbed(member, textChannel, ColorUtil.ORANGE);
        builder.setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "prune"));
        builder.addField(LanguageUtil.getString(guild, Bundle.CAPTION, "amount"),
                String.format("`%s`", amount), true);

        logChannel.sendMessage(builder.build()).queue();
    }

    public static void sinsCleared(Member member, Member target) {
        Guild guild = member.getGuild();
        TextChannel logChannel = GuildUtil.getLogChannel(guild, true);
        if (logChannel == null) return;

        EmbedBuilder builder = getEmbed(member, null, ColorUtil.ORANGE);
        builder.setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "cleared_sins"));
        builder.addField(LanguageUtil.getString(guild, Bundle.CAPTION, target.getUser().isBot() ? "bot" : "author"),
                String.format("%s (%s#%s)", member.getAsMention(), target.getUser().getName(), target.getUser().getDiscriminator()),
                true);

        logChannel.sendMessage(builder.build()).queue();
    }

    private static EmbedBuilder getEmbed(Member member, @Nullable TextChannel textChannel, Color color) {
        Guild guild = member.getGuild();
        User user = member.getUser();
        EmbedBuilder builder = new EmbedBuilder()
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, user.isBot() ? "bot" : "author"),
                        String.format("%s (%s#%s)", member.getAsMention(), user.getName(), user.getDiscriminator()),
                        true)
                .setTimestamp(Instant.now())
                .setColor(color);
        if (textChannel != null)
            builder.addField(LanguageUtil.getString(guild, Bundle.CAPTION, "text_channel"), textChannel.getAsMention(), true);
        return builder;
    }

}
