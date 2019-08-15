package fr.slama.yeahbot.blub;

import fr.slama.yeahbot.YeahBot;

/**
 * Created on 2019-07-17.
 */
public class Mute {

    private long authorId;
    private long targetId;
    private long timeout;
    private String reason;

    public Mute() {
    }

    public Mute(long authorId, long timeout, String reason) {
        this.targetId = YeahBot.getInstance().getSnowflake().nextId();
        this.authorId = authorId;
        this.timeout = timeout;
        this.reason = reason;
    }

    public long getAuthorId() {
        return authorId;
    }

    public long getTargetId() {
        return targetId;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getReason() {
        return reason;
    }
}
