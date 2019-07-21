package fr.slama.yeahbot.tasks;

import com.google.common.util.concurrent.AtomicLongMap;

import java.util.HashMap;
import java.util.TimerTask;

/**
 * Created on 27/09/2018.
 */
public class SpamTask extends TimerTask {

    public static HashMap<Long, AtomicLongMap<Long>> idSpamMap = new HashMap<>();
    public static HashMap<Long, AtomicLongMap<Long>> idSpamCapsMap = new HashMap<>();
    public static HashMap<Long, AtomicLongMap<Long>> idSpamEmotesMap = new HashMap<>();
    public static HashMap<Long, AtomicLongMap<Long>> idSpamReactionMap = new HashMap<>();

    private int timer = 0;

    @Override
    public void run() {
        timer++;
        idSpamMap.clear();
        if (timer % 3 == 0) idSpamReactionMap.clear();
        if (timer == 12) {
            timer = 0;
            idSpamCapsMap.clear();
            idSpamEmotesMap.clear();
        }
    }

}
