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
import java.util.Objects;

/**
 * Created on 28/09/2018.
 */
public class SwearingListener extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
            file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("bad_words.json")).getFile());
            if (!file.exists()) return;
        } catch (NullPointerException e) {
            logger.warn("(SWR) Dictionary not found!");
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
                int occurence = event.getMessage().getContentRaw().split((String) s, -1).length - 1;
                if (occurence > 0) {
                    System.out.println(s + " - " + occurence);
                    SwearingTask.idSwearingMap.get(event.getGuild().getIdLong()).merge(event.getMember().getUser().getIdLong(), occurence, Integer::sum);
                }
                else continue;
                if (SwearingTask.idSwearingMap.get(event.getGuild().getIdLong()).get(event.getMember().getUser().getIdLong()) >= settings.timeScaleSwearingTrigger) {
                    ReportsManager.reportSwearing(event.getMessage(), event.getChannel());
                    SwearingTask.idSwearingMap.get(event.getGuild().getIdLong()).remove(event.getMember().getUser().getIdLong());
                    return;
                }
            }

            System.out.println(SwearingTask.idSwearingMap.get(event.getGuild().getIdLong()).get(event.getMember().getUser().getIdLong()));

        } catch (IOException e) {
            logger.error("(SWR) Error while fetching data!");
        } catch (JSONException e) {
            logger.error("(SWR) Error while parsing data!");
        }

    }

}
