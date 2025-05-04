package com.floye.commandpkh;

import com.floye.commandpkh.commands.*;
import com.floye.commandpkh.data.AventurePositionStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandPKH implements ModInitializer {
	public static final String MOD_ID = "commandpkh";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		AventurePositionStorage.load();
		registerCommands();
		MondeCommand.register();
		TPTuto.register();
		CommandRegistrationCallback.EVENT.register(TpaCommand::register);
		CommandRegistrationCallback.EVENT.register(TpaHereCommand::register);
		CommandRegistrationCallback.EVENT.register(TpacceptCommand::register);
		CommandRegistrationCallback.EVENT.register(TpdenyCommand::register);
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			// Enregistrer toutes vos commandes ici
			dispatcher.register(CommandManager.literal("rankupapprenti")
					.executes(new RankupApprentiCommand()));
			dispatcher.register(CommandManager.literal("TotalCapture")
					.executes(new TotalCaptureCommand()));
		});
	}
}