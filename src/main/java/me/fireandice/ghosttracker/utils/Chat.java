package me.fireandice.ghosttracker.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Chat {

    public static final String PREFIX = "§b§lGhostTracker§r§8 »§r ";

    /** Sends a client-side-only chat message (legacy § codes allowed). */
    public static void send(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.gui.getChat().addClientSystemMessage(Component.literal(PREFIX + message));
    }

    public static String stripControlCodes(String s) {
        return s.replaceAll("(?i)§[0-9A-FK-OR]", "");
    }
}
