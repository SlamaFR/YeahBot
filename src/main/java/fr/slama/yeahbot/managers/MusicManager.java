package fr.slama.yeahbot.managers;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.listeners.SelectionListener;
import fr.slama.yeahbot.music.MusicPlayer;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.TimeUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created on 12/11/2018.
 */
public class MusicManager {

    private final AudioPlayerManager manager = new DefaultAudioPlayerManager();
    private final Map<Long, MusicPlayer> players = new HashMap<>();

    public MusicManager() {
        AudioSourceManagers.registerLocalSource(manager);
        AudioSourceManagers.registerRemoteSources(manager);
    }

    public synchronized MusicPlayer getPlayer(Guild guild) {
        if (!players.containsKey(guild.getIdLong())) {
            AudioPlayer audioPlayer = manager.createPlayer();
            audioPlayer.setVolume(RedisData.getSettings(guild).playerVolume);
            players.put(guild.getIdLong(), new MusicPlayer(audioPlayer, guild, players));
        }
        return players.get(guild.getIdLong());
    }

    public void loadTrack(final TextChannel textChannel, final String source, Member member, final boolean firstPosition) {
        this.loadTrack(textChannel, source, member, firstPosition, true);
    }

    public void loadTrack(final TextChannel textChannel, final String source, Member member, final boolean firstPosition, final boolean useIndex) {
        MusicPlayer player = getPlayer(textChannel.getGuild());
        textChannel.getGuild().getAudioManager().setSendingHandler(player.getAudioHandler());

        final Settings settings = RedisData.getSettings(textChannel.getGuild());

        manager.loadItemOrdered(player, source, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                textChannel.sendMessage(
                        new EmbedBuilder()
                                .setColor(ColorUtil.GREEN)
                                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "track_added"))
                                .setDescription(track.getInfo().title)
                                .build()
                ).queue();
                if (player.getTextChannel() == null) player.setTextChannel(textChannel);
                player.playTrack(track, member, firstPosition);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

                Guild guild = textChannel.getGuild();

