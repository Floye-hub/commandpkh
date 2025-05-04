package com.floye.commandpkh;
import com.floye.commandpkh.util.PlayerDataManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

public class TotalCaptureCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("Cette commande ne peut être exécutée que par un joueur."));
            return 0;
        }

        UUID playerId = player.getUuid();
        int totalCaptureCount = PlayerDataManager.getTotalCaptureCount(playerId);

        player.sendMessage(Text.literal("Vous avez capturé un total de " + totalCaptureCount + " Pokémon."), false);

        return 1;
    }
}
