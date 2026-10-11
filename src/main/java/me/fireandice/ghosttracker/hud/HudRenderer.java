package me.fireandice.ghosttracker.hud;

import me.fireandice.ghosttracker.config.GhostConfig;
import me.fireandice.ghosttracker.utils.ScoreboardUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.List;

/**
 * All drawing goes through this class. If a Minecraft update renames a GUI method, this is the only file to touch.
 */
public final class HudRenderer {

    /** Registered as a Fabric HudElement (see GhostTrackerClient). */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        GhostConfig c = GhostConfig.INSTANCE;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!(c.showEverywhere || ScoreboardUtils.inDwarvenMines)) return;

        if (c.trackerEnabled) draw(graphics, HudContent.tracker(), c.trackerX, c.trackerY, c.trackerScale);
        if (c.timerEnabled) draw(graphics, HudContent.timer(), c.timerX, c.timerY, c.timerScale);
    }

    private static void draw(GuiGraphicsExtractor g, List<HudLine> lines, int x, int y, float scale) {
        if (lines.isEmpty()) return;
        GhostConfig c = GhostConfig.INSTANCE;
        Font font = Minecraft.getInstance().font;

        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);

        int lineY = 0;
        for (HudLine line : lines) {
            int cx = 0;
            if (c.showIcons) {
                Images.Icon icon = line.icon();
                // scales the whole texture down to 8x8
                g.blit(RenderPipelines.GUI_TEXTURED, icon.id(), cx, lineY, 0f, 0f, 8, 8,
                    icon.width(), icon.height(), icon.width(), icon.height());
                cx += 10;
            }
            if (c.showPrefixes()) {
                g.text(font, line.prefix(), cx, lineY, 0xFF000000 | line.mainColor(), c.shadow);
                cx += font.width(line.prefix());
            }
            g.text(font, line.main(), cx, lineY, 0xFF000000 | line.mainColor(), c.shadow);
            cx += font.width(line.main());
            if (!line.suffix().isEmpty()) {
                g.text(font, line.suffix(), cx, lineY, 0xFF000000 | line.suffixColor(), c.shadow);
            }
            lineY += font.lineHeight;
        }
        g.pose().popMatrix();
    }

    private HudRenderer() {
    }
}
