package com.floye.commandpkh.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class PlayerDataManager {

    private static final String PLAYER_DATA_DIR = "world/cobblemonplayerdata/";

    public static int getTotalCaptureCount(UUID playerId) {
        String playerDataFile = getPlayerDataFile(playerId);
        File file = new File(playerDataFile);

        if (!file.exists()) {
            return 0; // ou une valeur par défaut si le fichier n'existe pas
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject advancementData = jsonObject.getAsJsonObject("advancementData");
            if (advancementData != null) {
                return advancementData.get("totalCaptureCount").getAsInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0; // ou une valeur par défaut en cas d'erreur
    }

    private static String getPlayerDataFile(UUID playerId) {
        return PLAYER_DATA_DIR + playerId.toString().substring(0, 2) + "/" + playerId.toString() + ".json";
    }
}