                if (!playlist.isSearchResult()) {

                    int from = 0;
                    long totalDuration = 0L;
                    StringBuilder trackList = new StringBuilder();

                    for (String s : source.split("&")) {
                        if (s.startsWith("index=")) try {
                            from = Integer.parseInt(s.replace("index=", ""));
                        } catch (NumberFormatException e) {
                            break;
                        }
                    }

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    if (from > 0) {
                        playlist.getTracks().subList(0, from - 1).clear();
                        embedBuilder.setTitle(LanguageUtil.getArguedString(guild, Bundle.CAPTION, "playlist_added_from", from));
                    } else embedBuilder.setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "playlist_added"));

                    for (AudioTrack track : playlist.getTracks()) {
                        totalDuration += track.getDuration();
                        if (playlist.getTracks().indexOf(track) < 10) {
                            if (trackList.length() > 1) trackList.append("\n");
                            trackList.append(String.format("● `%s`", track.getInfo().title));
                        }
                    }

                    embedBuilder.setColor(ColorUtil.GREEN);
                    embedBuilder.setDescription(playlist.getName());
                    embedBuilder.addField(LanguageUtil.getString(guild, Bundle.CAPTION, "music_track_duration"), "⏱ " + TimeUtil.toTime(totalDuration), false);
                    embedBuilder.addField(LanguageUtil.getString(guild, Bundle.CAPTION, "music_tracks_loaded"), trackList.toString(), false);
                    if (playlist.getTracks().size() > 10)
                        embedBuilder.setFooter(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_other_tracks", playlist.getTracks().size() - 10, playlist.getTracks().size() - 10 > 1 ? "s" : ""), null);

                    textChannel.sendMessage(embedBuilder.build()).queue();

                    if (player.getTextChannel() == null) player.setTextChannel(textChannel);
                    playlist.getTracks().forEach(audioTrack -> player.playTrack(audioTrack, member, false));

                } else {

                    StringBuilder results = new StringBuilder();
                    int maxResults = settings.maxSearchResults;

                    if (maxResults > 1) {

                        for (int i = 0; i < playlist.getTracks().size() && i < maxResults; i++) {
                            AudioTrack track = playlist.getTracks().get(i);
                            if (results.length() > 1) results.append("\n");
                            results.append(String.format("`%d` - `%s` [%s](%s)", i + 1,
                                    TimeUtil.toTime(track.getDuration()),
                                    track.getInfo().title,
                                    track.getInfo().uri));
                        }

                        String url = "https://www.youtube.com/results?search_query=" + source.substring(9).replace(" ", "+");

                        textChannel.sendMessage(new EmbedBuilder()
                                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "search_results"), results.toString(), false)
                                .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "more_results"), LanguageUtil.getArguedString(guild, Bundle.STRINGS, "results_page", url), false)
                                .setFooter(LanguageUtil.getString(guild, Bundle.CAPTION, "thirty_seconds_expiration"), null)
                                .build()).queue(message -> new SelectionListener(message, member.getUser(), 30 * 1000, SelectionListener.get(playlist.getTracks().size() > maxResults ? maxResults : playlist.getTracks().size()), r -> {

                            if (member.getVoiceState().getChannel() == null) {
                                textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "must_stay_connected")).queue();
                                return;
                            }

                            if (r.isEmpty()) return;

                            if (r.size() == 1) {
                                textChannel.sendMessage(
                                        new EmbedBuilder()
                                                .setColor(ColorUtil.GREEN)
                                                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "track_added"))
                                                .setDescription(playlist.getTracks().get(r.get(0).charAt(0) - '\u0030' - 1).getInfo().title)
                                                .build()
                                ).queue();
                                if (player.getTextChannel() == null) player.setTextChannel(textChannel);
                                player.playTrack(playlist.getTracks().get(r.get(0).charAt(0) - '\u0030' - 1), member, firstPosition);
                            } else {
                                StringBuilder trackList = new StringBuilder();
                                if (player.getTextChannel() == null) player.setTextChannel(textChannel);
                                for (String c : r) {
                                    AudioTrack track = playlist.getTracks().get(c.charAt(0) - '\u0030' - 1);
                                    if (trackList.length() > 1) trackList.append("\n");
                                    trackList.append(String.format("● `%s`", track.getInfo().title));
                                }
                                textChannel.sendMessage(
                                        new EmbedBuilder()
                                                .setColor(ColorUtil.GREEN)
                                                .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "tracks_added"))
                                                .setDescription(trackList.toString())
                                                .build()
                                ).queue();

                                for (String c : r) {
                                    AudioTrack track = playlist.getTracks().get(c.charAt(0) - '\u0030' - 1);
                                    player.playTrack(track, member, false);
                                }
                            }

                        }, settings.multipleResultsSearch));

                    } else {

                        textChannel.sendMessage(
                                new EmbedBuilder()
                                        .setColor(ColorUtil.GREEN)
                                        .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "track_added"))
                                        .setDescription(playlist.getTracks().get(0).getInfo().title)
                                        .build()
                        ).queue();
                        if (player.getTextChannel() == null) player.setTextChannel(textChannel);
                        player.playTrack(playlist.getTracks().get(0), member, firstPosition);

                    }

                }

            }

            @Override
            public void noMatches() {
                textChannel.sendMessage(LanguageUtil.getString(textChannel.getGuild(), Bundle.STRINGS, "no_result")).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                textChannel.sendMessage(new EmbedBuilder()
                        .setColor(ColorUtil.RED)
                        .setTitle(LanguageUtil.getString(textChannel.getGuild(), Bundle.CAPTION, "error"))
                        .setDescription(String.format("```\n%s\n```", exception.getMessage()))
                        .setFooter(LanguageUtil.getTimeExpiration(textChannel.getGuild(), 20, TimeUnit.SECONDS), null)
                        .build()
                ).queue(message -> message.delete().queueAfter(20, TimeUnit.SECONDS));
            }
        });
    }
}
