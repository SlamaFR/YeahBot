package fr.slama.yeahbot.tasks;

import java.util.HashMap;
import java.util.TimerTask;

/**
 * Created on 27/09/2018.
 */
public class SpamTask extends TimerTask {

    public static HashMap<Long, HashMap<Long, Integer>> idSpamMap = new HashMap<>();
    public static HashMap<Long, HashMap<Long, Integer>> idSpamCapsMap = new HashMap<>();
    public static HashMap<Long, HashMap<Long, Integer>> idSpamEmotesMap = new HashMap<>();
    public static HashMap<Long, HashMap<Long, Integer>> idSpamReactionMap = new HashMap<>();

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
