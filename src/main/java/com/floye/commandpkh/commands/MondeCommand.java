package com.floye.commandpkh.commands;

import com.floye.commandpkh.util.TeleportHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.literal;

public class MondeCommand {

    // ----------------------------
    // Définition des dimensions
    // ----------------------------
    private static final RegistryKey<World> ROYAUME_DIM =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("minecraft", "overworld"));
    private static final Vec3d ROYAUME_POS = new Vec3d(0, 109, 0);
    private static final float ROYAUME_YAW = 0f;
    private static final float ROYAUME_PITCH = 0f;

    private static final RegistryKey<World> RESSOURCE_DIM =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("monde", "ressource"));
    private static final int RESSOURCE_X = 10_000;
    private static final int RESSOURCE_Z = 10_000;
    // yaw / pitch identiques
    private static final float RESSOURCE_YAW   = 0f;
    private static final float RESSOURCE_PITCH = 0f;

    // ----------------------------
    // Enregistrement de la commande
    // ----------------------------
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            dispatcher.register(
                    literal("monde")
                            .requires(src -> src.hasPermissionLevel(2))

                            .then(literal("royaume")
                                    .executes(ctx ->
                                            TeleportHelper.teleportTo(
                                                    ctx,
                                                    ROYAUME_DIM,
                                                    ROYAUME_POS,
                                                    ROYAUME_YAW,
                                                    ROYAUME_PITCH,
                                                    "Royaume"
                                            )
                                    )
                            )

                            .then(literal("ressource")
                                    .executes(ctx -> {

                                        ServerCommandSource   src   = ctx.getSource();
                                        ServerWorld world = src.getServer().getWorld(RESSOURCE_DIM);

                                        // S’assure que le chunk est chargé (optionnel mais plus sûr)
                                        world.getChunk(RESSOURCE_X >> 4, RESSOURCE_Z >> 4);

                                        // Cherche le bloc le plus haut qui « bloque » le déplacement
                                        // (=> ignore l’air, la neige, les feuilles…)
                                        int surfaceY = world.getTopY(
                                                net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                                                RESSOURCE_X,
                                                RESSOURCE_Z
                                        ) + 1;           // +1 => au-dessus du bloc, pas dedans

                                        Vec3d surfacePos = new Vec3d(
                                                RESSOURCE_X + 0.5,  // milieu du bloc
                                                surfaceY,
                                                RESSOURCE_Z + 0.5
                                        );

                                        // Téléportation
                                        return TeleportHelper.teleportTo(
                                                ctx,
                                                RESSOURCE_DIM,
                                                surfacePos,
                                                RESSOURCE_YAW,
                                                RESSOURCE_PITCH,
                                                "Ressource"
                                        );
                                    })
                            )

                            .then(literal("aventure")
                                    .executes(TeleportHelper::teleportToAventure)
                            )
            );
        });
    }
}