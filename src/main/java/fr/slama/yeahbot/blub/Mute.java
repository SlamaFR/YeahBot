package fr.slama.yeahbot.blub;

import fr.slama.yeahbot.YeahBot;

/**
 * Created on 2019-07-17.
 */
public class Mute {

    private long id;
    private long timeout;

    public Mute() {
    }

    public Mute(long timeout) {
        this.id = YeahBot.getInstance().getSnowflake().nextId();
        this.timeout = timeout;
    }

    public long getId() {
        return id;
    }

    public long getTimeout() {
        return timeout;
    }
}
