package me.fireandice.ghosttracker.hud;

import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.config.GhostConfig;
import me.fireandice.ghosttracker.tracker.GhostDrops;
import me.fireandice.ghosttracker.tracker.GhostStats;
import me.fireandice.ghosttracker.tracker.GhostTimer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** Builds the lines for both HUDs from the current stats. */
public final class HudContent {

    private static final DecimalFormat INT = new DecimalFormat("#,###");
    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0.##");
    private static final DecimalFormat MARGIN = new DecimalFormat("0.00");

    private static String perHour(double value, long seconds, boolean prefixes) {
        String s = seconds == 0 ? "-" : DECIMAL.format(value / seconds * 3600);
        return prefixes ? s : s + "/hr";
    }

    // ------------------------------------------------------------------ total (non-timed) tracker

    public static List<HudLine> tracker() {
        GhostConfig c = GhostConfig.INSTANCE;
        GhostStats s = GhostTrackerClient.ghostStats;
        List<HudLine> out = new ArrayList<>(11);

        if (c.line("tracker.kills"))
            out.add(HudLine.of("Kills: ", INT.format(s.kills), c.killColor, Images.KILLS));
        drop(out, c, "tracker.sorrow", "Sorrows: ", s.sorrowCount, s, GhostDrops.SORROW, Images.SORROW);
        drop(out, c, "tracker.volta", "Voltas: ", s.voltaCount, s, GhostDrops.VOLTA, Images.VOLTA);
        drop(out, c, "tracker.plasma", "Plasmas: ", s.plasmaCount, s, GhostDrops.PLASMA, Images.PLASMA);
        drop(out, c, "tracker.boots", "Ghostly Boots: ", s.bootsCount, s, GhostDrops.BOOTS, Images.BOOTS);
        drop(out, c, "tracker.coins", "1m Coins: ", s.coinsCount, s, GhostDrops.COINS, Images.COINS);
        if (c.line("tracker.mf"))
            out.add(HudLine.of("Average MF: ", s.averageMf(DECIMAL), c.mfColor, Images.MAGIC_FIND));
        if (c.line("tracker.averageXp")) {
            String avg = s.averageXp(DECIMAL);
            out.add(HudLine.of("Average XP: ", c.showPrefixes() ? avg : avg + "/kill", c.xpColor, Images.COMBAT_XP));
        }
        if (c.line("tracker.totalXp"))
            out.add(HudLine.of("Total XP: ", DECIMAL.format(s.totalXp), c.xpColor, Images.COMBAT_XP));
        if (c.line("tracker.scavenger"))
            out.add(HudLine.of("Scavenger: ", INT.format(s.scavenger), c.coinColor, Images.COINS));
        if (c.line("tracker.totalMoney"))
            out.add(HudLine.of("Total Coins: ", INT.format(s.totalValue()), c.coinColor, Images.MONEY));
        return out;
    }

    private static void drop(List<HudLine> out, GhostConfig c, String key, String prefix, int count, GhostStats s,
                             GhostDrops drop, Images.Icon icon) {
        if (!c.line(key)) return;
        String suffix = c.showMargins && count != 0 ? s.percentDifference(drop, MARGIN) : "";
        out.add(new HudLine(prefix, INT.format(count), c.dropColor, suffix, c.marginColor, icon));
    }

    // ------------------------------------------------------------------ timed session tracker

    public static List<HudLine> timer() {
        GhostConfig c = GhostConfig.INSTANCE;
        GhostStats s = GhostTimer.stats;
        long seconds = GhostTimer.elapsedTime() / 1000;
        boolean p = c.showPrefixes();
        List<HudLine> out = new ArrayList<>(12);

        if (c.line("timer.kills"))
            out.add(HudLine.of("Kills/hr: ", perHour(s.kills, seconds, p), c.killColor, Images.KILLS));
        timedDrop(out, c, "timer.sorrow", "Sorrows/hr: ", s.sorrowCount, seconds, s, GhostDrops.SORROW, Images.SORROW);
        timedDrop(out, c, "timer.volta", "Voltas/hr: ", s.voltaCount, seconds, s, GhostDrops.VOLTA, Images.VOLTA);
        timedDrop(out, c, "timer.plasma", "Plasmas/hr: ", s.plasmaCount, seconds, s, GhostDrops.PLASMA, Images.PLASMA);
        timedDrop(out, c, "timer.boots", "Ghostly Boots/hr: ", s.bootsCount, seconds, s, GhostDrops.BOOTS, Images.BOOTS);
        timedDrop(out, c, "timer.coins", "1m Coins/hr: ", s.coinsCount, seconds, s, GhostDrops.COINS, Images.COINS);
        if (c.line("timer.mf"))
            out.add(HudLine.of("Average MF: ", s.averageMf(DECIMAL), c.mfColor, Images.MAGIC_FIND));
        if (c.line("timer.averageXp")) {
            String avg = s.averageXp(DECIMAL);
            out.add(HudLine.of("Average XP: ", p ? avg : avg + "/kill", c.xpColor, Images.COMBAT_XP));
        }
        if (c.line("timer.xpRate"))
            out.add(HudLine.of("XP/hr: ", perHour(s.totalXp, seconds, p), c.xpColor, Images.COMBAT_XP));
        if (c.line("timer.scavenger"))
            out.add(HudLine.of("Scavenger/hr: ", perHour(s.scavenger, seconds, p), c.coinColor, Images.COINS));
        if (c.line("timer.moneyRate"))
            out.add(HudLine.of("Coins/hr: ", perHour(s.totalValue(), seconds, p), c.coinColor, Images.MONEY));
        if (c.line("timer.time")) {
            long h = seconds / 3600, m = (seconds % 3600) / 60, sec = seconds % 60;
            StringBuilder sb = new StringBuilder();
            if (h > 0) sb.append(h).append("h ");
            if (m > 0 || h > 0) sb.append(m).append("m "); // 1h 0m should still show minutes
            sb.append(sec).append("s");
            out.add(new HudLine("Time: ", sb.toString(), c.timeColor,
                GhostTimer.isPaused() ? " (Paused)" : "", c.pauseColor, Images.TIME));
        }
        return out;
    }

    private static void timedDrop(List<HudLine> out, GhostConfig c, String key, String prefix, int count, long seconds,
                                  GhostStats s, GhostDrops drop, Images.Icon icon) {
        if (!c.line(key)) return;
        String suffix = c.showMargins && count != 0 ? s.percentDifference(drop, MARGIN) : "";
        out.add(new HudLine(prefix, perHour(count, seconds, c.showPrefixes()), c.dropColor, suffix, c.marginColor, icon));
    }

    private HudContent() {
    }
}
