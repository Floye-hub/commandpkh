package com.floye.commandpkh.util;

import com.floye.commandpkh.data.AventurePositionStorage;
import com.floye.commandpkh.data.AventurePositionStorage.PlayerAventureData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class TeleportHelper {
    private TeleportHelper() {}

    // Clé de la dimension aventure
    private static final RegistryKey<World> AVENTURE_DIM =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("monde", "aventure"));

    /**
     * Sauvegarde la position du joueur avant de le sortir du monde aventure.
     */
    public static void saveIfLeavingAventure(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey().equals(AVENTURE_DIM)) {
            BlockPos pos = player.getBlockPos();
            AventurePositionStorage.setPlayerPosition(
                    player.getUuid(), pos, player.getYaw(), player.getPitch()
            );
        }
    }

    /**
     * Téléport “Brigadier” depuis une commande :
     * feedback + sauvegarde si on quitte aventure.
     */
    public static int teleportTo(
            CommandContext<ServerCommandSource> ctx,
            RegistryKey<World> targetDim,
            Vec3d targetPos,
            float yaw,
            float pitch,
            String locationName
    ) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        MinecraftServer server = source.getServer();
        ServerWorld world = server.getWorld(targetDim);

        if (world == null) {
            source.sendError(Text.literal("§cMonde introuvable : " + targetDim.getValue()));
            return 0;
        }

        saveIfLeavingAventure(player);
        player.teleport(world, targetPos.x, targetPos.y, targetPos.z, yaw, pitch);
        source.sendFeedback(
                () -> Text.literal("§aTéléporté à « " + locationName + " »."),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Téléport “Brigadier” spécifique au monde aventure :
     * restitue la dernière position ou le point de spawn par défaut.
     */
    public static int teleportToAventure(
            CommandContext<ServerCommandSource> ctx
    ) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        MinecraftServer server = source.getServer();
        ServerWorld aventure = server.getWorld(AVENTURE_DIM);

        if (aventure == null) {
            source.sendError(Text.literal("§cMonde d'aventure introuvable."));
            return 0;
        }

        PlayerAventureData data = AventurePositionStorage.getPlayerPosition(player.getUuid());
        if (data == null) {
            // spawn par défaut
            Vec3d def = new Vec3d(0, 100, 0);
            player.teleport(aventure, def.x, def.y, def.z, 0f, 0f);
            source.sendFeedback(
                    () -> Text.literal("§aTéléporté au point de départ aventure."),
                    false
            );
        } else {
            BlockPos p = data.toBlockPos();
            player.teleport(
                    aventure,
                    p.getX() + 0.5, p.getY(), p.getZ() + 0.5,
                    data.yaw, data.pitch
            );
            source.sendFeedback(
                    () -> Text.literal("§aTéléporté à votre dernière position aventure."),
                    false
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Téléport « manuelle » depuis n’importe quel appel (ex : TpaManager) :
     * sauvegarde si on quitte aventure.
     */
    public static void teleportPlayer(
            ServerPlayerEntity player,
            ServerWorld targetWorld,
            double x, double y, double z,
            float yaw, float pitch
    ) {
        saveIfLeavingAventure(player);
        player.teleport(targetWorld, x, y, z, yaw, pitch);
    }
}