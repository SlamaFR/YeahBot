package fr.slama.yeahbot.redis.buckets;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 16/11/2018.
 */
public class Reports {

    private Map<Long, Integer> spamReports = new HashMap<>();
    private Map<Long, Integer> swearingReports = new HashMap<>();
    private Map<Long, Integer> advertisingReports = new HashMap<>();

    public Reports() {
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public Map<Long, Integer> getSpamReports() {
        return spamReports;
    }

    public Map<Long, Integer> getSwearingReports() {
        return swearingReports;
    }

    public Map<Long, Integer> getAdvertisingReports() {
        return advertisingReports;
    }

    public Reports incrSpamReport(long memberId) {
        this.spamReports.merge(memberId, 1, Integer::sum);
        return this;
    }

    public Reports incrSwearingReport(long memberId) {
        this.swearingReports.merge(memberId, 1, Integer::sum);
        return this;
    }

    public Reports incrAdvertisingReport(long memberId) {
        this.advertisingReports.merge(memberId, 1, Integer::sum);
        return this;
    }

}
