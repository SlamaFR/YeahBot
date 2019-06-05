package fr.slama.yeahbot.music;


import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.Map;

/**
 * Created on 12/11/2018.
 */
public class MusicPlayer {

    private final AudioPlayer audioPlayer;
    private final TrackScheduler trackScheduler;
    private final Guild guild;
    private TextChannel textChannel;

    public MusicPlayer(AudioPlayer audioPlayer, Guild guild, Map<Long, MusicPlayer> players) {
        this.audioPlayer = audioPlayer;
        this.guild = guild;
        trackScheduler = new TrackScheduler(this, players);
        audioPlayer.addListener(trackScheduler);
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public Guild getGuild() {
        return guild;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public void setTextChannel(TextChannel textChannel) {
        this.textChannel = textChannel;
    }

    public TrackScheduler getTrackScheduler() {
        return trackScheduler;
    }

    public AudioHandler getAudioHandler() {
        return new AudioHandler(audioPlayer);
    }

    public synchronized void playTrack(AudioTrack audioTrack, Member member, boolean firstPosition) {
        trackScheduler.addToQueue(new Track(audioTrack, member.getUser().getIdLong()), firstPosition, true);
        member.getGuild().getAudioManager().openAudioConnection(member.getVoiceState().getChannel());
    }

    public synchronized void skipTrack() {
        trackScheduler.skip();
    }

    public synchronized void removeNextTrack() {
        Track track = trackScheduler.getQueue().pollFirst();
        if (trackScheduler.isLoopingQueue())
            trackScheduler.getQueue().offerLast(track);
    }
}
