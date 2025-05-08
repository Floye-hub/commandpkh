package com.floye.commandpkh.commands;

import com.floye.commandpkh.util.EconomyHandler;
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
import net.impactdev.impactor.api.economy.accounts.Account;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
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

    // Map to store player cooldowns (player UUID -> last used time)
    private static final Map<java.util.UUID, Long> cooldowns = new HashMap<>();
    private static final int COOLDOWN_SECONDS = 60; // Set the cooldown time in seconds

    // Map to store dimension-specific minimum radii
    private static final Map<net.minecraft.util.Identifier, Integer> dimensionMinRadii = new HashMap<>();
    private static final int DEFAULT_MINIMUM_RADIUS = 200; // Default minimum radius

    //Map to store dimension-specific costs
    private static final Map<net.minecraft.util.Identifier, Double> dimensionCosts = new HashMap<>();
    private static final double DEFAULT_COST = 100.0;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rtp")
                .executes(ctx -> execute(ctx.getSource()))
        );

        // Initialize dimension zones (example)
        initializeDimensionZones();
        initializeDimensionMinRadii(); // Initialize dimension-specific radii
        initializeDimensionCosts();
    }

    private static int execute(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command can only be used by a player."));
            return 0;
        }

        java.util.UUID playerUUID = player.getUuid();
        long currentTime = System.currentTimeMillis();

        if (cooldowns.containsKey(playerUUID)) {
            long lastUsed = cooldowns.get(playerUUID);
            long timeSinceLastUse = (currentTime - lastUsed) / 1000; // Convert to seconds

            if (timeSinceLastUse < COOLDOWN_SECONDS) {
                long timeLeft = COOLDOWN_SECONDS - timeSinceLastUse;
                source.sendError(Text.literal("You must wait " + timeLeft + " seconds before using this command again."));
                return 0;
            }
        }

        // Update the last used time
        cooldowns.put(playerUUID, currentTime);

        //Get the players account
        CompletableFuture<Account> accountFuture = EconomyHandler.getAccount(playerUUID);

        accountFuture.thenAccept(account -> {
            if(account == null){
                source.sendError(Text.literal("Could not find economy account."));
                return;
            }

            //Get the cost for the dimension
            double cost = dimensionCosts.getOrDefault(player.getServerWorld().getRegistryKey().getValue(), DEFAULT_COST);

            //Check if player has enough money
            if(!EconomyHandler.tryRemove(account, cost)){
                source.sendError(Text.literal("You do not have enough money to use this command. Cost: " + cost));
                return;
            }

            player.sendMessage(Text.literal("You will be teleported in 3 seconds... Cost: " + cost), false);
            scheduler.schedule(() -> teleportPlayerRandomly(player), 3, TimeUnit.SECONDS);
        });
        return 1;
    }

    private static void teleportPlayerRandomly(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        RegistryKey<World> dimensionKey = world.getRegistryKey();
        net.minecraft.util.Identifier dimensionId = dimensionKey.getValue();

        // Get the zone for the current dimension
        Zone zone = dimensionZones.get(dimensionId);

        // If no zone is defined for this dimension, use a default zone or cancel
        if (zone == null) {
            player.sendMessage(Text.literal("RTP is not configured for this dimension."), false);
            return;
        }

        // Get the minimum radius for the current dimension
        int minimumRadius = dimensionMinRadii.getOrDefault(dimensionId, DEFAULT_MINIMUM_RADIUS);

        WorldBorder worldBorder = world.getWorldBorder();
        double size = zone.size / 2.0;
        double centerX = zone.centerX;
        double centerZ = zone.centerZ;

        double x = 0, z = 0; // Initialize x and z
        int y = 0; // Initialize y to a default value
        boolean foundValidLocation = false; // Flag to track if a valid location was found

        int attempts = 0;
        final int maxAttempts = 10;

        do {
            x = centerX + (random.nextDouble() * 2 - 1) * size;
            z = centerZ + (random.nextDouble() * 2 - 1) * size;

            // Check if the generated coordinates are within the minimum radius
            double distance = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2));
            if (distance < minimumRadius) {
                continue; // Skip this attempt and generate new coordinates
            }

            ChunkPos chunkPos = new ChunkPos(BlockPos.ofFloored(x, 0, z));
            world.getChunkManager().addTicket(net.minecraft.server.world.ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getId());
            Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);

            y = chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) x & 15, (int) z & 15);

            if (!(world.getFluidState(new BlockPos((int)x, y - 1, (int)z)).isIn(FluidTags.WATER)
                    || !worldBorder.contains(BlockPos.ofFloored(x, y, z)))) {
                foundValidLocation = true;
                break; // Exit the loop if a valid location is found
            }

            attempts++;
        } while (attempts < maxAttempts);

        if (!foundValidLocation) {
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

    // Method to initialize dimension-specific minimum radii
    private static void initializeDimensionMinRadii() {
        dimensionMinRadii.put(net.minecraft.util.Identifier.of("minecraft:overworld"), 300); // Example: Overworld - 300
        dimensionMinRadii.put(net.minecraft.util.Identifier.of("minecraft:the_nether"), 100); // Example: Nether - 100
        // If a dimension is not in this map, it will use the DEFAULT_MINIMUM_RADIUS
    }

    private static void initializeDimensionCosts(){
        dimensionCosts.put(net.minecraft.util.Identifier.of("minecraft:overworld"), 50.0);
        dimensionCosts.put(net.minecraft.util.Identifier.of("minecraft:the_nether"), 150.0);
    }
}