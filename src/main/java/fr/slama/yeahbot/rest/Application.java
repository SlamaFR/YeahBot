package fr.slama.yeahbot.rest;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.blub.Track;
import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.music.MusicPlayer;
import fr.slama.yeahbot.music.PlayerSequence;
import fr.slama.yeahbot.utilities.LanguageUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import org.jooby.Jooby;
import org.jooby.Status;
import org.jooby.handlers.CorsHandler;
import org.jooby.json.Jackson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.Collectors;

/**
 * Created on 30/12/2018.
 */
public class Application extends Jooby {

    private YeahBot yeahBot = YeahBot.getInstance();

    public Application() {

        use(new Jackson());
        use("*", new CorsHandler());

        // STATS
        get("/guildCount", (req, rsp) -> rsp.send(
                new JSONObject()
                        .put("code", 200)
                        .put("message", "OK")
                        .put("content", yeahBot.getShardManager().getGuildCache().size())
                        .toMap())
        );
        get("/userCount", (req, rsp) -> rsp.send(
                new JSONObject()
                        .put("code", 200)
                        .put("message", "OK")
                        .put("content", yeahBot.getShardManager().getUserCache().stream().filter(u ->
                                !u.isBot()).toArray().length)
                        .toMap())
        );
        get("/channelCount", (req, rsp) -> rsp.send(
                new JSONObject()
                        .put("code", 200)
                        .put("message", "OK")
                        .put("content", yeahBot.getShardManager().getTextChannelCache().size())
                        .toMap())
        );

        // ENTITIES METADATA

        get("/guilds", (req, rsp) -> rsp.send(
                new JSONObject()
                        .put("code", 200)
                        .put("message", "OK")
                        .put("content", yeahBot.getShardManager().getGuildCache()
                                .stream()
                                .map(ISnowflake::getId)
                                .collect(Collectors.toList()))
                        .toMap())
        );
        get("/guild/{id:\\d+}", (req, rsp) -> {
            Guild guild = yeahBot.getShardManager().getGuildById(req.param("id").longValue());

            if (guild == null) {
                rsp.status(Status.NOT_FOUND);
                rsp.send(new JSONObject()
                        .put("code", 404)
                        .put("message", "Not found")
                        .put("error", "unknown_guild").toMap());
                return;
            }

            JSONObject channels = new JSONObject()
                    .put("textChannels", new JSONArray())
                    .put("voiceChannels", new JSONArray());
            JSONArray roles = new JSONArray();

            guild.getRoles().forEach(role -> roles
                    .put(new JSONObject()
                            .put("id", role.getId())
                            .put("name", role.getName())
                            .put("permissions", role.getPermissionsRaw())
                            .put("color_raw", role.getColorRaw())));

            guild.getVoiceChannelCache().forEach(channel -> channels
                    .getJSONArray("voiceChannels").put(new JSONObject()
                            .put("id", channel.getId())
                            .put("bitrate", channel.getBitrate())
                            .put("name", channel.getName())));

            guild.getTextChannels().forEach(channel -> channels
                    .getJSONArray("textChannels").put(new JSONObject()
                            .put("id", channel.getId())
                            .put("name", channel.getName())
                            .put("topic", channel.getTopic())
                            .put("slowmode", channel.getSlowmode())));

            rsp.send(new JSONObject()
                    .put("code", 200)
                    .put("message", "OK")
                    .put("content", new JSONObject()
                            .put("name", guild.getName())
                            .put("iconUrl", guild.getIconUrl())
                            .put("memberCount", guild.getMemberCache().size())
                            .put("ownerId", guild.getOwnerId())
                            .put("channels", channels)
                            .put("roles", roles))
                    .toMap()
            );
        });
        get("/member/{guildId:\\d+}/{userId:\\d+}", (req, rsp) -> {
            Guild guild = yeahBot.getShardManager().getGuildById(req.param("guildId").longValue());
            if (guild == null) {
                rsp.status(Status.NOT_FOUND);
                rsp.send(new JSONObject()
                        .put("code", 404)
                        .put("message", "Not found")
                        .put("error", "unknown_guild").toMap());
                return;
            }
            Member member = guild.getMemberById(req.param("userId").longValue());
            if (member == null) {
                rsp.status(Status.NOT_FOUND);
                rsp.send(new JSONObject()
                        .put("code", 404)
                        .put("message", "Not found")
                        .put("error", "unknown_member").toMap());
                return;
            }

            rsp.send(new JSONObject()
                    .put("code", 200)
                    .put("message", "OK")
                    .put("content", new JSONObject()
                            .put("nickname", member.getEffectiveName())
                            .put("id", member.getUser().getId())
                            .put("name", member.getUser().getName())
                            .put("discriminator", member.getUser().getDiscriminator())
                            .put("isBot", member.getUser().isBot()))
                    .toMap()
            );
        });

        // MUSIC
        get("/music/{guildId:\\d+}", (req, rsp) -> {
            Guild guild = yeahBot.getShardManager().getGuildById(req.param("guildId").longValue());
            if (guild == null) {
                rsp.status(Status.NOT_FOUND);
                rsp.send(new JSONObject()
                        .put("code", 404)
                        .put("message", "Not found")
                        .put("error", "unknown_guild").toMap());
                return;
            }
            MusicPlayer player = yeahBot.getMusicManager().getPlayer(guild);
            Track track = player.getTrackScheduler().getCurrentTrack();
            if (player.getAudioPlayer().getPlayingTrack() == null) {
                rsp.send(new JSONObject()
                        .put("code", 200)
                        .put("message", "OK")
                        .put("content", "not_playing")
                        .toMap()
                );
                return;
            }
            if (player.getGuild().getSelfMember().getVoiceState() == null ||
                    player.getGuild().getSelfMember().getVoiceState().getChannel() == null) {
                rsp.send(new JSONObject()
                        .put("code", 200)
                        .put("message", "OK")
                        .put("content", "not_connected")
                        .toMap()
                );
                return;
            }
            rsp.send(new JSONObject()
                    .put("code", 200)
                    .put("message", "OK")
                    .put("content", new JSONObject()
                            .put("requesterId", track.getRequesterId())
                            .put("channelId", player.getGuild().getSelfMember().getVoiceState().getChannel().getId())
                            .put("title", track.getAudioTrack().getInfo().title)
                            .put("author", track.getAudioTrack().getInfo().author)
                            .put("url", track.getAudioTrack().getInfo().uri)
                            .put("duration", track.getAudioTrack().getDuration())
                            .put("paused", player.getAudioPlayer().isPaused())
                            .put("position", player.getAudioPlayer().getPlayingTrack().getPosition())
                    ).toMap());
        });

        // COMMANDS AND CATEGORIES
        get("/commands", (req, rsp) -> {
            JSONArray array = new JSONArray();
            for (BotCommand cmd : YeahBot.getInstance().getCommandMap().getRegistry()) {
                if (!cmd.getExecutor().equals(Command.CommandExecutor.USER)) continue;
                if (!cmd.displayInHelp()) continue;

                JSONObject object = new JSONObject();
                object.put("name", cmd.getName());
                object.put("aliases", cmd.getAliases());

                JSONObject description = new JSONObject();
                JSONObject arguments = new JSONObject();
                JSONObject argumentsDescription = new JSONObject();
                JSONObject usage = new JSONObject();
                JSONObject permission = new JSONObject();
                JSONObject discordPermission = new JSONObject();
                JSONObject category = new JSONObject();
                for (String l : Language.codeValues()) {
                    description.put(l, cmd.getDescription(l));
                    arguments.put(l, cmd.getArguments(l));
                    argumentsDescription.put(l, cmd.getArgumentsDescription(l));
                    usage.put(l, cmd.getUsage(l));
                    permission.put(l, LanguageUtil.getString(l, Bundle.PERMISSION, cmd.getPermission().toString().toLowerCase()));
                    StringBuilder builder = new StringBuilder();
                    for (Permission p : cmd.getDiscordPermission()) {
                        if (builder.length() > 1) builder.append(", ");
                        builder.append(LanguageUtil.getString(l, Bundle.PERMISSION, p.toString().toLowerCase()));
                    }
                    discordPermission.put(l, builder.toString());
                    category.put(l, LanguageUtil.getString(l, Bundle.CATEGORY, cmd.getCategory().getName()));
                }
                category.put("raw", cmd.getCategory().toString());
                object.put("description", description);
                object.put("arguments", arguments);
                object.put("argumentsDescription", argumentsDescription);
                object.put("usage", usage);
                object.put("permission", permission);
                object.put("discordPermission", discordPermission);
                object.put("category", category);

                object.put("disabled", YeahBot.getInstance().getCommandMap().getDisabledCommands().contains(cmd));

                array.put(object.toMap());
            }
            rsp.send(new JSONObject()
                    .put("code", 200)
                    .put("message", "OK")
                    .put("content", array.toList())
                    .toMap());
        });
        get("/categories", (req, rsp) -> rsp.send(new JSONObject()
                .put("code", 200)
                .put("message", "OK")
                .put("content", Command.CommandCategory.values())
                .toMap()));

        // AVAILABLE VALUES
        get("/sequences", (req, rsp) -> rsp.send(new JSONObject()
                .put("code", 200)
                .put("message", "OK")
                .put("content", PlayerSequence.values())
                .toMap()));
        get("/languages", (req, rsp) -> rsp.send(new JSONObject()
                .put("code", 200)
                .put("message", "OK")
                .put("content", Language.values())
                .toMap()));

        // DEFAULT
        get("/", (req, rsp) -> rsp.send(new JSONObject()
                .put("code", 200)
                .put("message", "OK").toMap()));
        get("*", (req, rsp) -> {
            rsp.status(Status.NOT_FOUND);
            rsp.send(new JSONObject()
                    .put("code", 404)
                    .put("message", "Not found")
                    .put("error", "unknown_endpoint").toMap());
        });

        // STATUS SYSTEM
        head("/", (req, rsp) -> rsp.send(new JSONObject()
                .put("code", 200)
                .put("message", "OK").toMap()));
    }

}
