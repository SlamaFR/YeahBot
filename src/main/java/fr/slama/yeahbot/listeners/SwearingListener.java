package fr.slama.yeahbot.listeners;

import fr.slama.yeahbot.json.JSONReader;
import fr.slama.yeahbot.managers.ReportsManager;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.tasks.SwearingTask;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on 28/09/2018.
 */
public class SwearingListener extends ListenerAdapter {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        super.onGuildMessageReceived(event);

        Settings settings = RedisData.getSettings(event.getGuild());
        if (!settings.detectingSwearing) return;

        if (!SwearingTask.idSwearingMap.containsKey(event.getGuild().getIdLong()))
            SwearingTask.idSwearingMap.put(event.getGuild().getIdLong(), new HashMap<>());

        if (settings.swearingIgnoredChannels.contains(event.getChannel().getIdLong())) return;

        File file;
        try {
            file = new File("./bad_words.json");
            if (!file.exists()) return;
        } catch (NullPointerException e) {
            LOGGER.warn("(SWR) Dictionary not found!");
            return;
        }

        try {

            JSONReader reader = new JSONReader(file);
            JSONObject object = reader.toJSONObject();

            JSONArray array;
            try {
                array = object.getJSONArray(settings.locale);
            } catch (JSONException ignored) {
                return;
            }

            List<Object> badWords = array.toList();

            for (Object s : badWords) {
                Matcher matcher = Pattern
                        .compile("\\b(" + s + "[^ ]*)\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(event.getMessage().getContentRaw());
                if (matcher.find()) {
                    int occurence = 1;
                    while (matcher.find()) occurence++;
                    SwearingTask.idSwearingMap.get(event.getGuild().getIdLong()).merge(event.getMember().getUser().getIdLong(), occurence, Integer::sum);
                } else continue;
                if (SwearingTask.idSwearingMap.get(event.getGuild().getIdLong()).get(event.getMember().getUser().getIdLong()) >= settings.timeScaleSwearingTrigger) {
                    ReportsManager.reportSwearing(event.getMessage(), event.getChannel());
                    SwearingTask.idSwearingMap.get(event.getGuild().getIdLong()).remove(event.getMember().getUser().getIdLong());
                    return;
                }
            }

        } catch (IOException e) {
            LOGGER.error("(SWR) Error while fetching data!");
        } catch (JSONException e) {
            LOGGER.error("(SWR) Error while parsing data!");
        }

    }

}
