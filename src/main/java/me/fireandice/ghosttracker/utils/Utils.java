package me.fireandice.ghosttracker.utils;

import net.minecraft.client.Minecraft;

public final class Utils {

    public static boolean inGhostArea() {
        Minecraft mc = Minecraft.getInstance();
        return ScoreboardUtils.inDwarvenMines && mc.player != null && mc.player.getY() <= 100;
    }

    private Utils() {
    }
}
