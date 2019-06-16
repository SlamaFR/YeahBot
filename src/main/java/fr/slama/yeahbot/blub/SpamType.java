package fr.slama.yeahbot.blub;

import fr.slama.yeahbot.redis.buckets.Settings;

/**
 * Created on 12/11/2018.
 */
public enum SpamType {

    FLOOD,
    EMOJIS,
    CAPS,
    REACTIONS;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public String toKey() {
        return "spam_" + this.toString();
    }

    public String toWarningKey() {
        return String.format("spam_%s_warning_sentence", this.toString());
    }

    public String getSentenceFromSettings(Settings settings) {
        switch (this) {
            case CAPS:
                return settings.capsSpamWarningSentence;
            case EMOJIS:
                return settings.emojisSpamWarningSentence;
            case REACTIONS:
                return settings.reactionsSpamWarningSentence;
            default:
                return settings.floodWarningSentence;
        }
    }

}
