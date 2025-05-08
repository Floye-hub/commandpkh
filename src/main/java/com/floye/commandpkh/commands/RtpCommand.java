package com.floye.commandpkh.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.Heightmap;
import net.minecraft.registry.tag.FluidTags;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public class RtpCommand {

    private static final Random random = new Random();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Map to store dimension-specific RTP zones
    private static final Map<net.minecraft.util.Identifier, Zone> dimensionZones = new HashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rtp")
                .executes(ctx -> execute(ctx.getSource()))
        );

        // Initialize dimension zones (example)
        initializeDimensionZones();
    }

    private static int execute(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        player.sendMessage(Text.literal("You will be teleported in 3 seconds..."), false);

        scheduler.schedule(() -> teleportPlayerRandomly(player), 3, TimeUnit.SECONDS);

        return 1;
    }

    private static void teleportPlayerRandomly(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        RegistryKey<World> dimensionKey = world.getRegistryKey();

        // Get the zone for the current dimension
        Zone zone = dimensionZones.get(dimensionKey);

        // If no zone is defined for this dimension, use a default zone or cancel
        if (zone == null) {
            player.sendMessage(Text.literal("RTP is not configured for this dimension."), false);
            return;
        }

        WorldBorder worldBorder = world.getWorldBorder();
        double size = zone.size / 2.0;
        double centerX = zone.centerX;
        double centerZ = zone.centerZ;

        double x, z;
        int y;
        int attempts = 0;
        final int maxAttempts = 10;

        do {
            x = centerX + (random.nextDouble() * 2 - 1) * size;
            z = centerZ + (random.nextDouble() * 2 - 1) * size;

            ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(x, 0, z));
            world.getChunkManager().addTicket(net.minecraft.server.world.ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getId());
            Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

            y = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) x & 15, (int) z & 15);

            attempts++;
        } while (
                (world.getFluidState(new BlockPos((int)x, y - 1, (int)z)).isIn(FluidTags.WATER)
                        || !worldBorder.contains(BlockPos.ofFloored(x, y, z)))
                        && attempts < maxAttempts
        );

        if (attempts >= maxAttempts) {
            player.sendMessage(Text.literal("Could not find a safe teleport location. Please try again."), false);
            return;
        }

        player.requestTeleport(x + 0.5, y + 1, z + 0.5);
        player.sendMessage(Text.literal("Teleported to a random location within the world border."), false);
    }

    // Helper class to define a zone
    private static class Zone {
        double size;
        double centerX;
        double centerZ;

        public Zone(double size, double centerX, double centerZ) {
            this.size = size;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }
    }

    // Method to initialize dimension-specific zones
    private static void initializeDimensionZones() {
        // Example: Overworld zone
        dimensionZones.put(net.minecraft.util.Identifier.of("minecraft:overworld"), new Zone(1000, 0, 0));

        dimensionZones.put(net.minecraft.util.Identifier.of("minecraft:the_end"), new Zone(800, 0, 0));
    }
}