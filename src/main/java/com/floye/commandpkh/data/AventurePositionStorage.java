package com.floye.commandpkh.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AventurePositionStorage {

    private static final Gson GSON = new Gson();
    private static final File FILE = new File("config/aventures.json");

    // Classe interne pour stocker les données de position
    public static class PlayerAventureData {
        public int x, y, z;
        public float yaw, pitch;

        public PlayerAventureData(int x, int y, int z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    private static Map<UUID, PlayerAventureData> positions = new HashMap<>();

    public static void load() {
        try {
            if (!FILE.exists()) {
                FILE.getParentFile().mkdirs();
                save(); // Crée un fichier vide si inexistant
                return;
            }

            FileReader reader = new FileReader(FILE);
            Type type = new TypeToken<Map<UUID, PlayerAventureData>>() {}.getType();
            positions = GSON.fromJson(reader, type);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            FileWriter writer = new FileWriter(FILE);
            GSON.toJson(positions, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setPlayerPosition(UUID uuid, BlockPos pos, float yaw, float pitch) {
        positions.put(uuid, new PlayerAventureData(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch));
        save(); // Sauvegarde après chaque modification
    }

    public static PlayerAventureData getPlayerPosition(UUID uuid) {
        return positions.get(uuid);
    }
}