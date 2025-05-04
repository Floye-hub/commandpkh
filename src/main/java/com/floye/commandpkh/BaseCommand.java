package com.floye.commandpkh;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;

public abstract class BaseCommand implements Command<ServerCommandSource> {


    protected abstract String getName();

    // Optionnel (retourne null par défaut)
    protected String getRequiredTag() {
        return null;
    }

    protected abstract int executeCommand(ServerCommandSource source);

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        return executeCommand(context.getSource());
    }
}