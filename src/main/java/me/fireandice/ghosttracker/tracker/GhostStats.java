package me.fireandice.ghosttracker.tracker;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.api.PriceData;
import me.fireandice.ghosttracker.config.GhostConfig;

import java.text.DecimalFormat;

public class GhostStats {

    public int sorrowCount, voltaCount, plasmaCount, bootsCount, coinsCount;
    public int kills;
    public long totalMf;
    public int mfDropCount;
    public float totalXp;
    public long scavenger;

    public long totalValue() {
        return (long) (sorrowCount * (double) PriceData.sorrowPrice
            + voltaCount * (double) PriceData.voltaPrice
            + plasmaCount * (double) PriceData.plasmaPrice
            + bootsCount * (double) PriceData.BOOTS_PRICE
            + coinsCount * 1_000_000d
            + scavenger);
    }

    public int count(GhostDrops drop) {
        return switch (drop) {
            case SORROW -> sorrowCount;
            case VOLTA -> voltaCount;
            case PLASMA -> plasmaCount;
            case BOOTS -> bootsCount;
            case COINS -> coinsCount;
        };
    }

    private Float averageMf() {
        return mfDropCount > 0 ? (float) totalMf / mfDropCount : null;
    }

    public String averageMf(DecimalFormat format) {
        Float mf = averageMf();
        return mf == null ? "-" : format.format(mf);
    }

    public String averageXp(DecimalFormat format) {
        return kills == 0 ? "-" : format.format(totalXp / kills);
    }

    private Float relativeDifference(GhostDrops drop) {
        if (kills == 0) return null;
        GhostConfig config = GhostConfig.INSTANCE;
        float chanceModifier = 1f;
        float avgMf = averageMf() == null ? 0f : averageMf();

        switch (drop) {
            case SORROW, VOLTA, PLASMA -> {
                chanceModifier += avgMf / 100;
                chanceModifier += config.lootingLevel * 0.15f;
            }
            case BOOTS -> {
                chanceModifier += avgMf / 100;
                chanceModifier += config.luckLevel * 0.05f;
            }
            case COINS -> {
            }
        }
        int actual = count(drop);
        if (actual == 0) return null; // would display "-100.00%"

        float theoretical = kills * drop.baseChance * chanceModifier;
        return (actual - theoretical) / theoretical;
    }

    /** e.g. " (+1.20%)", or "" if there's nothing to show */
    public String percentDifference(GhostDrops drop, DecimalFormat format) {
        Float diff = relativeDifference(drop);
        if (diff == null) return "";
        String percent = format.format(diff * 100);
        return diff >= 0 ? " (+" + percent + "%)" : " (" + percent + "%)";
    }

    public void reset() {
        sorrowCount = voltaCount = plasmaCount = bootsCount = coinsCount = 0;
        kills = 0;
        totalMf = 0;
        mfDropCount = 0;
        totalXp = 0f;
        scavenger = 0;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("sorrowCount", sorrowCount);
        o.addProperty("voltaCount", voltaCount);
        o.addProperty("plasmaCount", plasmaCount);
        o.addProperty("bootsCount", bootsCount);
        o.addProperty("coinsCount", coinsCount);
        o.addProperty("kills", kills);
        o.addProperty("totalMf", totalMf);
        o.addProperty("mfDropCount", mfDropCount);
        o.addProperty("totalXp", totalXp);
        o.addProperty("scavenger", scavenger);
        return o;
    }

    public void fromJson(JsonObject o) {
        sorrowCount = readNum(o, "sorrowCount").intValue();
        voltaCount = readNum(o, "voltaCount").intValue();
        plasmaCount = readNum(o, "plasmaCount").intValue();
        bootsCount = readNum(o, "bootsCount").intValue();
        coinsCount = readNum(o, "coinsCount").intValue();
        kills = readNum(o, "kills").intValue();
        totalMf = readNum(o, "totalMf").longValue();
        mfDropCount = readNum(o, "mfDropCount").intValue();
        totalXp = readNum(o, "totalXp").floatValue();
        scavenger = readNum(o, "scavenger").longValue();
    }

    private static Number readNum(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null) return 0;
        try {
            return e.getAsNumber();
        } catch (Exception ex) {
            GhostTrackerClient.LOGGER.error("{} couldn't be read as a number", key);
            return 0;
        }
    }
}
