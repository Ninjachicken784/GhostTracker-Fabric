package me.fireandice.ghosttracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.fireandice.ghosttracker.api.PriceData;
import me.fireandice.ghosttracker.command.GhostCommand;
import me.fireandice.ghosttracker.config.GhostConfig;
import me.fireandice.ghosttracker.hud.HudRenderer;
import me.fireandice.ghosttracker.tracker.GhostListener;
import me.fireandice.ghosttracker.tracker.GhostStats;
import me.fireandice.ghosttracker.tracker.GhostTimer;
import me.fireandice.ghosttracker.tracker.PurseListener;
import me.fireandice.ghosttracker.ui.GhostConfigScreen;
import me.fireandice.ghosttracker.utils.Chat;
import me.fireandice.ghosttracker.utils.ScoreboardUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GhostTrackerClient implements ClientModInitializer {

    public static final String MOD_ID = "ghosttracker";
    public static final Logger LOGGER = LoggerFactory.getLogger("GhostTracker");
    public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
        .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final GhostStats ghostStats = new GhostStats();
    private static Path statsFile;

    private static boolean openConfigNextTick = false;
    private static boolean openEditorNextTick = false;
    private static Object lastLevel = null;
    private static long lastSave = -1L;

    @Override
    public void onInitializeClient() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("GhostTracker");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error("Couldn't create config directory", e);
        }
        statsFile = dir.resolve("GhostStats.json");

        GhostConfig.init(dir);
        loadStats();
        GhostTimer.init(dir);

        ClientTickEvents.START_CLIENT_TICK.register(GhostTrackerClient::onTick);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String text = message.getString();
            if (overlay) GhostListener.onActionBar(text);
            else GhostListener.onChat(text);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> GhostTimer.pause(false));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> GhostCommand.register(dispatcher));

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudRenderer::render);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveStats();
            GhostTimer.save();
        }));
    }

    private static void onTick(Minecraft mc) {
        if (openConfigNextTick) {
            openConfigNextTick = false;
            mc.setScreen(new GhostConfigScreen(mc.screen));
        }

        if (openEditorNextTick) {
            openEditorNextTick = false;
            mc.setScreen(new me.fireandice.ghosttracker.ui.HudEditorScreen(mc.screen));
        }

        // the 1.8.9 version paused on WorldEvent.Unload; this is the equivalent (new level object = world change)
        if (mc.level != lastLevel) {
            if (lastLevel != null) GhostTimer.pause(false);
            lastLevel = mc.level;
        }

        ScoreboardUtils.checkLocations();
        PurseListener.onTick();
        me.fireandice.ghosttracker.tracker.WeaponKills.onTick();
        GhostTimer.autoTick();

        long now = System.currentTimeMillis();
        if (lastSave == -1L || now - lastSave > 300_000) {
            saveStats();
            GhostTimer.save();
            lastSave = now;
        }

        if (ScoreboardUtils.inSkyblock) PriceData.fetchPrices();
    }

    /** Commands run while chat is open, so the screen has to be opened on the next tick. */
    public static void openConfig() {
        openConfigNextTick = true;
    }

    public static void openHudEditor() {
        openEditorNextTick = true;
    }

    public static void resetStats(boolean message) {
        ghostStats.reset();
        if (message) Chat.send("§cMain tracker reset");
    }

    public static synchronized void saveStats() {
        if (statsFile == null) return;
        try {
            Files.writeString(statsFile, GSON.toJson(ghostStats.toJson()));
        } catch (IOException e) {
            LOGGER.error("Couldn't save tracker stats", e);
        }
    }

    private static void loadStats() {
        if (!Files.exists(statsFile)) {
            saveStats();
            return;
        }
        try {
            JsonObject o = GSON.fromJson(Files.readString(statsFile), JsonObject.class);
            if (o != null) ghostStats.fromJson(o);
            LOGGER.info("Tracker stats loaded");
        } catch (Exception e) {
            LOGGER.error("Couldn't read tracker stats", e);
        }
    }
}
