package com.floye.commandpkh.commands;

import com.floye.commandpkh.util.TeleportHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CityCommand {

    // Structure pour stocker les données d'une ville
    private static class CityData {
        final RegistryKey<World> dimension;
        final Vec3d position;
        final float yaw;
        final float pitch;
        final String name;

        CityData(String name, RegistryKey<World> dimension, Vec3d position, float yaw, float pitch) {
            this.name = name;
            this.dimension = dimension;
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    // Map pour stocker les villes disponibles
    private static final Map<String, CityData> CITIES = new HashMap<>();

    // Initialiser les villes disponibles
    static {
        // Dimension overworld
        RegistryKey<World> overworld = World.OVERWORLD;

        // Dimension nether
        RegistryKey<World> nether = World.NETHER;

        // Dimension end
        RegistryKey<World> end = World.END;

        // Dimension aventure (si disponible dans votre serveur)
        RegistryKey<World> aventure = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("monde", "aventure"));

        // Ajouter les villes (exemples)
        CITIES.put("end_city", new CityData("Cité de l'End", end, new Vec3d(1000, 70, 1000), 0f, 0f));
        CITIES.put("aventure_spawn", new CityData("Départ Aventure", aventure, new Vec3d(0, 100, 0), 0f, 0f));
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("city")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.argument("nom", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            CITIES.keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(CityCommand::executeCity))
                .executes(CityCommand::listCities));
    }

    private static int listCities(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("§eVilles disponibles:"), false);
        for (Map.Entry<String, CityData> entry : CITIES.entrySet()) {
            source.sendFeedback(() -> Text.literal("§a- " + entry.getKey() + " : " + entry.getValue().name), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int executeCity(CommandContext<ServerCommandSource> context) {
        try {
            String cityName = StringArgumentType.getString(context, "nom").toLowerCase();
            CityData city = CITIES.get(cityName);

            if (city == null) {
                context.getSource().sendError(Text.literal("§cVille inconnue: " + cityName));
                return 0;
            }

            // Utiliser le contexte actuel pour la téléportation
            return TeleportHelper.teleportTo(
                    context,
                    city.dimension,
                    city.position,
                    city.yaw,
                    city.pitch,
                    city.name
            );
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("§cErreur lors de la téléportation: " + e.getMessage()));
            return 0;
        }
    }
}