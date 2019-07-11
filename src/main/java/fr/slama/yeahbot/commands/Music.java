package fr.slama.yeahbot.commands;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.Paginator;
import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.commands.core.CommandError;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.managers.MusicManager;
import fr.slama.yeahbot.music.MusicPlayer;
import fr.slama.yeahbot.music.PlayerSequence;
import fr.slama.yeahbot.music.Playlist;
import fr.slama.yeahbot.music.Track;
import fr.slama.yeahbot.redis.RedisData;
import fr.slama.yeahbot.redis.buckets.Playlists;
import fr.slama.yeahbot.redis.buckets.Settings;
import fr.slama.yeahbot.utilities.ColorUtil;
import fr.slama.yeahbot.utilities.EmoteUtil;
import fr.slama.yeahbot.utilities.LanguageUtil;
import fr.slama.yeahbot.utilities.TimeUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created on 09/09/2018.
 */
public class Music {

    private final MusicManager manager = YeahBot.getInstance().getMusicManager();

    @Command(name = "play",
            discordPermission = {Permission.VOICE_SPEAK, Permission.VOICE_CONNECT},
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void play(Guild guild, TextChannel textChannel, Member member, String[] args, BotCommand cmd) {

        if (guild == null) return;
        if (isDisconnected(guild, member, textChannel)) return;

        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
            return;
        }

        String query = String.join(" ", args);

        boolean firstPosition = false;

        if (query.contains("--next")) {
            query = query.replace("--next", "");
            firstPosition = true;
        }

        query = query.trim();

        try {
            new URL(query);
        } catch (MalformedURLException e) {
            query = "ytsearch:" + query;
        }

        manager.loadTrack(textChannel, query, member, firstPosition);

    }

    @Command(name = "skip",
            permission = Command.CommandPermission.DJ,
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void skip(Guild guild, TextChannel textChannel, Member member, String[] args, BotCommand cmd) {

        if (guild == null) return;
        if (isDisconnected(guild, member, textChannel)) return;

        MusicPlayer player = manager.getPlayer(guild);

        if (player.getAudioPlayer().getPlayingTrack() == null) return;

        if (player.getTrackScheduler().getCurrentRequesterId() != member.getUser().getIdLong() &&
                !Command.CommandPermission.STAFF.test(member)) {
            List<Long> votes = player.getTrackScheduler().getVotingUsers();
            if (votes.contains(member.getUser().getIdLong())) votes.remove(member.getUser().getIdLong());
            else votes.add(member.getUser().getIdLong());
            if (votes.size() * 2 > guild.getSelfMember().getVoiceState().getChannel().getMembers().stream()
                    .filter(m -> !m.getUser().isBot()).toArray().length) {
                textChannel.sendMessage(
                        LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_voted_to_skip", votes.size())
                ).queue();
                player.getTrackScheduler().getVotingUsers().clear();
                player.skipTrack();
            } else {
                textChannel.sendMessage(
                        LanguageUtil.getArguedString(guild, Bundle.STRINGS, "user_voting_to_skip",
                                (guild.getSelfMember().getVoiceState().getChannel().getMembers().stream()
                                        .filter(m -> !m.getUser().isBot()).toArray().length / 2) - votes.size() + 1)
                ).queue();
            }
            return;
        }

        int amount;

        if (args.length == 0) {
            amount = 1;
        } else {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                textChannel.sendMessage(
                        new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.INTEGER).toEmbed()
                ).queue();
                return;
            }
        }

        for (int i = 0; i < amount - 1; i++) {
            if (player.getTrackScheduler().getQueue().getFirst().getRequesterId() == member.getUser().getIdLong() ||
                    Command.CommandPermission.STAFF.test(member))
                player.removeNextTrack();
            else break;
        }

