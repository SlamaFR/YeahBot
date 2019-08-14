package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.json.JSONReader;
import fr.slama.yeahbot.managers.ReportsManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 08/11/2018.
 */
public class AdvertisingListener extends ListenerAdapter {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        super.onGuildMessageReceived(event);

        Settings settings = RedisData.getSettings(event.getGuild());
        if (!settings.detectingAdvertising) return;

        if (settings.advertisingIgnoredChannels.contains(event.getChannel().getIdLong())) return;

        int rate = 0;
        int keywords_amount = 0;

        boolean hasInvite = false;
        boolean hasKeyword = false;

        boolean containsMarkdown = !event.getMessage().getContentDisplay().equals(event.getMessage().getContentStripped());
        if (containsMarkdown) rate += 5;

        File file;
        try {
            file = new File("./ad_keywords.json");
            if (!file.exists()) return;
        } catch (NullPointerException e) {
            LOGGER.warn("(AD) Dictionary not found!");
            return;
        }

        try {

            JSONReader reader = new JSONReader(file);
            JSONObject object = reader.toJSONObject();

            JSONArray array;
            try {
                array = object.getJSONArray(settings.locale);
            } catch (JSONException e) {
                return;
            }

            List<Object> keywords = array.toList();

            for (Object s : keywords) {
                Matcher matcher = Pattern.compile(Pattern.quote(String.valueOf(s)), Pattern.CASE_INSENSITIVE).matcher(event.getMessage().getContentRaw());
                while (matcher.find()) {
                    rate += 10;
                    keywords_amount++;
                    hasKeyword = true;
                }
            }

        } catch (IOException e) {
            LOGGER.error("(AD) Error while fetching data!");
        } catch (JSONException e) {
            LOGGER.error("(AD) Error while parsing data!");
        }

        for (String s : event.getMessage().getContentRaw().split("\\W")) {
            try {
                URL url = new URL(s);
                if ("discord.gg".equals(url.getHost())) {
                    rate += 50;
                    hasInvite = true;
                } else rate += 10;
            } catch (MalformedURLException e) {
                continue;
            }
        }

        Matcher matcher = Pattern.compile("([\\u20a0-\\u32ff\\ud83c\\udc00-\\ud83d\\udeff\\udbb9\\udce5-\\udbb9\\udcee])", Pattern.CASE_INSENSITIVE).matcher(event.getMessage().getContentRaw());
        while (matcher.find()) {
            rate += 2;
        }

        if ((hasInvite || hasKeyword) && rate >= 100 && keywords_amount >= 3) {
            ReportsManager.reportAdvertising(event.getMessage());
        }

    }

}
