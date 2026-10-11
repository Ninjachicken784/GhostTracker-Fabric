package me.fireandice.ghosttracker.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.config.GhostConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Fetches bazaar prices from the Coflnet API. Requests run asynchronously (the 1.8.9 version blocked the client thread). */
public final class PriceData {

    private static final String USER_AGENT = "GhostTracker/" + GhostTrackerClient.VERSION;
    // example: https://sky.coflnet.com/api/bazaar/SORROW/history/day
    private static final String BASE_URL = "https://sky.coflnet.com/api/bazaar";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String PRICE_KEY = "buy";

    private static final float SORROW_NPC = 13f;
    private static final float VOLTA_NPC = 1_111f;
    private static final float PLASMA_NPC = 20_000f;
    public static final float BOOTS_PRICE = 77_777f;

    public static volatile float sorrowPrice = SORROW_NPC;
    public static volatile float voltaPrice = VOLTA_NPC;
    public static volatile float plasmaPrice = PLASMA_NPC;

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static long lastFetch = -1L;

    /** Called every tick while in SkyBlock. */
    public static void fetchPrices() {
        long now = System.currentTimeMillis();
        if (lastFetch != -1L && now - lastFetch <= GhostConfig.INSTANCE.priceFrequency * 60_000L) return;
        lastFetch = now;

        String span = GhostConfig.INSTANCE.priceTimespan();
        fetch("SORROW", span, SORROW_NPC, v -> sorrowPrice = v);
        fetch("VOLTA", span, VOLTA_NPC, v -> voltaPrice = v);
        fetch("PLASMA", span, PLASMA_NPC, v -> plasmaPrice = v);
    }

    private interface FloatSink {
        void accept(float v);
    }

    private static void fetch(String itemId, String span, float npcFloor, FloatSink sink) {
        String url = BASE_URL + "/" + itemId + "/history/" + span;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).whenComplete((response, error) -> {
            if (error != null || response == null || response.statusCode() != 200) {
                GhostTrackerClient.LOGGER.error("Failed to fetch {} price", itemId);
                return;
            }
            try {
                float avg = average(JsonParser.parseString(response.body()).getAsJsonArray());
                if (Float.isNaN(avg)) throw new IllegalStateException("no data");
                sink.accept(Math.max(avg, npcFloor));
                GhostTrackerClient.LOGGER.info("Fetched {} price: {}", itemId, Math.max(avg, npcFloor));
            } catch (Exception e) {
                GhostTrackerClient.LOGGER.error("Couldn't parse {} price data", itemId);
            }
        });
    }

    private static float average(JsonArray array) {
        float total = 0f;
        int count = 0;
        for (JsonElement e : array) {
            try {
                total += e.getAsJsonObject().get(PRICE_KEY).getAsFloat();
                count++;
            } catch (Exception ignored) {
            }
        }
        return count == 0 ? Float.NaN : total / count;
    }
}
