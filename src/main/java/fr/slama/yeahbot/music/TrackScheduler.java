package fr.slama.yeahbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.Track;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.TimeUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created on 12/11/2018.
 */
public class TrackScheduler extends AudioEventAdapter {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final LinkedList<Track> queue = new LinkedList<>();
    private final MusicPlayer musicPlayer;
    private final Map<Long, MusicPlayer> players;
    private final Guild guild;

    private long nowPlayingMessageId = 0L;
    private long currentRequesterId = 0L;
    private boolean userPaused = false;
    private Track currentTrack;
    private List<Long> votingUsers;

    private int playedTrack = 0;

    TrackScheduler(MusicPlayer musicPlayer, Map<Long, MusicPlayer> players) {
        this.musicPlayer = musicPlayer;
        this.players = players;
        this.guild = musicPlayer.getGuild();
    }

    void addToQueue(Track track, boolean firstPosition, boolean canShuffle) {
        if (firstPosition) queue.offerFirst(track);
        else {
            queue.offerLast(track);
            if (canShuffle && RedisData.getSettings(guild).playerSequence.equals(PlayerSequence.SHUFFLE) ||
                    RedisData.getSettings(guild).playerSequence.equals(PlayerSequence.SHUFFLE_QUEUE_LOOP)) {
                Collections.shuffle(queue);
            }
        }
        if (currentTrack == null)
            startNextTrack(true);
    }

    void skip() {
        startNextTrack(false);
    }

    public void togglePause() {
        if (userPaused) resume(true);
        else pause(true);
        userPaused = !userPaused;
    }

    public void stop() {
        musicPlayer.getAudioPlayer().stopTrack();
        musicPlayer.getAudioPlayer().destroy();
        musicPlayer.setTextChannel(null);
        players.remove(guild.getIdLong());
        guild.getAudioManager().closeAudioConnection();
        currentTrack = null;
        currentRequesterId = 0L;
        nowPlayingMessageId = 0L;
        playedTrack = 0;
        userPaused = false;
    }

