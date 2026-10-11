package me.fireandice.ghosttracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.fireandice.ghosttracker.GhostTrackerClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain JSON config (replaces the OneConfig screen of the 1.8.9 version). Edit via /ghost, the config screen
 * (/ghost config) or the file itself.
 */
public final class GhostConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;

    public static GhostConfig INSTANCE = new GhostConfig();

    // ---- General ----
    public boolean showEverywhere = false;
    public boolean shadow = true;
    public boolean showIcons = true;
    /** Reduces text on each line (hides prefixes, adds "/hr" etc.) */
    public boolean abbreviate = false;
    public boolean showMargins = true;
    public int lootingLevel = 5;
    public int luckLevel = 7;
    /** 0 = hour, 1 = day, 2 = week */
    public int priceTimespanOption = 2;
    public int priceFrequency = 20;

    // ---- Colors (RGB) ----
    public int killColor = 0x55FFFF;
    public int dropColor = 0x5555FF;
    public int marginColor = 0x555555;
    public int mfColor = 0xFFAA00;
    public int xpColor = 0xFF5555;
    public int timeColor = 0x55FFFF;
    public int coinColor = 0xFFAA00;
    public int pauseColor = 0x555555;

    // ---- HUD placement ----
    public boolean trackerEnabled = true;
    public int trackerX = 5;
    public int trackerY = 5;
    public float trackerScale = 1.0f;

    public boolean timerEnabled = true;
    public int timerX = 5;
    public int timerY = 120;
    public float timerScale = 1.0f;

    // ---- Per-line toggles, keys are "tracker.<line>" / "timer.<line>" ----
    public Map<String, Boolean> lines = new LinkedHashMap<>();

    public static final String[] TRACKER_LINES = {
        "kills", "sorrow", "volta", "plasma", "boots", "coins", "mf", "averageXp", "totalXp", "scavenger", "totalMoney"
    };
    public static final String[] TIMER_LINES = {
        "kills", "sorrow", "volta", "plasma", "boots", "coins", "mf", "averageXp", "xpRate", "scavenger", "moneyRate", "time"
    };

    public boolean line(String key) {
        return lines.getOrDefault(key, true);
    }

    public void setLine(String key, boolean value) {
        lines.put(key, value);
    }

    public String priceTimespan() {
        return switch (priceTimespanOption) {
            case 0 -> "hour";
            case 1 -> "day";
            default -> "week";
        };
    }

    public boolean showPrefixes() {
        return !abbreviate;
    }

    /** Returns a mutable color field by name, or null. Used by /ghost color. */
    public boolean setColor(String name, int rgb) {
        switch (name.toLowerCase()) {
            case "kill" -> killColor = rgb;
            case "drop" -> dropColor = rgb;
            case "margin" -> marginColor = rgb;
            case "mf" -> mfColor = rgb;
            case "xp" -> xpColor = rgb;
            case "time" -> timeColor = rgb;
            case "coin" -> coinColor = rgb;
            case "pause" -> pauseColor = rgb;
            default -> {
                return false;
            }
        }
        return true;
    }

    public static final String[] COLOR_NAMES = {"kill", "drop", "margin", "mf", "xp", "time", "coin", "pause"};

    private void sanitize() {
        lootingLevel = Math.max(0, Math.min(10, lootingLevel));
        luckLevel = Math.max(0, Math.min(10, luckLevel));
        priceFrequency = Math.max(5, Math.min(120, priceFrequency));
        priceTimespanOption = Math.max(0, Math.min(2, priceTimespanOption));
        trackerScale = Math.max(0.25f, Math.min(5f, trackerScale));
        timerScale = Math.max(0.25f, Math.min(5f, timerScale));
        if (lines == null) lines = new LinkedHashMap<>();
        for (String l : TRACKER_LINES) lines.putIfAbsent("tracker." + l, true);
        for (String l : TIMER_LINES) lines.putIfAbsent("timer." + l, true);
    }

    public static void init(Path dir) {
        file = dir.resolve("GhostConfig.json");
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                GhostConfig loaded = GSON.fromJson(r, GhostConfig.class);
                if (loaded != null) INSTANCE = loaded;
            } catch (Exception e) {
                GhostTrackerClient.LOGGER.error("Couldn't read config, using defaults", e);
            }
        }
        INSTANCE.sanitize();
        save();
    }

    public static void save() {
        if (file == null) return;
        try (Writer w = Files.newBufferedWriter(file)) {
            GSON.toJson(INSTANCE, w);
        } catch (IOException e) {
            GhostTrackerClient.LOGGER.error("Couldn't save config", e);
        }
    }
}
