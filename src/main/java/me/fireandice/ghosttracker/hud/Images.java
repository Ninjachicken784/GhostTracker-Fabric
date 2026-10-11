package me.fireandice.ghosttracker.hud;

import net.minecraft.resources.Identifier;

/** Icon textures with their pixel sizes (they're drawn scaled down to 8x8). */
public final class Images {

    public record Icon(Identifier id, int width, int height) {
        static Icon of(String name, int w, int h) {
            return new Icon(Identifier.fromNamespaceAndPath("ghosttracker", "textures/gui/" + name + ".png"), w, h);
        }
    }

    public static final Icon KILLS = Icon.of("kills", 160, 160);
    public static final Icon SORROW = Icon.of("sorrow", 16, 16);
    public static final Icon VOLTA = Icon.of("volta", 185, 185);
    public static final Icon PLASMA = Icon.of("plasma", 185, 185);
    public static final Icon BOOTS = Icon.of("ghostly_boots", 160, 160);
    public static final Icon COINS = Icon.of("coin_drop", 185, 185);
    public static final Icon MAGIC_FIND = Icon.of("magic_find", 160, 160);
    public static final Icon COMBAT_XP = Icon.of("combat", 16, 16);
    public static final Icon TIME = Icon.of("time", 16, 16);
    public static final Icon MONEY = Icon.of("money", 16, 16);

    private Images() {
    }
}
