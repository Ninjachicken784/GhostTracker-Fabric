package me.fireandice.ghosttracker.tracker;

public enum GhostDrops {
    SORROW(0.0012f), VOLTA(0.002f), PLASMA(0.001f), BOOTS(0.0001f), COINS(0.0001f);

    public final float baseChance;

    GhostDrops(float baseChance) {
        this.baseChance = baseChance;
    }

    /** 'COINS' has no formal name so it's not returned here. */
    public static GhostDrops byName(String name) {
        return switch (name) {
            case "Sorrow" -> SORROW;
            case "Volta" -> VOLTA;
            case "Plasma" -> PLASMA;
            case "Ghostly Boots" -> BOOTS;
            default -> null;
        };
    }
}
