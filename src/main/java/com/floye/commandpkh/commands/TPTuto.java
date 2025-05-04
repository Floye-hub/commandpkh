package com.floye.commandpkh.commands;

import com.floye.commandpkh.util.TeleportHelper;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.literal;

public class TPTuto {

    private static final RegistryKey<World> EVENT_DIM =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("monde", "event"));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> {
            dispatcher.register(
                    literal("tuto")
                            .requires(src -> src.hasPermissionLevel(2))
                            .executes(ctx ->
                                    TeleportHelper.teleportTo(
                                            ctx,
                                            EVENT_DIM,
                                            new Vec3d(98.5, 58, 26.5),
                                            0f,
                                            0f,
                                            "Zone Tuto"
                                    )
                            )
            );
        });
    }
}