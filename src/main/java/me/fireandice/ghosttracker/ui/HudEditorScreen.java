package me.fireandice.ghosttracker.ui;

import me.fireandice.ghosttracker.config.GhostConfig;
import me.fireandice.ghosttracker.hud.HudContent;
import me.fireandice.ghosttracker.hud.HudLine;
import me.fireandice.ghosttracker.hud.HudRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Drag a HUD to move it, scroll over it to resize it, Esc to finish. */
public class HudEditorScreen extends Screen {

    private final Screen parent;
    private int dragging = -1; // 0 = tracker, 1 = timer
    private int offX, offY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("Move HUDs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HudRenderer.editing = true;
    }

    private List<HudLine> lines(int which) {
        return which == 0 ? HudContent.tracker() : HudContent.timer();
    }

    private boolean enabled(int which) {
        return which == 0 ? GhostConfig.INSTANCE.trackerEnabled : GhostConfig.INSTANCE.timerEnabled;
    }

    private int x(int w) {
        return w == 0 ? GhostConfig.INSTANCE.trackerX : GhostConfig.INSTANCE.timerX;
    }

    private int y(int w) {
        return w == 0 ? GhostConfig.INSTANCE.trackerY : GhostConfig.INSTANCE.timerY;
    }

    private float scale(int w) {
        return w == 0 ? GhostConfig.INSTANCE.trackerScale : GhostConfig.INSTANCE.timerScale;
    }

    private void setPos(int w, int x, int y) {
        GhostConfig c = GhostConfig.INSTANCE;
        if (w == 0) {
            c.trackerX = x;
            c.trackerY = y;
        } else {
            c.timerX = x;
            c.timerY = y;
        }
    }

    private int hit(double mx, double my) {
        for (int w = 1; w >= 0; w--) { // timer is drawn last, so check it first
            if (!enabled(w)) continue;
            int[] size = HudRenderer.size(lines(w), scale(w));
            if (mx >= x(w) && mx <= x(w) + size[0] && my >= y(w) && my <= y(w) + size[1]) return w;
        }
        return -1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int hovered = dragging >= 0 ? dragging : hit(mouseX, mouseY);
        for (int w = 0; w < 2; w++) {
            if (!enabled(w)) continue;
            List<HudLine> l = lines(w);
            int[] size = HudRenderer.size(l, scale(w));
            graphics.fill(x(w) - 2, y(w) - 2, x(w) + size[0] + 2, y(w) + size[1] + 2,
                w == hovered ? 0x6088CCFF : 0x30FFFFFF);
            HudRenderer.draw(graphics, l, x(w), y(w), scale(w));
        }
        String hint = "Drag a HUD to move it  |  Scroll over it to resize  |  Esc to finish";
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, this.height - 20, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int w = hit(event.x(), event.y());
            if (w >= 0) {
                dragging = w;
                offX = (int) event.x() - x(w);
                offY = (int) event.y() - y(w);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging >= 0) {
            int[] size = HudRenderer.size(lines(dragging), scale(dragging));
            int nx = Math.max(0, Math.min(this.width - size[0], (int) event.x() - offX));
            int ny = Math.max(0, Math.min(this.height - size[1], (int) event.y() - offY));
            setPos(dragging, nx, ny);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging >= 0) {
            dragging = -1;
            GhostConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int w = hit(mouseX, mouseY);
        if (w >= 0) {
            GhostConfig c = GhostConfig.INSTANCE;
            float next = Math.max(0.5f, Math.min(4f, Math.round((scale(w) + (scrollY > 0 ? 0.1f : -0.1f)) * 10f) / 10f));
            if (w == 0) c.trackerScale = next;
            else c.timerScale = next;
            GhostConfig.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        HudRenderer.editing = false;
        GhostConfig.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        HudRenderer.editing = false;
        super.removed();
    }
}
