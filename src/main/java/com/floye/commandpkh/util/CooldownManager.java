package com.floye.commandpkh.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    /**
     * Définit un cooldown pour un joueur.
     *
     * @param playerId L'UUID du joueur
     * @param cooldownSeconds Durée du cooldown en secondes
     */
    public static void setCooldown(UUID playerId, int cooldownSeconds) {
        cooldowns.put(playerId, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    /**
     * Vérifie si un joueur est encore en cooldown.
     *
     * @param playerId L'UUID du joueur
     * @return true si le joueur est en cooldown, false sinon
     */
    public static boolean isOnCooldown(UUID playerId) {
        return cooldowns.containsKey(playerId) && cooldowns.get(playerId) > System.currentTimeMillis();
    }

    /**
     * Retourne le temps restant du cooldown en secondes.
     *
     * @param playerId L'UUID du joueur
     * @return Temps restant en secondes, ou 0 si le cooldown est expiré
     */
    public static int getRemainingTime(UUID playerId) {
        if (!isOnCooldown(playerId)) {
            return 0;
        }
        return (int) ((cooldowns.get(playerId) - System.currentTimeMillis()) / 1000);
    }

    /**
     * Réinitialise le cooldown d'un joueur (utile pour annuler un cooldown).
     *
     * @param playerId L'UUID du joueur
     */
    public static void resetCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}