package com.floye.commandpkh.commands;

import com.floye.commandpkh.data.HomePositionStorage;
import com.floye.commandpkh.data.HomePositionStorage.HomeData;
import com.floye.commandpkh.util.CooldownManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.UUID;

public class HomeCommand {

    // Durée du cooldown en secondes
    private static final int COOLDOWN_SECONDS = 600;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("home")
                .requires(source -> source.hasPermissionLevel(0)) // Accessible à tous
                .executes(HomeCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        UUID playerUuid = player.getUuid();

        // Vérifiez si le joueur est en cooldown
        if (CooldownManager.isOnCooldown(playerUuid)) {
            int remainingTime = CooldownManager.getRemainingTime(playerUuid);
            player.sendMessage(Text.literal("§cVous devez attendre encore " + remainingTime + " secondes avant d'utiliser /home."), false);
            return 0;
        }

        // Récupérez les données du "home" du joueur
        HomeData home = HomePositionStorage.getHome(playerUuid);
        if (home == null) {
            player.sendMessage(Text.literal("§cVous n'avez pas de home défini."), false);
            return 0;
        }

        String currentDimension = player.getWorld().getRegistryKey().getValue().toString();

        // Vérifiez si le joueur est dans la bonne dimension
        if (!home.dimension.equals(currentDimension)) {
            player.sendMessage(Text.literal("§cVotre home est défini dans une autre dimension : " + home.dimension), false);
            return 0;
        }

        // Téléportation au home
        player.teleport((ServerWorld) player.getWorld(), home.x + 0.5, home.y, home.z + 0.5, home.yaw, home.pitch);
        player.sendMessage(Text.literal("§aVous avez été téléporté à votre home."), false);

        // Appliquez le cooldown
        CooldownManager.setCooldown(playerUuid, COOLDOWN_SECONDS);

        return Command.SINGLE_SUCCESS;
    }
}