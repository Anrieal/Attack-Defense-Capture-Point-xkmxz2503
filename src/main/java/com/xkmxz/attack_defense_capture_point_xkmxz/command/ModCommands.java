package com.xkmxz.attack_defense_capture_point_xkmxz.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xkmxz.attack_defense_capture_point_xkmxz.manager.CaptureManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ModCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("capturepoint")
                .requires(source -> source.hasPermission(2))

                // ---- Point commands ----
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ModCommands::createPoint)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ModCommands::removePoint)))
                .then(Commands.literal("setowner")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("owner", StringArgumentType.string())
                                        .executes(ModCommands::setOwner))))
                .then(Commands.literal("clearowner")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> setOwner(ctx, null))))
                .then(Commands.literal("list")
                        .executes(ModCommands::listAll))

                // ---- Zone commands ----
                .then(Commands.literal("zone")
                        .then(Commands.literal("create")
                                .then(Commands.argument("zoneName", StringArgumentType.word())
                                        .executes(ctx -> createZone(ctx, null))
                                        .then(Commands.argument("requiredZone", StringArgumentType.word())
                                                .executes(ctx -> createZone(ctx, StringArgumentType.getString(ctx, "requiredZone"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("zoneName", StringArgumentType.word())
                                        .executes(ModCommands::removeZone)))
                        .then(Commands.literal("addpoint")
                                .then(Commands.argument("zoneName", StringArgumentType.word())
                                        .then(Commands.argument("pointName", StringArgumentType.word())
                                                .executes(ModCommands::addPointToZone))))
                        .then(Commands.literal("removepoint")
                                .then(Commands.argument("zoneName", StringArgumentType.word())
                                        .then(Commands.argument("pointName", StringArgumentType.word())
                                                .executes(ModCommands::removePointFromZone))))
                        .then(Commands.literal("status")
                                .executes(ModCommands::zoneStatus)))
        );
    }

    private static int createPoint(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var name = StringArgumentType.getString(ctx, "name");
        var player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.capturepoint.error.player_only"));
            return 0;
        }

        var level = source.getLevel();
        var pos = player.blockPosition();
        var manager = CaptureManager.get(level);
        manager.addOrUpdatePoint(name, pos);

        source.sendSuccess(() -> Component.translatable("command.capturepoint.create.success", name, pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    private static int removePoint(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var name = StringArgumentType.getString(ctx, "name");
        var manager = CaptureManager.get(source.getLevel());

        if (manager.getPoints().containsKey(name)) {
            manager.removePoint(name);
            source.sendSuccess(() -> Component.translatable("command.capturepoint.remove.success", name), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("command.capturepoint.error.not_found", name));
            return 0;
        }
    }

    private static int setOwner(CommandContext<CommandSourceStack> ctx) {
        return setOwner(ctx, StringArgumentType.getString(ctx, "owner"));
    }

    private static int setOwner(CommandContext<CommandSourceStack> ctx, @org.jetbrains.annotations.Nullable String owner) {
        var source = ctx.getSource();
        var name = StringArgumentType.getString(ctx, "name");
        var manager = CaptureManager.get(source.getLevel());

        if (!manager.getPoints().containsKey(name)) {
            source.sendFailure(Component.translatable("command.capturepoint.error.not_found", name));
            return 0;
        }

        manager.setPointOwner(name, owner);
        if (owner != null) {
            source.sendSuccess(() -> Component.translatable("command.capturepoint.setowner.success", name, owner), true);
        } else {
            source.sendSuccess(() -> Component.translatable("command.capturepoint.clearowner.success", name), true);
        }
        return 1;
    }

    private static int listAll(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var manager = CaptureManager.get(source.getLevel());

        source.sendSuccess(() -> Component.translatable("command.capturepoint.list.header"), false);

        var points = manager.getPoints();
        if (points.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.capturepoint.list.no_points"), false);
        } else {
            for (var entry : points.values()) {
                var owner = entry.owner() != null ? entry.owner() : "none";
                source.sendSuccess(() -> Component.translatable("command.capturepoint.list.point_entry",
                        entry.name(), entry.pos().getX(), entry.pos().getY(), entry.pos().getZ(), owner), false);
            }
        }

        var zones = manager.getZones();
        if (!zones.isEmpty()) {
            for (var entry : zones.values()) {
                var captured = manager.isZoneCaptured(entry.name());
                var statusKey = captured ? "command.capturepoint.list.captured" : "command.capturepoint.list.not_captured";
                source.sendSuccess(() -> Component.translatable("command.capturepoint.list.zone_entry",
                        entry.name(),
                        Component.translatable(statusKey),
                        String.join(", ", entry.capturePoints())), false);
            }
        }
        return 1;
    }

    private static int createZone(CommandContext<CommandSourceStack> ctx, @org.jetbrains.annotations.Nullable String requiredZone) {
        var source = ctx.getSource();
        var zoneName = StringArgumentType.getString(ctx, "zoneName");
        var manager = CaptureManager.get(source.getLevel());

        if (manager.getZones().containsKey(zoneName)) {
            source.sendFailure(Component.translatable("command.capturepoint.error.zone_exists", zoneName));
            return 0;
        }

        manager.createZone(zoneName, requiredZone);
        if (requiredZone != null) {
            source.sendSuccess(() -> Component.translatable("command.capturepoint.zone.create_with_req.success", zoneName, requiredZone), true);
        } else {
            source.sendSuccess(() -> Component.translatable("command.capturepoint.zone.create.success", zoneName), true);
        }
        return 1;
    }

    private static int removeZone(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var zoneName = StringArgumentType.getString(ctx, "zoneName");
        var manager = CaptureManager.get(source.getLevel());

        if (manager.getZones().containsKey(zoneName)) {
            manager.removeZone(zoneName);
            source.sendSuccess(() -> Component.translatable("command.capturepoint.zone.remove.success", zoneName), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("command.capturepoint.error.zone_not_found", zoneName));
            return 0;
        }
    }

    private static int addPointToZone(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var zoneName = StringArgumentType.getString(ctx, "zoneName");
        var pointName = StringArgumentType.getString(ctx, "pointName");
        var manager = CaptureManager.get(source.getLevel());

        if (!manager.getZones().containsKey(zoneName)) {
            source.sendFailure(Component.translatable("command.capturepoint.error.zone_not_found", zoneName));
            return 0;
        }
        if (!manager.getPoints().containsKey(pointName)) {
            source.sendFailure(Component.translatable("command.capturepoint.error.not_found", pointName));
            return 0;
        }

        manager.addPointToZone(zoneName, pointName);
        source.sendSuccess(() -> Component.translatable("command.capturepoint.zone.addpoint.success", pointName, zoneName), true);
        return 1;
    }

    private static int removePointFromZone(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var zoneName = StringArgumentType.getString(ctx, "zoneName");
        var pointName = StringArgumentType.getString(ctx, "pointName");
        var manager = CaptureManager.get(source.getLevel());

        manager.removePointFromZone(zoneName, pointName);
        source.sendSuccess(() -> Component.translatable("command.capturepoint.zone.removepoint.success", pointName, zoneName), true);
        return 1;
    }

    private static int zoneStatus(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        var manager = CaptureManager.get(source.getLevel());
        var zones = manager.getZones();

        if (zones.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.capturepoint.list.no_zones"), false);
            return 1;
        }

        for (var entry : zones.values()) {
            var captured = manager.isZoneCaptured(entry.name());
            var accessible = manager.canAccessZone(entry.name());
            var statusKey = captured ? "command.capturepoint.list.captured" : "command.capturepoint.list.not_captured";
            var accessKey = accessible ? "command.capturepoint.list.accessible" : "command.capturepoint.list.locked";

            source.sendSuccess(() -> Component.translatable("command.capturepoint.zone.status_entry",
                    entry.name(),
                    Component.translatable(statusKey),
                    Component.translatable(accessKey)), false);
        }
        return 1;
    }
}
