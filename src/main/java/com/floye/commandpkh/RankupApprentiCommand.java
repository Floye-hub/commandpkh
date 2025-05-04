package com.floye.commandpkh;

import com.floye.commandpkh.util.EconomyHandler;
import com.floye.commandpkh.util.PlayerDataManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RankupApprentiCommand implements Command<ServerCommandSource> {

    private static final double REQUIRED_BALANCE = 1000.0; // Balance requise pour exécuter la commande
    private static final String REQUIRED_TAG = "apprenti";

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("Cette commande ne peut être exécutée que par un joueur."));
            return 0;
        }
        if (!player.getCommandTags().contains(REQUIRED_TAG)) {
            context.getSource().sendError(Text.literal("Vous n'avez pas le tag requis pour effectuer cette commande."));
            return 0;
        }

        // Vérifier la balance de manière asynchrone
        CompletableFuture.runAsync(() -> checkBalanceAndExecute(player, context.getSource()));
        return 1;
    }

    private void checkBalanceAndExecute(ServerPlayerEntity player, ServerCommandSource source) {
        UUID playerId = player.getUuid();
        EconomyHandler.getAccount(playerId).thenAccept(account -> {
            if (account == null) {
                player.getServer().execute(() ->
                        player.sendMessage(Text.literal("Erreur lors de la récupération de votre compte."), false));
                return;
            }

            double balance = EconomyHandler.getBalance(account);
            int totalCaptureCount = PlayerDataManager.getTotalCaptureCount(playerId);

            if (balance >= REQUIRED_BALANCE && totalCaptureCount >= 50 ) {
                boolean withdrawalSuccess = EconomyHandler.remove(account, REQUIRED_BALANCE);

                player.getServer().execute(() -> {
                    if (withdrawalSuccess) {
                        try {
                            // Exécution de la commande de téléportation
                            source.getServer().getCommandManager().executeWithPrefix(
                                    source.getServer().getCommandSource(),
                                    "execute as " + player.getName().getString() + " run tp 200 100 200"
                            );
                            player.sendMessage(Text.literal("Rankup réussi ! Vous avez été téléporté."), false);
                        } catch (Exception e) {
                            player.sendMessage(Text.literal("Erreur lors de la téléportation."), false);
                            // Remettre l'argent si la commande échoue
                            EconomyHandler.add(account, REQUIRED_BALANCE);
                        }
                    } else {
                        player.sendMessage(Text.literal("Erreur lors du retrait de l'argent. Veuillez réessayer."), false);
                    }
                });
            } else {
                player.getServer().execute(() -> {
                    if (balance < REQUIRED_BALANCE) {
                        player.sendMessage(Text.literal("Vous n'avez pas assez d'argent. Nécessaire : " + REQUIRED_BALANCE), false);
                    }
                    if (totalCaptureCount < 50) {
                        player.sendMessage(Text.literal("Vous devez avoir au moins 50 captures pour effectuer cette action."), false);
                    }
                });
            }
        });
    }
}