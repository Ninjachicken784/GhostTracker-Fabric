package me.fireandice.ghosttracker.tracker;

import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.utils.ScoreboardUtils;
import me.fireandice.ghosttracker.utils.Utils;
import net.minecraft.client.Minecraft;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Works on plain (unformatted) text, so no § code matching is needed. */
public final class GhostListener {

    private static final Pattern RARE_DROP = Pattern.compile(
        "RARE DROP! (?<drop>[A-Za-z ]+) \\(\\+(?<mf>\\d+)%? \\S+ Magic Find\\)");
    private static final Pattern COIN_DROP = Pattern.compile(
        "The ghost.s death materialized 1,000,000 coins from the mists!");
    private static final Pattern COMBAT_XP = Pattern.compile(
        "\\+(?<gained>[\\d.,]+) Combat \\((?<progress>.+)\\)");
    private static final NumberFormat NUMBERS = NumberFormat.getInstance(Locale.US);

    private static float previousXp = -1f;

    /** Normal chat messages */
    public static void onChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (!ScoreboardUtils.inDwarvenMines || mc.player == null || mc.player.getY() > 100) return;

        if (COIN_DROP.matcher(message).find()) {
            GhostTrackerClient.ghostStats.coinsCount++;
            if (GhostTimer.isTracking) GhostTimer.stats.coinsCount++;
            return;
        }

        Matcher m = RARE_DROP.matcher(message);
        if (m.find()) {
            GhostDrops drop = GhostDrops.byName(m.group("drop"));
            if (drop == null) return;
            trackDrops(drop, Integer.parseInt(m.group("mf")));
        }
    }

    /**
     * Some logic was taken from https://www.chattriggers.com/modules/v/GhostCounterV3.
     */
    public static void onActionBar(String message) {
        Matcher m = COMBAT_XP.matcher(message);
        if (!m.find()) return;

        float xpGained;
        float newXp;
        try {
            xpGained = NUMBERS.parse(m.group("gained")).floatValue();
            String progress = m.group("progress");
            if (progress.endsWith("/0")) progress = progress.substring(0, progress.length() - 2);
            newXp = NUMBERS.parse(progress).floatValue();
        } catch (ParseException e) {
            return;
        }
        if (xpGained <= 0f) return;

        // avoids tracking non ghost kills but still saves the xp value
        if (!Utils.inGhostArea()) {
            previousXp = newXp;
            return;
        }

        if (previousXp == -1f) { // no previously tracked kills
            trackKills(WeaponKills.active ? 0 : 1, xpGained);
        } else {
            float actualXpGained = newXp - previousXp;
            // gaining a bestiary level / 1m xp might count as lots of kills, hence the cap (15 was arbitrary)
            int killsGained = Math.min(Math.round(actualXpGained / xpGained), 15);

            // xpGained * killsGained is more accurate and avoids rounding error
            if (previousXp != 0f && killsGained >= 0) trackKills(WeaponKills.active ? 0 : killsGained, xpGained * killsGained);
        }
        previousXp = newXp;
    }

    private static void trackDrops(GhostDrops drop, int magicFind) {
        add(GhostTrackerClient.ghostStats, drop, magicFind);
        if (GhostTimer.isTracking) add(GhostTimer.stats, drop, magicFind);
    }

    private static void add(GhostStats stats, GhostDrops drop, int magicFind) {
        switch (drop) {
            case SORROW -> stats.sorrowCount++;
            case VOLTA -> stats.voltaCount++;
            case PLASMA -> stats.plasmaCount++;
            case BOOTS -> stats.bootsCount++;
            default -> {
            }
        }
        stats.totalMf += magicFind;
        stats.mfDropCount++;
    }

    static void trackKills(int killsGained, float xpGained) {
        GhostTrackerClient.ghostStats.kills += killsGained;
        GhostTrackerClient.ghostStats.totalXp += xpGained;
        if (GhostTimer.isTracking) {
            GhostTimer.stats.kills += killsGained;
            GhostTimer.stats.totalXp += xpGained;
        }
    }

    private GhostListener() {
    }
}