    private void startNextTrack(boolean noInterrupt) {

        Track exTrack = currentTrack;

        if (isLoopingQueue() && !noInterrupt) {
            addToQueue(new Track(exTrack.getAudioTrack().makeClone(), currentRequesterId), false, false);
        }

        Track track = queue.pollFirst();

        if (track != null) {
            long exRequesterId = currentRequesterId;
            currentRequesterId = track.getRequesterId();
            currentTrack = track;
            if (!musicPlayer.getAudioPlayer().startTrack(track.getAudioTrack(), noInterrupt)) {
                queue.offerFirst(track);
                currentRequesterId = exRequesterId;
                currentTrack = exTrack;
            }
            return;
        }

        musicPlayer.getTextChannel().sendMessage(
                new EmbedBuilder()
                        .setColor(ColorUtil.ORANGE)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "audio_player"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.STRINGS, "playlist_over"))
                        .build()
        ).queue();
        stop();
    }

    public void pause(boolean tell) {
        musicPlayer.getAudioPlayer().setPaused(true);
        assert musicPlayer.getTextChannel() != null;
        if (tell) musicPlayer.getTextChannel().sendMessage(
                LanguageUtil.getString(musicPlayer.getGuild(), Bundle.STRINGS, "music_paused")
        ).queue();
    }

    public void resume(boolean tell) {
        musicPlayer.getAudioPlayer().setPaused(false);
        assert musicPlayer.getTextChannel() != null;
        if (tell) musicPlayer.getTextChannel().sendMessage(
                LanguageUtil.getString(musicPlayer.getGuild(), Bundle.STRINGS, "music_resumed")
        ).queue();
    }

    boolean isLoopingQueue() {
        return RedisData.getSettings(guild).playerSequence.equals(PlayerSequence.QUEUE_LOOP) ||
                RedisData.getSettings(guild).playerSequence.equals(PlayerSequence.SHUFFLE_QUEUE_LOOP);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {

            if (playedTrack >= YeahBot.getConfig().maxTracks) {
                musicPlayer.getTextChannel().sendMessage(
                        LanguageUtil.getArguedString(guild, Bundle.STRINGS, "audio_player_limit_reached", YeahBot.getConfig().maxTracks)
                ).queue();
                stop();
                return;
            }

            if (RedisData.getSettings(guild).playerSequence.equals(PlayerSequence.LOOP)) {
                addToQueue(new Track(track.makeClone(), currentRequesterId), true, false);
            } else if (isLoopingQueue()) {
                addToQueue(new Track(track.makeClone(), currentRequesterId), false, false);
            }
            startNextTrack(true);
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {

        if (playedTrack >= YeahBot.getConfig().maxTracks) {
            musicPlayer.getTextChannel().sendMessage(
                    LanguageUtil.getArguedString(guild, Bundle.STRINGS, "audio_player_limit_reached", YeahBot.getConfig().maxTracks)
            ).queue();
            stop();
            return;
        }

        playedTrack++;
        LOGGER.info("{} Now playing {}", guild, track.getInfo().title);

        votingUsers = new ArrayList<>();

        if (RedisData.getSettings(guild).playerSequence.equals(PlayerSequence.LOOP) && nowPlayingMessageId > 0L)
            return;

        String requesterName = musicPlayer.getGuild().getMemberById(currentRequesterId).getEffectiveName();
        String requesterAvatarUrl = musicPlayer.getGuild().getMemberById(currentRequesterId).getUser().getAvatarUrl();

        MessageEmbed embed = new EmbedBuilder()
                .setColor(ColorUtil.BLUE)
                .addField(
                        LanguageUtil.getString(guild, Bundle.CAPTION, "now_playing"),
                        LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_track", track.getInfo().title, track.getInfo().uri),
                        false)
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "music_player_volume"),
                        String.format("%s %d%%",
                                EmoteUtil.getVolumeEmote(musicPlayer.getAudioPlayer().getVolume()),
                                musicPlayer.getAudioPlayer().getVolume()
                        ), true)
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "music_player_sequence"),
                        String.format("%s %s",
                                EmoteUtil.getSequenceEmote(RedisData.getSettings(guild).playerSequence),
                                LanguageUtil.getString(guild, Bundle.CAPTION, RedisData.getSettings(guild).playerSequence.toKey())
                        ), true)
                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "music_track_duration"),
                        "⏱ " + TimeUtil.toTime(track.getDuration()), true)
                .setFooter(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_submitted_by", requesterName) + (!queue.isEmpty() && !isLoopingQueue() ? " • " + LanguageUtil.getArguedString(guild, Bundle.CAPTION, "remaining_tracks", queue.size()) : ""), requesterAvatarUrl)
                .build();

        if (nowPlayingMessageId > 0 && musicPlayer.getTextChannel().getLatestMessageIdLong() == nowPlayingMessageId) {
            musicPlayer.getTextChannel().getMessageById(nowPlayingMessageId).queue(
                    msg -> msg.editMessage(embed).queue(),
                    t -> musicPlayer.getTextChannel().sendMessage(embed).queue(message -> nowPlayingMessageId = message.getIdLong())
            );
        } else {
            if (guild.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE))
                musicPlayer.getTextChannel().getMessageById(nowPlayingMessageId).queue(message -> message.delete().queue(), t -> {
                });
            musicPlayer.getTextChannel().sendMessage(embed).queue(message -> nowPlayingMessageId = message.getIdLong());
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        musicPlayer.getTextChannel().sendMessage(
                LanguageUtil.getArguedString(musicPlayer.getGuild(), Bundle.STRINGS, "music_got_stuck", track.getInfo().title)
        ).queue();
        startNextTrack(false);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        musicPlayer.getTextChannel().sendMessage(
                new EmbedBuilder()
                        .setColor(ColorUtil.RED)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                        .setDescription(String.format("%s\n```\n%s\n```", LanguageUtil.getString(guild, Bundle.ERROR, "something_went_wrong"), exception.getMessage()))
                        .build()
        ).queue();
        super.onTrackException(player, track, exception);
    }

    public boolean isUserPaused() {
        return userPaused;
    }

    public LinkedList<Track> getQueue() {
        return queue;
    }

    public List<Long> getVotingUsers() {
        return votingUsers;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public long getCurrentRequesterId() {
        return currentRequesterId;
    }
}
