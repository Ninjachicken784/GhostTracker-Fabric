package me.fireandice.ghosttracker.ui;

import me.fireandice.ghosttracker.config.GhostConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Simple button-based settings screen with three tabs. Colors and HUD placement are set with /ghost commands. */
public class GhostConfigScreen extends Screen {

    private static final String[] TABS = {"General", "Tracker", "Timer"};
    private static final String[] SPANS = {"Hour", "Day", "Week"};
    private static final String[] LINE_LABELS_TRACKER = {
        "Kills", "Sorrows", "Voltas", "Plasmas", "Ghostly Boots", "1m Coins", "Average MF", "Average XP",
        "Total XP", "Scavenger", "Total Coins"
    };
    private static final String[] LINE_LABELS_TIMER = {
        "Kills/hr", "Sorrows/hr", "Voltas/hr", "Plasmas/hr", "Ghostly Boots/hr", "1m Coins/hr", "Average MF",
        "Average XP", "XP/hr", "Scavenger/hr", "Coins/hr", "Time"
    };

    private final Screen parent;
    private int tab = 0;
    private int slot = 0;

    public GhostConfigScreen(Screen parent) {
        super(Component.literal("GhostTracker"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        slot = 0;
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            Button b = Button.builder(Component.literal((i == tab ? "§e" : "") + TABS[i]), btn -> {
                    tab = index;
                    rebuildWidgets();
                })
                .bounds(this.width / 2 - 155 + i * 105, 22, 100, 20).build();
            addRenderableWidget(b);
        }

        GhostConfig c = GhostConfig.INSTANCE;
        switch (tab) {
            case 0 -> {
                toggle("Show everywhere", () -> c.showEverywhere, v -> c.showEverywhere = v);
                toggle("Text shadow", () -> c.shadow, v -> c.shadow = v);
                toggle("Show icons", () -> c.showIcons, v -> c.showIcons = v);
                toggle("Abbreviate text", () -> c.abbreviate, v -> c.abbreviate = v);
                toggle("Show margins", () -> c.showMargins, v -> c.showMargins = v);
                cycle("Looting level", () -> c.lootingLevel, 0, 10, 1, v -> c.lootingLevel = v, null);
                cycle("Luck level", () -> c.luckLevel, 0, 10, 1, v -> c.luckLevel = v, null);
                cycle("Price timespan", () -> c.priceTimespanOption, 0, 2, 1, v -> c.priceTimespanOption = v, SPANS);
                cycle("Price fetch (min)", () -> c.priceFrequency, 5, 120, 5, v -> c.priceFrequency = v, null);
                addRenderableWidget(Button.builder(Component.literal("§bMove HUDs..."),
                        btn -> this.minecraft.setScreen(new HudEditorScreen(this)))
                    .bounds(nextX(), nextY(), 150, 20).build());
                slot++;
            }
            case 1 -> {
                toggle("Tracker HUD", () -> c.trackerEnabled, v -> c.trackerEnabled = v);
                for (int i = 0; i < GhostConfig.TRACKER_LINES.length; i++) {
                    String key = "tracker." + GhostConfig.TRACKER_LINES[i];
                    toggle(LINE_LABELS_TRACKER[i], () -> c.line(key), v -> c.setLine(key, v));
                }
            }
            default -> {
                toggle("Timer HUD", () -> c.timerEnabled, v -> c.timerEnabled = v);
                for (int i = 0; i < GhostConfig.TIMER_LINES.length; i++) {
                    String key = "timer." + GhostConfig.TIMER_LINES[i];
                    toggle(LINE_LABELS_TIMER[i], () -> c.line(key), v -> c.setLine(key, v));
                }
            }
        }

        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
            .bounds(this.width / 2 - 100, this.height - 28, 200, 20).build());
    }

    private int nextX() {
        return this.width / 2 - 155 + (slot % 2) * 160;
    }

    private int nextY() {
        return 50 + (slot / 2) * 24;
    }

    private void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        Button b = Button.builder(toggleText(label, get.getAsBoolean()), btn -> {
                boolean now = !get.getAsBoolean();
                set.accept(now);
                btn.setMessage(toggleText(label, now));
            })
            .bounds(nextX(), nextY(), 150, 20).build();
        slot++;
        addRenderableWidget(b);
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "§aON" : "§cOFF"));
    }

    /** Click to increase by step, wrapping back to min after max. */
    private void cycle(String label, IntSupplier get, int min, int max, int step, Consumer<Integer> set, String[] names) {
        Button b = Button.builder(cycleText(label, get.getAsInt(), names), btn -> {
                int next = get.getAsInt() + step;
                if (next > max) next = min;
                set.accept(next);
                btn.setMessage(cycleText(label, next, names));
            })
            .bounds(nextX(), nextY(), 150, 20).build();
        slot++;
        addRenderableWidget(b);
    }

    private static Component cycleText(String label, int value, String[] names) {
        return Component.literal(label + ": §e" + (names != null ? names[value] : String.valueOf(value)));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 8, 0xFFFFFFFF, true);
        String hint = "HUD position & colors: /ghost help";
        graphics.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2, this.height - 42, 0xFF999999, true);
    }

    @Override
    public void onClose() {
        GhostConfig.save();
        this.minecraft.setScreen(parent);
    }
}
