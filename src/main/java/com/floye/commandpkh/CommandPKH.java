package com.floye.commandpkh;

import com.floye.commandpkh.commands.*;
import com.floye.commandpkh.data.AventurePositionStorage;
import com.floye.commandpkh.data.HomePositionStorage;
import com.floye.commandpkh.util.FirstJoinManager;
import com.floye.commandpkh.util.TeleportHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

public class CommandPKH implements ModInitializer {
	public static final String MOD_ID = "commandpkh";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final String PLAYERDATA_DIR = "world/playerdata/";

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		AventurePositionStorage.load();
		registerCommands();
		MondeCommand.register();
		HomePositionStorage.load();
		TPTuto.register();
		CommandRegistrationCallback.EVENT.register(TpaCommand::register);
		CommandRegistrationCallback.EVENT.register(TpaHereCommand::register);
		CommandRegistrationCallback.EVENT.register(TpacceptCommand::register);
		CommandRegistrationCallback.EVENT.register(TpdenyCommand::register);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			UUID uuid = player.getUuid();

			if (FirstJoinManager.isFirstJoin(uuid)) {
				FirstJoinManager.markAsJoined(uuid);

				Identifier dimId = Identifier.of("monde", "tuto");
				RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, dimId);
				ServerWorld targetWorld = server.getWorld(dimKey);

				if (targetWorld != null) {
					player.stopRiding();
					Vec3d spawnPos = new Vec3d(98.5, 58, 26.5);
					float yaw = 0f;
					float pitch = 0f;

					player.teleport(targetWorld, spawnPos.x, spawnPos.y, spawnPos.z, yaw, pitch);
					player.sendMessage(Text.literal("§aBienvenue dans la dimension monde:tuto !"), false);
				}
			}
		});
	}

	// Vérifie si un fichier .dat pour le joueur existe
	private boolean hasPlayerDataFile(UUID playerId) {
		File playerDataFile = new File(PLAYERDATA_DIR, playerId.toString() + ".dat");
		return playerDataFile.exists();
	}


	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			// Enregistrer toutes vos commandes ici
			dispatcher.register(CommandManager.literal("rankupapprenti")
					.executes(new RankupApprentiCommand()));
			dispatcher.register(CommandManager.literal("TotalCapture")
					.executes(new TotalCaptureCommand()));
			SetHomeCommand.register(dispatcher);
			HomeCommand.register(dispatcher);
			DelHomeCommand.register(dispatcher);
			RtpCommand.register(dispatcher);
		});
	}
}