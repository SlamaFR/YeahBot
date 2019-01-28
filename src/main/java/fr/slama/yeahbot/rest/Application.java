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
import org.jooby.json.Jackson;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created on 30/12/2018.
 */
public class Application extends Jooby {

    private YeahBot yeahBot = YeahBot.getInstance();

    {

        use(new Jackson());
        get("/guildCount", (req, rsp) -> rsp.send(yeahBot.getShardManager().getGuildCache().size()));
        get("/userCount", (req, rsp) -> rsp.send(yeahBot.getShardManager().getUserCache().stream().filter(u -> !u.isBot()).toArray().length));
        get("/channelCount", (req, rsp) -> rsp.send(yeahBot.getShardManager().getTextChannelCache().size()));
        get("/guild/{id:\\d+}", (request, response) -> {
            long id = request.param("id").longValue();
            Guild guild = yeahBot.getShardManager().getGuildById(id);
            if (guild == null) {
                response.send(new JSONObject().put("error", "unknown_guild").toMap());
                return;
            }
            JSONObject channels = new JSONObject()
                    .put("textChannels", new JSONArray())
                    .put("voiceChannels", new JSONArray());
            JSONArray roles = new JSONArray();

            guild.getRoleCache().forEach(role -> roles.put(new JSONObject().put("id", role.getId()).put("name", role.getName())));
            guild.getVoiceChannelCache().forEach(channel -> channels.put("voiceChannels", channels.getJSONArray("voiceChannels").put(channel.getPosition(), new JSONObject().put("id", channel.getId()).put("name", channel.getName()))));
            guild.getTextChannelCache().forEach(channel -> channels.put("textChannels", channels.getJSONArray("textChannels").put(channel.getPosition(), new JSONObject().put("id", channel.getId()).put("name", channel.getName()))));
            response.send(new JSONObject()
                    .put("name", guild.getName())
                    .put("iconUrl", guild.getIconUrl())
                    .put("memberCount", guild.getMemberCache().size())
                    .put("ownerId", guild.getOwnerId())
                    .put("channels", channels)
                    .put("roles", roles)
                    .toMap());
        });
        get("/member/{guildId:\\d+}/{userId:\\d+}", (request, response) -> {
            Guild guild = yeahBot.getShardManager().getGuildById(request.param("guildId").longValue());
            if (guild == null) {
                response.send(new JSONObject().put("error", "unknown_guild").toMap());
                return;
            }
            User user = yeahBot.getShardManager().getUserById(request.param("userId").longValue());
            if (user == null || guild.getMember(user) == null) {
                response.send(new JSONObject().put("error", "unknown_member").toMap());
                return;
            }
            Member member = guild.getMember(user);
            response.send(new JSONObject()
                    .put("isMember", true)
                    .put("canAddBot", member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER) || member.isOwner())
                    .put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner())
                    .toMap());
        });
        get("/commands", (request, response) -> {
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
            response.send(array.toList());
        });
        get("/categories", (request, response) -> {
            response.send(new JSONArray().put(Command.CommandCategory.values()));
        });
        get("/commands/{id:\\d+}", (request, response) -> {
            Guild guild = yeahBot.getShardManager().getGuildById(request.param("id").longValue());
            if (guild == null) {
                response.send(new JSONObject().put("error", "unknown_guild").toMap());
                return;
            }
            JSONArray array = new JSONArray();
            for (BotCommand cmd : YeahBot.getInstance().getCommandMap().getRegistry()) {
                if (!cmd.getExecutor().equals(Command.CommandExecutor.USER)) continue;

                array.put(new JSONObject()
                        .put("name", cmd.getName())
                        .put("aliases", cmd.getAliases())
                        .put("description", cmd.getDescription(guild))
                        .put("arguments", cmd.getArguments(guild))
                        .put("argumentsDescription", cmd.getArgumentsDescription(guild))
                        .put("usage", cmd.getUsage(guild)).toMap());
            }
            response.send(array.toList());
        });
        get("*", () -> "It works");
    }

}
