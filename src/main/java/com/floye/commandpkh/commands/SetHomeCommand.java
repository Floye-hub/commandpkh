package com.floye.commandpkh.commands;

import com.floye.commandpkh.data.HomePositionStorage;
import com.floye.commandpkh.util.SetHomeConfirmationManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;

public class SetHomeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sethome")
                .requires(source -> source.hasPermissionLevel(0)) // Pas de restriction de permission
                .executes(SetHomeCommand::requestConfirmation) // Première étape : demande de confirmation
                .then(CommandManager.literal("confirm")        // Deuxième étape : confirmation explicite
                        .executes(SetHomeCommand::setHome)));
    }

    // Étape 1 : Demande de confirmation avec message interactif
    private static int requestConfirmation(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        // Ajoutez le joueur à la liste des confirmations en attente
        SetHomeConfirmationManager.addPendingConfirmation(player.getUuid());

        // Message d'information
        player.sendMessage(Text.literal("§eVous êtes sur le point de définir un nouveau home."), false);
        player.sendMessage(Text.literal("§cAttention : Pour changer de home à l'avenir, il faudra payer auprès d'un PNJ."), false);

        // Message interactif avec le bouton [Valider]
        Text confirmButton = Texts.bracketed(Text.literal("§a[Valider]")
                .styled(style -> style
                        .withClickEvent(new net.minecraft.text.ClickEvent(
                                net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                                "/sethome confirm" // Commande exécutée lorsque l'utilisateur clique sur le bouton
                        ))
                        .withHoverEvent(new net.minecraft.text.HoverEvent(
                                net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Cliquez pour confirmer")
                        ))
                ));
        player.sendMessage(Text.literal("§eCliquez ici pour confirmer : ").append(confirmButton), false);

        return Command.SINGLE_SUCCESS;
    }

    // Étape 2 : Confirmation et définition du home
    private static int setHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

        // Vérifiez si le joueur est en attente de confirmation
        if (!SetHomeConfirmationManager.isPendingConfirmation(player.getUuid())) {
            player.sendMessage(Text.literal("§cVous n'avez pas de demande de sethome en attente."), false);
            return 0;
        }

        // Obtenez la position et les informations du joueur
        BlockPos pos = player.getBlockPos();
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        // Enregistrez la position du home
        HomePositionStorage.setHome(player.getUuid(), pos, dimension, yaw, pitch);

        // Supprimez le joueur de la liste des confirmations
        SetHomeConfirmationManager.confirm(player.getUuid());

        // Confirmation de l'action
        player.sendMessage(Text.literal("§aVotre home a été défini à votre position actuelle dans la dimension " + dimension + "."), false);
        player.sendMessage(Text.literal("§eRappelez-vous : Vous devrez payer un PNJ pour changer votre home."), false);

        return Command.SINGLE_SUCCESS;
    }
}