package me.fireandice.ghosttracker.tracker;

import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.hud.DropPopups;
import me.fireandice.ghosttracker.utils.ScoreboardUtils;
import me.fireandice.ghosttracker.utils.Utils;
import net.minecraft.client.Minecraft;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
        "\\+(?<gained>[\\d.,]+)(?: (?<skill>[A-Za-z]+))? \\((?<progress>[\\d,./]+)\\)");
    private static final NumberFormat NUMBERS = NumberFormat.getInstance(Locale.US);

    private static float previousXp = -1f;

    /** Normal chat messages */
    public static void onChat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (!ScoreboardUtils.inDwarvenMines || mc.player == null || mc.player.getY() > 100) return;

        if (COIN_DROP.matcher(message).find()) {
            GhostTrackerClient.ghostStats.coinsCount++;
            if (GhostTimer.isTracking) GhostTimer.stats.coinsCount++;
            DropPopups.add("1M Coins", "COIN DROP!", 0xFFFF55);
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

        String skill = m.group("skill");
        boolean named = skill != null;
        if (named && !skill.equalsIgnoreCase("Combat")) return;

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

        // Weapon kill counter is the kill source: only add this XP if it belongs to a ghost kill (so mining XP
        // etc. in the Mines is ignored). The line may not name the skill, so pairing with a kill is what decides.
        if (WeaponKills.active) {
            previousXp = newXp;
            if (Utils.inGhostArea()) claimXp(xpGained);
            return;
        }
        // No weapon counter: kills are derived from the XP line, which needs the skill named to be trusted
        if (!named) {
            previousXp = newXp;
            return;
        }

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
        switch (drop) {
            case SORROW -> DropPopups.add("Sorrow", "RARE DROP!  +" + magicFind + " Magic Find", 0x55FFFF);
            case VOLTA -> DropPopups.add("Volta", "RARE DROP!  +" + magicFind + " Magic Find", 0x5555FF);
            case PLASMA -> DropPopups.add("Plasma", "RARE DROP!  +" + magicFind + " Magic Find", 0xFFAA00);
            case BOOTS -> DropPopups.add("Ghostly Boots", "RARE DROP!  +" + magicFind + " Magic Find", 0xFF55FF);
            default -> {
            }
        }
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

    // ---- pairing of weapon kills with the XP line that follows (or precedes) them by up to 2s
    private static final long PAIR_WINDOW = 2000;
    private static final List<Long> pendingKills = new ArrayList<>();
    private static final List<float[]> pendingXp = new ArrayList<>(); // {timeMillisLow, xp}
    private static final List<Long> pendingXpTimes = new ArrayList<>();

    private static void prune(long now) {
        pendingKills.removeIf(t -> now - t > PAIR_WINDOW);
        for (int i = pendingXpTimes.size() - 1; i >= 0; i--) {
            if (now - pendingXpTimes.get(i) > PAIR_WINDOW) {
                pendingXpTimes.remove(i);
                pendingXp.remove(i);
            }
        }
    }

    private static void addXp(float xp) {
        GhostTrackerClient.ghostStats.totalXp += xp;
        if (GhostTimer.isTracking) GhostTimer.stats.totalXp += xp;
    }

    private static void claimXp(float xp) {
        long now = System.currentTimeMillis();
        prune(now);
        if (!pendingKills.isEmpty()) {
            pendingKills.remove(0);
            addXp(xp);
        } else {
            pendingXp.add(new float[]{0f, xp});
            pendingXpTimes.add(now);
        }
    }

    /** Called by WeaponKills when the weapon's kill counter went up. */
    static void onWeaponKills(int kills) {
        trackKills(kills, 0f);
        long now = System.currentTimeMillis();
        prune(now);
        for (int i = 0; i < kills; i++) {
            if (!pendingXp.isEmpty()) {
                addXp(pendingXp.remove(0)[1]);
                pendingXpTimes.remove(0);
            } else {
                pendingKills.add(now);
            }
        }
    }

    static void trackKills(int killsGained, float xpGained) {
        if (killsGained > 0) GhostTimer.onKill(); // may auto-start the timer before stats are added
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
