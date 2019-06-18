package fr.slama.yeahbot.redis.buckets;

import com.google.gson.Gson;
import fr.slama.yeahbot.blub.Sanction;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.music.PlayerSequence;
import fr.slama.yeahbot.settings.AvailableVariables;
import fr.slama.yeahbot.settings.IgnoreSetting;
import fr.slama.yeahbot.settings.LongType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static fr.slama.yeahbot.settings.AvailableVariables.Variables.*;

/**
 * Created on 16/11/2018.
 */
public class Settings {

    @IgnoreSetting
    public String prefix = "!";
    @IgnoreSetting
    public String locale = Language.ENGLISH;
    @LongType(type = LongType.Type.ROLE)
    public long muteRole = 0L;

    @IgnoreSetting
    public int playerVolume = 50;
    @IgnoreSetting
    public PlayerSequence playerSequence = PlayerSequence.NORMAL;
    @IgnoreSetting
    public boolean shuffle = false;
    @IgnoreSetting
    public byte loop = 0;
    @IgnoreSetting
    public int maxSearchResults = 5;
    public boolean multipleResultsSearch = true;

    public boolean detectingFlood = true;
    public boolean detectingCapsSpam = true;
    public boolean detectingEmojisSpam = true;
    public boolean detectingReactionsSpam = true;
    public boolean detectingSwearing = true;
    public boolean detectingAdvertising = true;

    public Map<Integer, Sanction> spamPolicy = new HashMap<>();
    public Map<Integer, Sanction> swearingPolicy = new HashMap<>();
    public Map<Integer, Sanction> advertisingPolicy = new HashMap<>();

    @IgnoreSetting
    public int timeScaleSpamTrigger = 5;
    @IgnoreSetting
    public int timeScaleSwearingTrigger = 5;

    @IgnoreSetting
    public List<Long> spamIgnoredChannels = new ArrayList<>();
    @IgnoreSetting
    public List<Long> swearingIgnoredChannels = new ArrayList<>();
    @IgnoreSetting
    public List<Long> advertisingIgnoredChannels = new ArrayList<>();

    @AvailableVariables(variables = {USER})
    public String advertisingWarningSentence = "";
    @AvailableVariables(variables = {USER})
    public String capsSpamWarningSentence = "";
    @AvailableVariables(variables = {USER})
    public String emojisSpamWarningSentence = "";
    @AvailableVariables(variables = {USER})
    public String floodWarningSentence = "";
    @AvailableVariables(variables = {USER})
    public String swearingWarningSentence = "";
    @AvailableVariables(variables = {USER})
    public String reactionsSpamWarningSentence = "";

    public boolean sayWelcome = false;
    public boolean sayGoodbye = false;

    @LongType(type = LongType.Type.CHANNEL)
    public long joinLeaveChannel = 0L;
    @LongType(type = LongType.Type.CHANNEL)
    public long updateChannel = 0L;

    @AvailableVariables(variables = {USER, GUILD, COUNT})
    public String welcomeMessage = "";
    @AvailableVariables(variables = {USER, GUILD, COUNT})
    public String goodbyeMessage = "";

    @Override
    public String toString() {

        if (spamPolicy.isEmpty()) {
            spamPolicy.put(3, new Sanction(Sanction.Type.MUTE, 5, TimeUnit.MINUTES));
            spamPolicy.put(5, new Sanction(Sanction.Type.MUTE, 30, TimeUnit.MINUTES));
            spamPolicy.put(7, new Sanction(Sanction.Type.MUTE, 2, TimeUnit.HOURS));
            spamPolicy.put(10, new Sanction(Sanction.Type.MUTE, 24, TimeUnit.HOURS));
            spamPolicy.put(13, new Sanction(Sanction.Type.MUTE, 7, TimeUnit.DAYS));
            spamPolicy.put(15, new Sanction(Sanction.Type.KICK));
            spamPolicy.put(17, new Sanction(Sanction.Type.BAN));
        }

        if (swearingPolicy.isEmpty()) {
            swearingPolicy.put(3, new Sanction(Sanction.Type.MUTE, 5, TimeUnit.MINUTES));
            swearingPolicy.put(5, new Sanction(Sanction.Type.MUTE, 2, TimeUnit.HOURS));
            swearingPolicy.put(7, new Sanction(Sanction.Type.MUTE, 24, TimeUnit.HOURS));
            swearingPolicy.put(8, new Sanction(Sanction.Type.MUTE, 7, TimeUnit.DAYS));
            swearingPolicy.put(9, new Sanction(Sanction.Type.KICK));
            swearingPolicy.put(10, new Sanction(Sanction.Type.BAN));
        }

        if (advertisingPolicy.isEmpty()) {
            advertisingPolicy.put(2, new Sanction(Sanction.Type.MUTE, 2, TimeUnit.HOURS));
            advertisingPolicy.put(4, new Sanction(Sanction.Type.KICK));
            advertisingPolicy.put(5, new Sanction(Sanction.Type.BAN));
        }

        return new Gson().toJson(this);
    }

}