        player.skipTrack();

    }

    @Command(name = "stop",
            permission = Command.CommandPermission.DJ,
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void stop(Guild guild, TextChannel textChannel, Member member) {

        if (guild == null) return;
        if (isDisconnected(guild, member, textChannel)) return;

        MusicPlayer player = manager.getPlayer(guild);

        if (player.getAudioPlayer().getPlayingTrack() == null) return;

        if (!Command.CommandPermission.STAFF.test(member)) for (Track track : player.getTrackScheduler().getQueue()) {
            if (track.getRequesterId() != member.getUser().getIdLong()) {
                textChannel.sendMessage(new EmbedBuilder()
                        .setColor(ColorUtil.RED)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.CAPTION, "no_permission"))
                        .build()).queue();
                return;
            }
        }

        player.getTrackScheduler().stop();
        textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "music_stopped")).queue();

    }

    @Command(name = "pause",
            aliases = "unpause",
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void pause(Guild guild, TextChannel textChannel, Member member) {

        if (guild == null) return;
        if (isDisconnected(guild, member, textChannel)) return;

        MusicPlayer player = manager.getPlayer(guild);

        if (player.getAudioPlayer().getPlayingTrack() == null) return;

        player.getTrackScheduler().togglePause();
    }

    @Command(name = "nowplaying",
            aliases = {"np"},
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void nowplaying(Guild guild, TextChannel textChannel) {

        if (guild == null) return;
        if (manager.getPlayer(guild).getTrackScheduler().getCurrentTrack() == null) return;

        Track track = manager.getPlayer(guild).getTrackScheduler().getCurrentTrack();
        Settings settings = RedisData.getSettings(guild);

        String requesterName = guild.getMemberById(track.getRequesterId()).getEffectiveName();
        String requesterAvatarUrl = guild.getMemberById(track.getRequesterId()).getUser().getAvatarUrl();

        long duration = track.getAudioTrack().getDuration();
        long position = track.getAudioTrack().getPosition();

        StringBuilder durationBar = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            if (i == position * 15 / duration) durationBar.append("|");
            durationBar.append("━");
        }

        textChannel.sendMessage(
                new EmbedBuilder()
                        .setColor(ColorUtil.BLUE)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "now_playing"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_track", track.getAudioTrack().getInfo().title, track.getAudioTrack().getInfo().uri))
                        .addField(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "music_player_volume"),
                                EmoteUtil.getVolumeEmote(settings.playerVolume) + " " + settings.playerVolume + "%",
                                true
                        )
                        .addField(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "music_player_sequence"),
                                EmoteUtil.getSequenceEmote(settings.playerSequence) + " " + LanguageUtil.getString(guild, Bundle.CAPTION, RedisData.getSettings(guild).playerSequence.toKey()),
                                true
                        )
                        .addField(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "music_track_duration"),
                                "⏱ " + TimeUtil.toTime(track.getAudioTrack().getDuration()),
                                true
                        )
                        .addField(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "music_track_author"),
                                "\uD83C\uDF99 " + track.getAudioTrack().getInfo().author,
                                true
                        )
                        .addField(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "music_track_position"),
                                "⌛ " + TimeUtil.toTime(position) + " **" + durationBar.toString() + "**",
                                true
                        )
                        .setFooter(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_submitted_by", requesterName), requesterAvatarUrl)
                        .build()
        ).queue();

    }

    @Command(name = "queue",
            discordPermission = Permission.MESSAGE_ADD_REACTION,
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void queue(Guild guild, TextChannel textChannel, User user, Message message) {

        LinkedList<Track> queue = manager.getPlayer(guild).getTrackScheduler().getQueue();

        if (queue.size() < 1) {
            textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "empty_queue")).queue();
            return;
        }

        List<Track> tracks = new ArrayList<>(queue);
        long duration = 0;
        for (Track track : tracks) duration += track.getAudioTrack().getDuration();
        long finalDuration = duration;

        new Paginator.Builder<Track>()
                .textChannel(textChannel)
                .user(user)
                .objectList(tracks)
                .objectName(t -> "`" + t.getAudioTrack().getInfo().title + "`")
                .listTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "music_tracks"))
                .embedCustomizer(b -> b.setColor(ColorUtil.BLUE)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "music_queue"))
                        .addField(LanguageUtil.getString(guild, Bundle.CAPTION, "music_track_duration"),
                                String.format("⏱ %s", TimeUtil.toTime(finalDuration)), false)
                )
                .pageSize(10)
                .ordered(true)
                .closeable(true)
                .timeout(1, TimeUnit.MINUTES)
                .build();

        message.delete().queue();

    }

    @Command(name = "clear",
            permission = Command.CommandPermission.DJ,
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void clear(Guild guild, TextChannel textChannel, Member member) {

        if (guild == null) return;
        if (isDisconnected(guild, member, textChannel)) return;

        MusicPlayer player = manager.getPlayer(guild);

        if (hasNotPermission(guild, member, textChannel, player)) return;

        manager.getPlayer(guild).getTrackScheduler().getQueue().clear();
        textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "music_queue_cleared")).queue();

    }

    @Command(name = "shuffle",
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void shuffle(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd) {

        if (guild == null) return;

        Settings settings = RedisData.getSettings(guild);

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "on":
                    settings.shuffle = true;
                    break;
                case "off":
                    settings.shuffle = false;
                    break;
                default:
                    textChannel.sendMessage(
                            new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
                    ).queue();
                    return;
            }
        }

        PlayerSequence sequence = setNewSequence(guild, settings);

        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "shuffle_mode"))
                        .setDescription(LanguageUtil.getState(guild, settings.shuffle))
                        .addField(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "music_player_sequence"),
                                String.format("%s %s",
                                        EmoteUtil.getSequenceEmote(settings.playerSequence),
                                        LanguageUtil.getString(guild, Bundle.CAPTION, sequence.toKey())),
                                false
                        )
                        .setColor(settings.shuffle ? ColorUtil.GREEN : ColorUtil.RED)
                        .build()
        ).queue();

    }

    @Command(name = "repeat",
            aliases = {"loop"},
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void loop(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd) {

        if (guild == null) return;

        Settings settings = RedisData.getSettings(guild);

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "off":
                    settings.loop = 0;
                    break;
                case "track":
                case "one":
                    settings.loop = 1;
                    break;
                case "all":
                case "queue":
                    settings.loop = 2;
                    break;
                default:
                    textChannel.sendMessage(
                            new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
                    ).queue();
                    return;
            }
        }

        PlayerSequence sequence = setNewSequence(guild, settings);

        String state;
        if (settings.loop == 0) {
            state = LanguageUtil.getState(guild, false);
        } else if (settings.loop == 1) {
            state = LanguageUtil.getString(guild, Bundle.CAPTION, "track");
        } else {
            state = LanguageUtil.getState(guild, true);
        }

        textChannel.sendMessage(
                new EmbedBuilder()
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "loop_mode"))
                        .setDescription(state)
                        .addField(
                                LanguageUtil.getString(guild, Bundle.CAPTION, "music_player_sequence"),
                                String.format("%s %s",
                                        EmoteUtil.getSequenceEmote(settings.playerSequence),
                                        LanguageUtil.getString(guild, Bundle.CAPTION, sequence.toKey())),
                                false
                        )
                        .setColor(settings.loop > 0 ? ColorUtil.GREEN : ColorUtil.RED)
                        .build()
        ).queue();

    }

    @Command(name = "volume",
            aliases = "vol",
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void volume(Guild guild, TextChannel textChannel, String[] args, BotCommand cmd) {

        if (guild == null) return;

        Settings settings = RedisData.getSettings(guild);

        if (args.length == 0) {

            int volume = settings.playerVolume;
            textChannel.sendMessage(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_current_volume", EmoteUtil.getVolumeEmote(volume), volume)).queue();

        } else {

            int volume;

            try {
                volume = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                textChannel.sendMessage(
                        new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.INTEGER).toEmbed()
                ).queue();
                return;
            }

            if (volume < 0) volume = 0;
            if (volume > 100) volume = 100;

            settings.playerVolume = volume;
            RedisData.setSettings(guild, settings);
            manager.getPlayer(guild).getAudioPlayer().setVolume(volume);

            textChannel.sendMessage(LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_set_volume", EmoteUtil.getVolumeEmote(volume), volume)).queue();

        }

    }

    @Command(name = "seek",
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void seek(Guild guild, TextChannel textChannel, Member member, String[] args, BotCommand cmd) {

        if (guild == null) return;
        if (isDisconnected(guild, member, textChannel)) return;

        MusicPlayer player = manager.getPlayer(guild);

        if (player.getAudioPlayer().getPlayingTrack() == null) return;

        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
        } else {
            int hours = 0;
            int minutes = 0;
            int seconds;
            try {
                String[] time = args[0].split(":");
                switch (time.length) {
                    case 1:
                        seconds = Integer.parseInt(time[0]);
                        break;
                    case 2:
                        minutes = Integer.parseInt(time[0]);
                        seconds = Integer.parseInt(time[1]);
                        break;
                    case 3:
                        hours = Integer.parseInt(time[0]);
                        minutes = Integer.parseInt(time[1]);
                        seconds = Integer.parseInt(time[2]);
                        break;
                    default:
                        cmd.sendUsageEmbed(textChannel);
                        return;
                }
            } catch (NumberFormatException e) {
                textChannel.sendMessage(
                        new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.INTEGER).toEmbed()
                ).queue();
                return;
            }
            long seek = seconds * 1000 + minutes * 60000 + hours * 3600000;

            if (player.getTrackScheduler().getCurrentRequesterId() == member.getUser().getIdLong() ||
                    Command.CommandPermission.STAFF.test(member)) {
                player.getAudioPlayer().getPlayingTrack().setPosition(seek);
                textChannel.sendMessage(
                        LanguageUtil.getArguedString(guild, Bundle.STRINGS, "music_navigated_to",
                                TimeUtil.toTime(seek))
                ).queue();
            } else {
                textChannel.sendMessage(new EmbedBuilder()
                        .setColor(ColorUtil.RED)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                        .setDescription(LanguageUtil.getArguedString(guild, Bundle.ERROR, "not_dj",
                                guild.getMemberById(player.getTrackScheduler().getCurrentRequesterId()).getAsMention()))
                        .build()).queue();
            }
        }

    }

    @Command(name = "playlist",
            aliases = "pl",
            discordPermission = {Permission.VOICE_SPEAK, Permission.VOICE_CONNECT},
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void playlist(Guild guild, TextChannel textChannel, Member member, String[] args, BotCommand cmd) {

        if (guild == null) return;
        if (isDisconnected(guild, member, textChannel)) return;
        if (hasNotValue(guild, textChannel, cmd, args)) return;

        Playlists playlists = RedisData.getPlaylists(guild);
        String name = String.join(" ", args).toLowerCase();

        if (!playlists.getPlaylists().containsKey(name)) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setColor(ColorUtil.RED)
                            .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "unknown_playlist"))
                            .setDescription(LanguageUtil.getString(guild, Bundle.ERROR, "unknown_playlist"))
                            .build()
            ).queue();
            return;
        }

        Playlist playlist = playlists.getPlaylists().get(name);

        manager.loadTrack(textChannel, playlist.getUrl(), member, false, false);

    }

    @Command(name = "addplaylist",
            aliases = {"addpl", "apl"},
            category = Command.CommandCategory.MUSIC,
            executor = Command.CommandExecutor.USER)
    private void addPlaylist(Guild guild, TextChannel textChannel, Member member, String[] args, BotCommand cmd) {

        if (guild == null) return;

        StringBuilder builder = new StringBuilder();
        URL url = null;
        Playlists playlists = RedisData.getPlaylists(guild);

        for (String s : args) {
            try {
                url = new URL(s);
                break;
            } catch (MalformedURLException e) {
                if (builder.length() > 1) builder.append(" ");
                builder.append(s.toLowerCase());
            }
        }

        if (builder.length() < 1) {
            textChannel.sendMessage(
                    new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
            return;
        }

        if (url == null) {
            textChannel.sendMessage(
                    new CommandError(cmd, cmd.getArguments(guild)[1], guild, CommandError.ErrorType.URL).toEmbed()
            ).queue();
            return;
        }

        if (playlists.getPlaylists().containsKey(builder.toString())) {
            textChannel.sendMessage(
                    LanguageUtil.getString(guild, Bundle.STRINGS, "playlist_already_saved")
            ).queue();
            return;
        }

        playlists.getPlaylists().put(builder.toString(), new Playlist(url.toString(), builder.toString(), member.getUser().getIdLong()));
        RedisData.setPlaylists(guild, playlists);
        textChannel.sendMessage(
                LanguageUtil.getString(guild, Bundle.STRINGS, "playlist_added")
        ).queue();

    }

    @Command(name = "delplaylist",
            aliases = {"delpl", "dpl"},
            category = Command.CommandCategory.MUSIC,
            permission = Command.CommandPermission.PLAYLIST_OWNER,
            executor = Command.CommandExecutor.USER)
    private void delPlaylist(Guild guild, TextChannel textChannel, Member member, String[] args, BotCommand cmd) {

        if (guild == null) return;
        if (hasNotValue(guild, textChannel, cmd, args)) return;

        Playlists playlists = RedisData.getPlaylists(guild);
        String name = String.join(" ", args).toLowerCase();

        if (!playlists.getPlaylists().containsKey(name)) {
            textChannel.sendMessage(
                    new EmbedBuilder()
                            .setColor(ColorUtil.RED)
                            .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "unknown_playlist"))
                            .setDescription(LanguageUtil.getString(guild, Bundle.ERROR, "playlist_not_found"))
                            .build()
            ).queue();
            return;
        }

        if (!Command.CommandPermission.STAFF.test(member) &&
                playlists.getPlaylists().get(name).getOwnerLong() != member.getUser().getIdLong()) {
            textChannel.sendMessage(new EmbedBuilder()
                    .setColor(ColorUtil.RED)
                    .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                    .setDescription(LanguageUtil.getString(guild, Bundle.CAPTION, "no_permission"))
                    .build()).queue();
            return;
        }

        playlists.getPlaylists().remove(name);
        RedisData.setPlaylists(guild, playlists);
        textChannel.sendMessage(
                LanguageUtil.getString(guild, Bundle.STRINGS, "playlist_deleted")
        ).queue();

    }

    private PlayerSequence setNewSequence(Guild guild, Settings settings) {
        switch (settings.loop) {
            case 0:
                settings.playerSequence = settings.shuffle ? PlayerSequence.SHUFFLE : PlayerSequence.NORMAL;
                RedisData.setSettings(guild, settings);
                return settings.shuffle ? PlayerSequence.SHUFFLE : PlayerSequence.NORMAL;
            case 1:
                settings.playerSequence = PlayerSequence.LOOP;
                RedisData.setSettings(guild, settings);
                return PlayerSequence.LOOP;
            case 2:
                settings.playerSequence = settings.shuffle ? PlayerSequence.SHUFFLE_QUEUE_LOOP : PlayerSequence.QUEUE_LOOP;
                RedisData.setSettings(guild, settings);
                return settings.shuffle ? PlayerSequence.SHUFFLE_QUEUE_LOOP : PlayerSequence.QUEUE_LOOP;
            default:
                return PlayerSequence.NORMAL;
        }
    }

    private boolean isDisconnected(Guild guild, Member member, TextChannel textChannel) {
        if (!guild.getAudioManager().isConnected() && !guild.getAudioManager().isAttemptingToConnect()) {
            VoiceChannel channel = member.getVoiceState().getChannel();
            if (channel == null) {
                textChannel.sendMessage(LanguageUtil.getString(guild, Bundle.STRINGS, "must_be_connected")).queue();
                return true;
            }
        }
        return false;
    }

    private boolean hasNotPermission(Guild guild, Member member, TextChannel textChannel, MusicPlayer player) {
        if (!Command.CommandPermission.STAFF.test(member)) for (Track track : player.getTrackScheduler().getQueue()) {
            if (track.getRequesterId() != member.getUser().getIdLong()) {
                textChannel.sendMessage(new EmbedBuilder()
                        .setColor(ColorUtil.RED)
                        .setTitle(LanguageUtil.getString(guild, Bundle.CAPTION, "error"))
                        .setDescription(LanguageUtil.getString(guild, Bundle.CAPTION, "no_permission"))
                        .build()).queue();
                return true;
            }
        }
        return false;
    }

    private boolean hasNotValue(Guild guild, TextChannel textChannel, BotCommand cmd, String[] args) {
        if (args.length == 0) {
            textChannel.sendMessage(
                    new CommandError(cmd, cmd.getArguments(guild)[0], guild, CommandError.ErrorType.MISSING_VALUE).toEmbed()
            ).queue();
            return true;
        }
        return false;
    }
}
