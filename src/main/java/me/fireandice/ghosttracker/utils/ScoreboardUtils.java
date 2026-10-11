package me.fireandice.ghosttracker.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Modified from https://github.com/inglettronald/DulkirMod under the GNU Affero General Public License v3.0
 */
public final class ScoreboardUtils {

    /** Only updates after {@link #checkLocations()} is called */
    public static boolean inSkyblock = false;
    /** Only updates after {@link #checkLocations()} is called */
    public static boolean inDwarvenMines = false;

    private static final String[] LOCATIONS = {
        "Dwarven Village", "Miner's Guild", "Palace Bridge", "Royal Palace", "Puzzler", "Grand Library",
        "Barracks of Heroes", "Royal Mines", "Cliffside Veins", "Forge Basin", "The Forge", "Rampart's Quarry",
        "Far Reserve", "Upper Mines", "Goblin Burrows", "Great Ice Wall", "Aristocrat Passage", "Hanging Court",
        "Divan's Gateway", "Lava Springs", "The Mist", "Dwarven Mines", "Gates to the Mines", "The Lift"
    };

    /** Sidebar lines from top to bottom, with team prefixes/suffixes applied and formatting codes removed. */
    private static List<String> getLines() {
        Minecraft mc = Minecraft.getInstance();
        List<String> lines = new ArrayList<>();
        if (mc.player == null || mc.level == null) return lines;

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return lines;

        List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(objective));
        entries.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed());

        for (PlayerScoreEntry entry : entries) {
            if (entry.isHidden()) continue;
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            lines.add(name.getString());
        }
        return lines;
    }

    private static boolean computeInSkyblock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isSingleplayer()) return false;

        var server = mc.getCurrentServer();
        if (server == null || !server.ip.toLowerCase().contains("hypixel")) return false;

        Objective objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return false;
        return Chat.stripControlCodes(objective.getDisplayName().getString()).toLowerCase().contains("skyblock");
    }

    /** Called every tick */
    public static void checkLocations() {
        if (!computeInSkyblock()) {
            inSkyblock = false;
            inDwarvenMines = false;
            return;
        }
        inSkyblock = true;

        List<String> lines = getLines();
        // The location line sits near the top of the sidebar; look at the first few lines so small layout
        // changes on Hypixel's side don't break detection
        int limit = Math.min(lines.size(), 8);
        for (int i = 0; i < limit; i++) {
            String line = Chat.stripControlCodes(lines.get(i));
            for (String loc : LOCATIONS) {
                if (line.contains(loc)) {
                    inDwarvenMines = true;
                    return;
                }
            }
        }
        inDwarvenMines = false;
    }

    /**
     * Returns the purse line with everything but digits and spaces removed, e.g. "1234567 5" if the scavenger gain is
     * shown, or null.
     */
    public static String getPurse() {
        if (!inSkyblock) return null;
        for (String line : getLines()) {
            if (line.contains("Purse") || line.contains("Piggy")) {
                StringBuilder sb = new StringBuilder();
                for (char c : Chat.stripControlCodes(line).toCharArray()) {
                    if (Character.isDigit(c) || c == ' ') sb.append(c);
                }
                return sb.toString().trim();
            }
        }
        return null;
    }

    private ScoreboardUtils() {
    }
}
