package com.floye.commandpkh.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FirstJoinManager {

    private static final String FIRST_JOIN_DIR = "config/PKH/firstjoins/";

    // Vérifie si c'est la première connexion du joueur
    public static boolean isFirstJoin(UUID playerId) {
        File file = getPlayerFile(playerId);
        return !file.exists();
    }

    // Marque le joueur comme déjà connecté
    public static void markAsJoined(UUID playerId) {
        File file = getPlayerFile(playerId);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs(); // Crée les dossiers s'ils n'existent pas
                file.createNewFile(); // Crée le fichier
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Génère le chemin du fichier pour un joueur donné
    private static File getPlayerFile(UUID playerId) {
        return new File(FIRST_JOIN_DIR, playerId.toString() + ".dat");
    }
}