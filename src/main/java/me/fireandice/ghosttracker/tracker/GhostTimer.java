package me.fireandice.ghosttracker.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.config.GhostConfig;
import me.fireandice.ghosttracker.utils.Chat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GhostTimer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static long startTime = -1L; // time of the last start/unpause
    private static long totalTime = 0L;  // elapsed time up to the last pause

    public static boolean isTracking = false;
    public static final GhostStats stats = new GhostStats();
    private static Path file;

    public static long elapsedTime() {
        return totalTime + (isTracking ? System.currentTimeMillis() - startTime : 0);
    }

    public static boolean isPaused() {
        return !isTracking && totalTime != 0L;
    }

    public static void start(boolean message) {
        if (isTracking) {
            if (message) Chat.send("§aSession timer is already running");
            return;
        }
        startTime = System.currentTimeMillis();
        isTracking = true;
        if (message) Chat.send("§aSession timer started");
    }

    public static void pause(boolean message) {
        if (!isTracking) {
            if (message) Chat.send("§eSession timer is already inactive");
            return;
        }
        totalTime += System.currentTimeMillis() - startTime;
        isTracking = false;
        if (message) Chat.send("§eSession timer paused");
    }

    private static long lastKill = -1L;

    /** Called when a ghost kill is tracked: starts the timer if auto mode is on. */
    public static void onKill() {
        if (!GhostConfig.INSTANCE.autoTimer) return;
        lastKill = System.currentTimeMillis();
        if (!isTracking) start(false);
    }

    /** Called every tick: pauses 15s after the last kill, not counting the idle time. */
    public static void autoTick() {
        if (!GhostConfig.INSTANCE.autoTimer || !isTracking) return;
        long ref = Math.max(lastKill, startTime);
        if (System.currentTimeMillis() - ref > 15_000) {
            totalTime += Math.max(0, ref - startTime);
            isTracking = false;
        }
    }

    public static void reset(boolean message) {
        isTracking = false;
        stats.reset();
        startTime = -1L;
        totalTime = 0L;
        if (message) Chat.send("§cSession timer reset");
    }

    public static void init(Path dir) {
        file = dir.resolve("GhostTimer.json");
        if (Files.exists(file)) load();
        else save();
    }

    public static synchronized void save() {
        if (file == null) return;
        JsonObject o = stats.toJson();
        o.addProperty("time", elapsedTime());
        try {
            Files.writeString(file, GSON.toJson(o));
        } catch (IOException e) {
            GhostTrackerClient.LOGGER.error("Couldn't save timer stats", e);
        }
    }

    private static void load() {
        pause(false);
        try {
            JsonObject o = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (o == null) return;
            stats.fromJson(o);
            if (o.has("time")) totalTime = o.get("time").getAsLong();
            GhostTrackerClient.LOGGER.info("Timer stats loaded");
        } catch (Exception e) {
            GhostTrackerClient.LOGGER.error("Couldn't read timer stats file", e);
        }
    }
}
