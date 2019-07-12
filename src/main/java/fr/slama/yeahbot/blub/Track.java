package fr.slama.yeahbot.blub;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Created on 13/11/2018.
 */
public class Track {

    private final AudioTrack audioTrack;
    private final long requesterId;

    public Track(AudioTrack audioTrack, long requesterId) {
        this.audioTrack = audioTrack;
        this.requesterId = requesterId;
    }

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    public long getRequesterId() {
        return requesterId;
    }

    @Override
    public String toString() {
        return String.format("Track:R/%s:T/%s", requesterId, audioTrack.toString());
    }
}
