package fr.slama.yeahbot.redis.buckets;

import com.google.gson.Gson;
import fr.slama.yeahbot.blub.Playlist;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 14/12/2018.
 */
public class Playlists {

    private Map<String, Playlist> playlists = new HashMap<>();

    public Playlists() {
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public Map<String, Playlist> getPlaylists() {
        return playlists;
    }

}
