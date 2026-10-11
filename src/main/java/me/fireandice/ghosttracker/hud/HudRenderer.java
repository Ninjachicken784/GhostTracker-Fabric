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

    /** True while the HUD editor screen is open (it draws the HUDs itself). */
    public static boolean editing = false;

    /** Registered as a Fabric HudElement (see GhostTrackerClient). */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        GhostConfig c = GhostConfig.INSTANCE;
        Minecraft mc = Minecraft.getInstance();
        if (editing || mc.player == null || mc.options.hideGui) return;
        DropPopups.render(graphics);
        if (!(c.showEverywhere || ScoreboardUtils.inDwarvenMines)) return;

        if (c.trackerEnabled) draw(graphics, HudContent.tracker(), c.trackerX, c.trackerY, c.trackerScale);
        if (c.timerEnabled) draw(graphics, HudContent.timer(), c.timerX, c.timerY, c.timerScale);
    }

    public static void draw(GuiGraphicsExtractor g, List<HudLine> lines, int x, int y, float scale) {
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

    /** Pixel size of a HUD on screen: {width, height}. */
    public static int[] size(List<HudLine> lines, float scale) {
        GhostConfig c = GhostConfig.INSTANCE;
        Font font = Minecraft.getInstance().font;
        int w = 0;
        for (HudLine l : lines) {
            int lw = (c.showIcons ? 10 : 0) + (c.showPrefixes() ? font.width(l.prefix()) : 0)
                + font.width(l.main()) + font.width(l.suffix());
            w = Math.max(w, lw);
        }
        return new int[]{Math.max(20, Math.round(w * scale)), Math.max(10, Math.round(lines.size() * font.lineHeight * scale))};
    }

    private HudRenderer() {
    }
}
