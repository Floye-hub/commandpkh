package com.floye.commandpkh.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import static net.minecraft.server.command.CommandManager.literal;

public class SpawnCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("spawn")
                .executes(SpawnCommand::spawn));
    }

    private static int spawn(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            source.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        if (SetSpawnCommand.getSpawnPos() == null) {
            source.sendError(Text.literal("Spawn point is not set."));
            return 0;
        }

        // Fix: Use RegistryKey<World> instead of RegistryKey<DimensionType>
        Identifier dimensionId = Identifier.of(SetSpawnCommand.getSpawnDimension());
        RegistryKey<World> worldKey = RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimensionId);

        // Fix: Correctly retrieve the target world
        ServerWorld targetWorld = player.getServer().getWorld(worldKey);

        if (targetWorld == null) {
            source.sendError(Text.literal("Target world not found."));
            return 0;
        }

        BlockPos spawnPos = SetSpawnCommand.getSpawnPos();
        player.teleport(targetWorld, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getYaw(), player.getPitch());

        // Send a confirmation message to the player
        source.sendFeedback(() -> Text.literal("Teleported to spawn point."), false);
        return 1;
    }
}