package fr.slama.yeahbot.rest;

import fr.slama.yeahbot.YeahBot;
import fr.slama.yeahbot.commands.core.BotCommand;
import fr.slama.yeahbot.commands.core.Command;
import fr.slama.yeahbot.language.Bundle;
import fr.slama.yeahbot.language.Language;
import fr.slama.yeahbot.language.LanguageUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.jooby.Jooby;
import org.jooby.Status;
import org.jooby.json.Jackson;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created on 30/12/2018.
 */
public class Application extends Jooby {

    private YeahBot yeahBot = YeahBot.getInstance();
    private final String[] AVAILABLE_BADGES = new String[]{
            "guildCount",
            "userCount",
            "channelCount",
            "commandCount",
            "availableLocales"
    };

    {

        use(new Jackson());

        // Statuses
        JSONObject ok = new JSONObject()
                .put("code", 200)
                .put("message", "OK");
        JSONObject not_found = new JSONObject()
                .put("code", 404)
                .put("message", "Not found");

        JSONObject badge = new JSONObject()
                .put("schemaVersion", 1)
                .put("color", "brightgreen");

        // STATS
        get("/guildCount", (req, rsp) -> rsp.send(
                ok.put("content", yeahBot.getShardManager().getGuildCache().size())
                        .toMap())
        );
        get("/userCount", (req, rsp) -> rsp.send(
                ok.put("content", yeahBot.getShardManager().getUserCache().stream().filter(u ->
                        !u.isBot()).toArray().length)
                        .toMap())
        );
        get("/channelCount", (req, rsp) -> rsp.send(
                ok.put("content", yeahBot.getShardManager().getTextChannelCache().size())
                        .toMap())
        );

        // ENTITIES METADATA
        get("/guild/{id:\\d+}", (req, rsp) -> {
            Guild guild = yeahBot.getShardManager().getGuildById(req.param("id").longValue());

            if (guild == null) {
                rsp.status(Status.NOT_FOUND);
                rsp.send(not_found.put("error", "unknown_guild").toMap());
                return;
            }

            JSONObject channels = new JSONObject();
            JSONArray roles = new JSONArray();

            guild.getRoleCache().forEach(role -> roles
                    .put(new JSONObject()
                            .put("id", role.getId())
                            .put("name", role.getName())
                            .put("permissions", role.getPermissionsRaw())
                            .put("color_raw", role.getColorRaw())));

            guild.getVoiceChannelCache().forEach(channel -> channels
                    .put("voiceChannels", new JSONArray().put(new JSONObject()
                            .put("id", channel.getId())
                            .put("bitrate", channel.getBitrate())
                            .put("name", channel.getName()))));

            guild.getTextChannelCache().forEach(channel -> channels
                    .put("textChannels", new JSONArray().put(new JSONObject()
                            .put("id", channel.getId())
                            .put("name", channel.getName())
                            .put("topic", channel.getTopic())
                            .put("slowmode", channel.getSlowmode()))));

            rsp.send(new JSONObject()
                    .put("name", guild.getName())
                    .put("id", guild.getIdLong())
                    .put("iconUrl", guild.getIconUrl())
                    .put("memberCount", guild.getMemberCache().size())
                    .put("ownerId", guild.getOwnerId())
                    .put("channels", channels)
                    .put("roles", roles)
                    .toMap());
        });
        get("/member/{guildId:\\d+}/{userId:\\d+}", (req, rsp) -> {
            Guild guild = yeahBot.getShardManager().getGuildById(req.param("guildId").longValue());
            if (guild == null) {
                rsp.status(Status.NOT_FOUND);
                rsp.send(not_found.put("error", "unknown_guild").toMap());
                return;
            }
            User user = yeahBot.getShardManager().getUserById(req.param("userId").longValue());
            if (user == null || guild.getMember(user) == null) {
                rsp.status(Status.NOT_FOUND);
                rsp.send(not_found.put("error", "unknown_member").toMap());
                return;
            }

            Member member = guild.getMember(user);
            boolean isAdmin = member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner();
            rsp.send(new JSONObject()
                    .put("code", 200)
                    .put("message", "OK")
                    .put("content", new JSONObject()
                            .put("isBot", member.getUser().isBot())
                            .put("isAdmin", isAdmin)
                            .put("canAddBot", isAdmin || member.hasPermission(Permission.MANAGE_SERVER)))
                    .toMap()
            );
        });

        // COMMANDS AND CATEGORIES
        get("/commands", (req, rsp) -> {
            JSONArray array = new JSONArray();
            for (BotCommand cmd : YeahBot.getInstance().getCommandMap().getRegistry()) {
                if (!cmd.getExecutor().equals(Command.CommandExecutor.USER)) continue;

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
                for (String l : Language.languages) {
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
            rsp.send(array.toList());
        });
        get("/categories", (req, rsp) -> rsp.send(Command.CommandCategory.values()));

        // BADGES
        get("/badges", ((req, rsp) -> rsp.send(ok.put("content", AVAILABLE_BADGES).toMap())));
        get("/badges/:name", (req, rsp) -> {
            switch (req.param("name").to(String.class)) {
                case "guildCount":
                    rsp.send(ok.put("content", badge
                            .put("label", "Guilds")
                            .put("message", yeahBot.getShardManager().getGuildCache().size()))
                            .toMap());
                    break;
                case "userCount":
                    rsp.send(ok.put("content", badge
                            .put("label", "Users")
                            .put("message", yeahBot.getShardManager().getUserCache().stream().filter(u ->
                                    !u.isBot()).toArray().length))
                            .toMap());
                    break;
                case "channelCount":
                    rsp.send(ok.put("content", badge
                            .put("label", "Channels")
                            .put("message", yeahBot.getShardManager().getTextChannelCache().size() +
                                    yeahBot.getShardManager().getVoiceChannelCache().size()))
                            .toMap());
                    break;
                case "commandCount":
                    rsp.send(ok.put("content", badge
                            .put("label", "Commands")
                            .put("message", yeahBot.getCommandMap().getRegistry().size()))
                            .toMap());
                    break;
                case "availableLocales":
                    rsp.send(ok.put("content", badge
                            .put("label", "Available languages")
                            .put("message", Language.languages.size()))
                            .toMap());
                    break;
                default:
                    rsp.status(Status.NOT_FOUND);
                    rsp.send(not_found.put("error", "unknown_badge").toMap());
                    break;
            }
        });

        // DEFAULT
        get("/", (req, rsp) -> rsp.send(ok.toMap()));
        get("*", (req, rsp) -> {
            rsp.status(Status.NOT_FOUND);
            rsp.send(not_found.put("error", "unknown_endpoint").toMap());
        });
    }

}
