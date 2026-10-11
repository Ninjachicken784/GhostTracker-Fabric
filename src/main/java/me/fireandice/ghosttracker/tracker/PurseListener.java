package me.fireandice.ghosttracker.tracker;

import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.utils.ScoreboardUtils;
import me.fireandice.ghosttracker.utils.Utils;

public final class PurseListener {

    private static Long previousPurse = null;

    /** Called every client tick */
    public static void onTick() {
        String purseString = ScoreboardUtils.getPurse();
        if (purseString == null) return;

        String[] split = purseString.split(" +");

        long purse;
        try {
            purse = Long.parseLong(split[0]);
        } catch (NumberFormatException e) {
            GhostTrackerClient.LOGGER.error("Couldn't parse purse string as number");
            return;
        }

        long scavenger;
        if (split.length > 1) {
            try {
                scavenger = Long.parseLong(split[1]);
            } catch (NumberFormatException e) {
                GhostTrackerClient.LOGGER.error("Couldn't parse scavenger string as number");
                return;
            }
        } else {
            return;
        }

        if (previousPurse == null) { // first tick after joining skyblock
            previousPurse = purse;
            return;
        }
        if (!Utils.inGhostArea()) { // avoids tracking non ghost kills but still saves the purse value
            previousPurse = purse;
            return;
        }

        long purseGained = purse - previousPurse;
        // more than 15 kills worth or less than 6 coins (talisman of coins or a coins loss or smth)
        if (purseGained < 6 || purseGained > 15 * scavenger) return;

        GhostTrackerClient.ghostStats.scavenger += purseGained;
        if (GhostTimer.isTracking) GhostTimer.stats.scavenger += purseGained;

        previousPurse = purse;
    }

    private PurseListener() {
    }
}
