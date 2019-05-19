package fr.slama.yeahbot.redis.buckets;

import com.google.gson.Gson;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.music.PlayerSequence;
import fr.slama.yeahbot.settings.AvailableVariables;
import fr.slama.yeahbot.settings.IgnoreSetting;
import fr.slama.yeahbot.settings.LongType;

import java.util.ArrayList;
import java.util.List;

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
    public int maxSearchResults = 5;
    public boolean multipleResultsSearch = true;

    public boolean detectingFlood = true;
    public boolean detectingCapsSpam = true;
    public boolean detectingEmojisSpam = true;
    public boolean detectingReactionsSpam = true;
    public boolean detectingSwearing = true;
    public boolean detectingAdvertising = true;

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

    @AvailableVariables(variables = {AvailableVariables.Variables.USER})
    public String advertisingWarningSentence = "";
    @AvailableVariables(variables = {AvailableVariables.Variables.USER})
    public String capsSpamWarningSentence = "";
    @AvailableVariables(variables = {AvailableVariables.Variables.USER})
    public String emojisSpamWarningSentence = "";
    @AvailableVariables(variables = {AvailableVariables.Variables.USER})
    public String floodWarningSentence = "";
    @AvailableVariables(variables = {AvailableVariables.Variables.USER})
    public String swearingWarningSentence = "";
    @AvailableVariables(variables = {AvailableVariables.Variables.USER})
    public String reactionsSpamWarningSentence = "";

    public boolean sayWelcome = false;
    public boolean sayGoodbye = false;

    @LongType(type = LongType.Type.CHANNEL)
    public long joinLeaveChannel = 0L;
    @LongType(type = LongType.Type.CHANNEL)
    public long updateChannel = 0L;

    @AvailableVariables(variables = {AvailableVariables.Variables.USER,
            AvailableVariables.Variables.GUILD,
            AvailableVariables.Variables.COUNT})
    public String welcomeMessage = "";
    @AvailableVariables(variables = {AvailableVariables.Variables.USER,
            AvailableVariables.Variables.GUILD,
            AvailableVariables.Variables.COUNT})
    public String goodbyeMessage = "";

    public Settings() {
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
