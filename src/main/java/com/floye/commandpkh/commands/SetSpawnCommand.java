package com.floye.commandpkh.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.server.command.CommandManager;

public class SetSpawnCommand {

    private static BlockPos spawnPos = null;
    private static String spawnDimension = "minecraft:overworld";

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> builder = literal("setspawn")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(SetSpawnCommand::setSpawn);

        dispatcher.register(builder);
    }

    private static int setSpawn(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = null;
        try {
            player = source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        spawnPos = player.getBlockPos();
        spawnDimension = player.getWorld().getRegistryKey().getValue().toString();
        source.sendFeedback(() -> Text.literal("Spawn point set to: " + spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() + " in dimension " + spawnDimension), true);
        return 1;
    }

    public static BlockPos getSpawnPos() {
        return spawnPos;
    }

    public static String getSpawnDimension() {
        return spawnDimension;
    }
}