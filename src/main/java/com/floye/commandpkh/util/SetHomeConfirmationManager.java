package com.floye.commandpkh.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SetHomeConfirmationManager {
    private static final Set<UUID> pendingConfirmations = new HashSet<>();

    // Ajoute un joueur à la liste des confirmations en attente
    public static void addPendingConfirmation(UUID playerId) {
        pendingConfirmations.add(playerId);
    }

    // Vérifie si un joueur est en attente de confirmation
    public static boolean isPendingConfirmation(UUID playerId) {
        return pendingConfirmations.contains(playerId);
    }

    // Supprime un joueur de la liste des confirmations en attente
    public static void confirm(UUID playerId) {
        pendingConfirmations.remove(playerId);
    }
}