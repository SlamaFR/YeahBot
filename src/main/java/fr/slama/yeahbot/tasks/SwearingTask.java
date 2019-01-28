package fr.slama.yeahbot.tasks;

import java.util.HashMap;
import java.util.TimerTask;

/**
 * Created on 08/11/2018.
 */
public class SwearingTask extends TimerTask {

    public static HashMap<Long, HashMap<Long, Integer>> idSwearingMap = new HashMap<>();

    @Override
    public void run() {
        idSwearingMap.clear();
    }

}
