package fr.slama.yeahbot.tasks;

import com.google.common.util.concurrent.AtomicLongMap;

import java.util.HashMap;
import java.util.TimerTask;

/**
 * Created on 08/11/2018.
 */
public class SwearingTask extends TimerTask {

    public static HashMap<Long, AtomicLongMap<Long>> idSwearingMap = new HashMap<>();

    @Override
    public void run() {
        idSwearingMap.clear();
    }

}
