package me.fireandice.ghosttracker.hud;

import me.fireandice.ghosttracker.config.GhostConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Big centered pop-ups that fade out, shown when a drop happens. */
public final class DropPopups {

    private record Popup(String title, String sub, int color, long time) {
    }

    private static final long DURATION = 3500;
    private static final long FADE = 800;
    private static final List<Popup> ACTIVE = new ArrayList<>();

    public static void add(String title, String sub, int color) {
        if (!GhostConfig.INSTANCE.dropPopup) return;
        ACTIVE.add(new Popup(title, sub, color, System.currentTimeMillis()));
        if (ACTIVE.size() > 4) ACTIVE.remove(0);
    }

    public static void render(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(p -> now - p.time > DURATION);
        if (ACTIVE.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        int cx = g.guiWidth() / 2;
        int y = g.guiHeight() / 4;

        for (Popup p : ACTIVE) {
            long age = now - p.time;
            float alphaF = age > DURATION - FADE ? (DURATION - age) / (float) FADE : 1f;
            int alpha = Math.max(4, Math.min(255, (int) (alphaF * 255)));
            int argbColor = (alpha << 24) | p.color;
            int argbWhite = (alpha << 24) | 0xFFFFFF;

            // big title
            g.pose().pushMatrix();
            g.pose().translate((float) cx, (float) y);
            g.pose().scale(3f, 3f);
            g.text(font, p.title, -font.width(p.title) / 2, 0, argbColor, true);
            g.pose().popMatrix();

            // small subtitle under it
            g.text(font, p.sub, cx - font.width(p.sub) / 2, y + 30, argbWhite, true);
            y += 46;
        }
    }

    private DropPopups() {
    }
}
