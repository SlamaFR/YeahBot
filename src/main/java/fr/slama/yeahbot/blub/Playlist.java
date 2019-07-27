package fr.slama.yeahbot.blub;

/**
 * Created on 24/01/2019.
 */
public class Playlist {

    private String url;
    private String name;
    private long owner;

    public Playlist(String url, String name, long owner) {
        this.url = url;
        this.name = name;
        this.owner = owner;
    }

    public Playlist() {
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public long getOwnerLong() {
        return owner;
    }
}
