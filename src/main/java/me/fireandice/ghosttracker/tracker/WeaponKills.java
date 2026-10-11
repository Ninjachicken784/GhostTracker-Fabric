package me.fireandice.ghosttracker.tracker;

import me.fireandice.ghosttracker.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Counts kills from the held weapon's Book of Stats counter (SkyBlock item data "stats_book").
 * While a weapon with a counter is held, this is the kill source and the action bar no longer adds kills.
 */
public final class WeaponKills {

    /** True while a weapon with a kill counter was held within the last 5 seconds. */
    public static boolean active = false;

    private static String lastKey = null;
    private static int lastKills = -1;
    private static long lastSeen = 0;

    /** Called every client tick */
    public static void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        long now = System.currentTimeMillis();

        int kills = -1;
        String key = "";
        ItemStack stack = mc.player.getMainHandItem();
        if (!stack.isEmpty()) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                CompoundTag tag = data.copyTag();
                kills = tag.getIntOr("stats_book", -1);
                key = tag.getStringOr("uuid", "") + "|" + stack.getHoverName().getString();
            }
        }

        if (kills < 0) {
            if (now - lastSeen > 5000) active = false;
            return;
        }
        active = true;
        lastSeen = now;

        if (!key.equals(lastKey)) { // different weapon: just remember its counter
            lastKey = key;
            lastKills = kills;
            return;
        }
        if (kills > lastKills) {
            int gained = kills - lastKills;
            lastKills = kills;
            // ignore huge jumps (e.g. counter changes from other sources) and kills outside the ghost area
            if (gained <= 15 && Utils.inGhostArea()) GhostListener.onWeaponKills(gained);
        } else {
            lastKills = kills;
        }
    }

    private WeaponKills() {
    }
}
