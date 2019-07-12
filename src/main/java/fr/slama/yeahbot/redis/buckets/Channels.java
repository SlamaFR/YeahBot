package fr.slama.yeahbot.redis.buckets;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 20/11/2018.
 */
public class Channels {

    private List<Long> channels = new ArrayList<>();

    public Channels() {
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public List<Long> getChannels() {
        return channels;
    }
}
