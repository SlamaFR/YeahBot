package fr.slama.yeahbot.redis.buckets;

import com.google.gson.Gson;
import fr.slama.yeahbot.blub.Mute;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 18/11/2018.
 */
public class Mutes {

    private Map<Long, Mute> mutesMap = new HashMap<>();

    public Mutes() {
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public Map<Long, Mute> getMutesMap() {
        return mutesMap;
    }

}
