package fr.slama.yeahbot.blub;

import fr.slama.yeahbot.managers.SanctionManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.concurrent.TimeUnit;

/**
 * Created on 2019-06-16.
 */
public class Sanction {

    private final Type type;
    private final int duration;
    private final TimeUnit unit;

    public Sanction() {
        this.type = null;
        this.duration = 0;
        this.unit = null;
    }

    public Sanction(Type type) {
        this(type, -1, null);
    }

    public Sanction(Type type, int duration, TimeUnit unit) {
        this.type = type;
        this.duration = duration;
        this.unit = unit;
    }

    public Type getType() {
        return type;
    }

    public long getDuration() {
        return unit != null ? unit.toMillis(duration) : -1;
    }

    public void apply(TextChannel textChannel, Member member, String reason) {
        if (type == null) return;
        Guild guild = textChannel.getGuild();
        switch (type) {
            case MUTE:
                SanctionManager.registerMute(guild.getSelfMember(), member, textChannel, reason, duration, unit);
                return;
            case KICK:
                SanctionManager.registerKick(guild.getSelfMember(), member, textChannel, reason);
                return;
            case BAN:
                SanctionManager.registerBan(guild.getSelfMember(), member, textChannel, reason);
                return;
            default:
        }
    }

    public enum Type {

        MUTE, KICK, BAN

    }

}
