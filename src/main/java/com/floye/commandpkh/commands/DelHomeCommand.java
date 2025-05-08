package com.floye.commandpkh.commands;

import com.floye.commandpkh.data.HomePositionStorage;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class DelHomeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("delhome")
                .requires(source -> source.hasPermissionLevel(0)) // Accessible à tous les joueurs
                .executes(DelHomeCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        UUID playerUuid = player.getUuid();

        // Vérifiez si le joueur a un home défini
        if (HomePositionStorage.getHome(playerUuid) == null) {
            player.sendMessage(Text.literal("§cVous n'avez pas de home défini."), false);
            return 0;
        }

        // Supprimez le home
        HomePositionStorage.deleteHome(playerUuid);
        player.sendMessage(Text.literal("§aVotre home a été supprimé."), false);

        return Command.SINGLE_SUCCESS;
    }
}