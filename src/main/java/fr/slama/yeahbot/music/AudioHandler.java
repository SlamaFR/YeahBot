package fr.slama.yeahbot.music;

import com.sedmelluq.discord.lavaplayer.format.OpusAudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * Created on 19/09/2018.
 */
public class AudioHandler implements AudioSendHandler {

    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;

    AudioHandler(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    @Override
    public boolean isOpus() {
        return lastFrame != null && lastFrame.getFormat() instanceof OpusAudioDataFormat;
    }

}
