package fr.slama.yeahbot.music;

/**
 * Created on 13/11/2018.
 */
public enum PlayerSequence {

    NORMAL,
    LOOP,
    QUEUE_LOOP,
    SHUFFLE_QUEUE_LOOP,
    SHUFFLE;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public String toKey() {
        return "music_player_sequence_" + toString();
    }
}
