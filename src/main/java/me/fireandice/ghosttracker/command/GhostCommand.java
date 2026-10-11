package me.fireandice.ghosttracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.fireandice.ghosttracker.GhostTrackerClient;
import me.fireandice.ghosttracker.config.GhostConfig;
import me.fireandice.ghosttracker.tracker.GhostTimer;
import me.fireandice.ghosttracker.utils.Chat;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public final class GhostCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("ghost")
            .executes(ctx -> {
                GhostTrackerClient.openConfig();
                return 1;
            });

        root.then(ClientCommands.literal("config").executes(ctx -> {
            GhostTrackerClient.openConfig();
            return 1;
        }));
        root.then(ClientCommands.literal("start").executes(ctx -> {
            GhostTimer.start(true);
            return 1;
        }));
        root.then(ClientCommands.literal("pause").executes(ctx -> {
            GhostTimer.pause(true);
            return 1;
        }));
        root.then(ClientCommands.literal("stop").executes(ctx -> {
            GhostTimer.pause(true);
            return 1;
        }));
        root.then(ClientCommands.literal("reset").executes(ctx -> {
            GhostTimer.reset(true);
            return 1;
        }));
        root.then(ClientCommands.literal("clear").executes(ctx -> {
            GhostTimer.reset(true);
            return 1;
        }));
        root.then(ClientCommands.literal("stats")
            .then(ClientCommands.literal("reset").executes(ctx -> {
                GhostTrackerClient.resetStats(true);
                return 1;
            }))
            .then(ClientCommands.literal("clear").executes(ctx -> {
                GhostTrackerClient.resetStats(true);
                return 1;
            })));

        // /ghost hud <tracker|timer> <toggle | pos <x> <y> | scale <value>>
        for (String which : new String[]{"tracker", "timer"}) {
            root.then(ClientCommands.literal("hud").then(ClientCommands.literal(which)
                .then(ClientCommands.literal("toggle").executes(ctx -> {
                    GhostConfig c = GhostConfig.INSTANCE;
                    boolean now = which.equals("tracker") ? (c.trackerEnabled = !c.trackerEnabled)
                        : (c.timerEnabled = !c.timerEnabled);
                    GhostConfig.save();
                    Chat.send(which + " HUD " + (now ? "§aenabled" : "§cdisabled"));
                    return 1;
                }))
                .then(ClientCommands.literal("pos")
                    .then(ClientCommands.argument("x", IntegerArgumentType.integer(0))
                        .then(ClientCommands.argument("y", IntegerArgumentType.integer(0)).executes(ctx -> {
                            GhostConfig c = GhostConfig.INSTANCE;
                            int x = IntegerArgumentType.getInteger(ctx, "x");
                            int y = IntegerArgumentType.getInteger(ctx, "y");
                            if (which.equals("tracker")) {
                                c.trackerX = x;
                                c.trackerY = y;
                            } else {
                                c.timerX = x;
                                c.timerY = y;
                            }
                            GhostConfig.save();
                            Chat.send(which + " HUD moved to " + x + ", " + y);
                            return 1;
                        }))))
                .then(ClientCommands.literal("scale")
                    .then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.25f, 5f)).executes(ctx -> {
                        GhostConfig c = GhostConfig.INSTANCE;
                        float v = FloatArgumentType.getFloat(ctx, "value");
                        if (which.equals("tracker")) c.trackerScale = v;
                        else c.timerScale = v;
                        GhostConfig.save();
                        Chat.send(which + " HUD scale set to " + v);
                        return 1;
                    })))));
        }

        // /ghost color <name> <rrggbb>
        root.then(ClientCommands.literal("color")
            .then(ClientCommands.argument("name", StringArgumentType.word())
                .then(ClientCommands.argument("hex", StringArgumentType.word()).executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "name");
                    String hex = StringArgumentType.getString(ctx, "hex").replace("#", "");
                    try {
                        int rgb = Integer.parseInt(hex, 16) & 0xFFFFFF;
                        if (!GhostConfig.INSTANCE.setColor(name, rgb)) {
                            Chat.send("§cUnknown color. Options: " + String.join(", ", GhostConfig.COLOR_NAMES));
                            return 0;
                        }
                        GhostConfig.save();
                        Chat.send("Set " + name + " color to #" + hex);
                    } catch (NumberFormatException e) {
                        Chat.send("§cColor must be hex, e.g. 55FFFF");
                        return 0;
                    }
                    return 1;
                }))));

        root.then(ClientCommands.literal("help").executes(ctx -> {
            Chat.send("§7/ghost §f- config screen");
            Chat.send("§7/ghost <start|pause|reset> §f- timed tracker");
            Chat.send("§7/ghost stats reset §f- reset the total tracker");
            Chat.send("§7/ghost hud <tracker|timer> <toggle | pos <x> <y> | scale <n>>");
            Chat.send("§7/ghost color <" + String.join("|", GhostConfig.COLOR_NAMES) + "> <hex>");
            return 1;
        }));

        dispatcher.register(root);
    }

    private GhostCommand() {
    }
}
