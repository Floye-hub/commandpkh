package com.floye.commandpkh.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.floye.commandpkh.util.TpaManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TpaHereCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("tpahere")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(TpaHereCommand::execute)));
    }

    private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity requester = context.getSource().getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");

        if (requester == target) {
            requester.sendMessage(Text.literal("You cannot send a TPA request to yourself.").formatted(Formatting.RED), false);
            return 0;
        }

        if (TpaManager.hasOutgoingRequest(requester.getUuid())) {
            requester.sendMessage(Text.literal("You already have a pending TPA request. Wait for a response or expiration.").formatted(Formatting.RED), false);
            return 0;
        }

        TpaManager.addRequestHere(requester, target);

        return 1;
    }
}