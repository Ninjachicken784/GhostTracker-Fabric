package me.fireandice.ghosttracker.hud;

/**
 * One displayed row. {@code suffix} may be empty. Colors are RGB.
 */
public record HudLine(String prefix, String main, int mainColor, String suffix, int suffixColor, Images.Icon icon) {

    public static HudLine of(String prefix, String main, int color, Images.Icon icon) {
        return new HudLine(prefix, main, color, "", 0, icon);
    }
}
