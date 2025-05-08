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

public class HomePositionStorage {

    private static final Gson GSON = new Gson();
    private static final File FILE = new File("config/homes.json");

    // Classe interne pour stocker les données de position
    public static class HomeData {
        public int x, y, z;
        public String dimension;
        public float yaw, pitch;

        public HomeData(int x, int y, int z, String dimension, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    private static Map<UUID, HomeData> homes = new HashMap<>();

    public static void load() {
        try {
            if (!FILE.exists()) {
                FILE.getParentFile().mkdirs();
                save(); // Crée un fichier vide si inexistant
                return;
            }

            FileReader reader = new FileReader(FILE);
            Type type = new TypeToken<Map<UUID, HomeData>>() {}.getType();
            homes = GSON.fromJson(reader, type);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            FileWriter writer = new FileWriter(FILE);
            GSON.toJson(homes, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setHome(UUID uuid, BlockPos pos, String dimension, float yaw, float pitch) {
        homes.put(uuid, new HomeData(pos.getX(), pos.getY(), pos.getZ(), dimension, yaw, pitch));
        save(); // Sauvegarde après chaque modification
    }

    public static HomeData getHome(UUID uuid) {
        return homes.get(uuid);
    }
    public static void deleteHome(UUID uuid) {
        if (homes.containsKey(uuid)) {
            homes.remove(uuid); // Supprime le "home" du joueur
            save(); // Sauvegarde les modifications
        }
    }
}