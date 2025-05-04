package com.floye.commandpkh.util;

import com.floye.commandpkh.util.TeleportHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TpaManager {

    private static class TeleportRequest {
        final UUID requesterUuid;
        final UUID targetUuid;
        final long requestTime;
        final boolean isTpaHere;

        TeleportRequest(UUID requesterUuid, UUID targetUuid, boolean isTpaHere) {
            this.requesterUuid = requesterUuid;
            this.targetUuid = targetUuid;
            this.requestTime = System.currentTimeMillis();
            this.isTpaHere = isTpaHere;
        }
    }

    private static final Map<UUID, TeleportRequest> pendingRequests = new HashMap<>();
    private static final Map<UUID, UUID> activeOutgoingRequests = new HashMap<>();
    private static final long TIMEOUT_DURATION_MS = 60 * 1000;

    public static boolean hasOutgoingRequest(UUID requesterUuid) {
        return activeOutgoingRequests.containsKey(requesterUuid);
    }

    public static boolean hasIncomingRequest(UUID targetUuid) {
        return pendingRequests.containsKey(targetUuid);
    }

    public static void addRequest(ServerPlayerEntity requester, ServerPlayerEntity target) {
        UUID requesterUuid = requester.getUuid();
        UUID targetUuid = target.getUuid();

        if (isInSpecialDimension(target)) {
            requester.sendMessage(
                    Text.literal("le joueur destinataire n'est pas dans le monde Royaume !").formatted(Formatting.RED),
                    false
            );
            removeRequest(targetUuid, true);
            return;
        }

        if (isInSpecialDimension(requester)) {
            requester.sendMessage(
                    Text.literal("Tu dois être dans le monde Royaume !").formatted(Formatting.RED),
                    false
            );
            removeRequest(targetUuid, true);
            return;
        }
        removeRequest(targetUuid, false);
        if (activeOutgoingRequests.containsKey(requesterUuid)) {
            UUID old = activeOutgoingRequests.get(requesterUuid);
            removeRequest(old, false);
        }

        TeleportRequest request = new TeleportRequest(requesterUuid, targetUuid, false);
        pendingRequests.put(targetUuid, request);
        activeOutgoingRequests.put(requesterUuid, targetUuid);

        requester.sendMessage(
                Text.literal("TPA request sent to ").formatted(Formatting.GREEN)
                        .append(target.getDisplayName())
                        .append(Text.literal(". Wait for their response.").formatted(Formatting.GREEN)),
                false
        );

        MutableText acceptText = Text.literal("[Accept]")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to accept")))
                );
        MutableText denyText = Text.literal("[Deny]")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to deny")))
                );

        MutableText msg = Text.literal("")
                .append(requester.getDisplayName())
                .append(Text.literal(" wants to teleport to you.\n").formatted(Formatting.YELLOW))
                .append(acceptText).append(Text.literal(" ")).append(denyText);

        target.sendMessage(msg, false);
    }

    public static void addRequestHere(ServerPlayerEntity requester, ServerPlayerEntity target) {
        UUID requesterUuid = requester.getUuid();
        UUID targetUuid = target.getUuid();

        if (isInSpecialDimension(requester)) {
            requester.sendMessage(
                    Text.literal("Tu dois être dans le monde Royaume !").formatted(Formatting.RED),
                    false
            );
            removeRequest(targetUuid, true);
            return;
        }
        if (isInSpecialDimension(target)) {
            requester.sendMessage(
                    Text.literal("le joueur destinataire n'est pas dans le monde Royaume !").formatted(Formatting.RED),
                    false
            );

            removeRequest(targetUuid, true);
            return;
        }

        removeRequest(targetUuid, false);
        if (activeOutgoingRequests.containsKey(requesterUuid)) {
            UUID old = activeOutgoingRequests.get(requesterUuid);
            removeRequest(old, false);
        }

        TeleportRequest request = new TeleportRequest(requesterUuid, targetUuid, true);
        pendingRequests.put(targetUuid, request);
        activeOutgoingRequests.put(requesterUuid, targetUuid);

        requester.sendMessage(
                Text.literal("TPA request sent to ").formatted(Formatting.GREEN)
                        .append(target.getDisplayName())
                        .append(Text.literal(" to teleport them to you.").formatted(Formatting.GREEN)),
                false
        );

        MutableText acceptText = Text.literal("[Accept]")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to accept")))
                );
        MutableText denyText = Text.literal("[Deny]")
                .setStyle(Style.EMPTY
                        .withColor(Formatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to deny")))
                );

        MutableText msg = Text.literal("")
                .append(requester.getDisplayName())
                .append(Text.literal(" wants to teleport you to them.\n").formatted(Formatting.YELLOW))
                .append(acceptText).append(Text.literal(" ")).append(denyText);

        target.sendMessage(msg, false);
    }

    public static void acceptRequest(ServerPlayerEntity target) {
        TeleportRequest request = pendingRequests.get(target.getUuid());
        if (request == null) {
            target.sendMessage(
                    Text.literal("You don't have any pending TPA requests.").formatted(Formatting.RED),
                    false
            );
            return;
        }
        if (isExpired(request)) {
            target.sendMessage(
                    Text.literal("This TPA request has expired.").formatted(Formatting.RED),
                    false
            );
            removeRequest(target.getUuid(), true);
            return;
        }

        ServerPlayerEntity requester =
                target.getServer().getPlayerManager().getPlayer(request.requesterUuid);
        if (requester == null) {
            target.sendMessage(
                    Text.literal("The player who sent the request is offline.").formatted(Formatting.RED),
                    false
            );
            removeRequest(target.getUuid(), true);
            return;
        }

        if (request.isTpaHere) {
            // téléporte la cible vers le demandeur
            ServerWorld w = requester.getServerWorld();
            TeleportHelper.teleportPlayer(
                    target,
                    w,
                    requester.getX(), requester.getY(), requester.getZ(),
                    requester.getYaw(), requester.getPitch()
            );
        } else {
            // téléporte le demandeur vers la cible
            ServerWorld w = target.getServerWorld();
            TeleportHelper.teleportPlayer(
                    requester,
                    w,
                    target.getX(), target.getY(), target.getZ(),
                    target.getYaw(), target.getPitch()
            );
        }

        requester.sendMessage(
                Text.literal("Votre TPA a été acceptée par ").formatted(Formatting.GREEN)
                        .append(target.getDisplayName()),
                false
        );
        target.sendMessage(
                Text.literal("Vous avez accepté la TPA de ").formatted(Formatting.GREEN)
                        .append(requester.getDisplayName()),
                false
        );

        removeRequest(target.getUuid(), false);
    }

    public static void denyRequest(ServerPlayerEntity target) {
        TeleportRequest request = pendingRequests.get(target.getUuid());
        if (request == null) {
            target.sendMessage(
                    Text.literal("Vous n'avez pas de demandes TPA en attente.").formatted(Formatting.RED),
                    false
            );
            return;
        }

        ServerPlayerEntity requester =
                target.getServer().getPlayerManager().getPlayer(request.requesterUuid);
        if (requester != null) {
            requester.sendMessage(
                    Text.literal("Votre TPA a été refusée par ").formatted(Formatting.RED)
                            .append(target.getDisplayName()),
                    false
            );
        }

        target.sendMessage(
                Text.literal("Vous avez refusé la demande TPA.").formatted(Formatting.YELLOW),
                false
        );

        removeRequest(target.getUuid(), false);
    }

    public static void cleanExpiredRequests(MinecraftServer server) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, TeleportRequest>> it = pendingRequests.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TeleportRequest> e = it.next();
            TeleportRequest r = e.getValue();
            if (now - r.requestTime > TIMEOUT_DURATION_MS) {
                UUID tgt = e.getKey();
                UUID reqU = r.requesterUuid;
                it.remove();
                activeOutgoingRequests.remove(reqU);

                ServerPlayerEntity tgtPlayer = server.getPlayerManager().getPlayer(tgt);
                ServerPlayerEntity reqPlayer = server.getPlayerManager().getPlayer(reqU);

                if (tgtPlayer != null) {
                    tgtPlayer.sendMessage(
                            Text.literal("La TPA reçue de ")
                                    .formatted(Formatting.GRAY)
                                    .append(reqPlayer != null
                                            ? reqPlayer.getDisplayName()
                                            : Text.literal("un joueur déconnecté"))
                                    .append(Text.literal(" a expiré.")).formatted(Formatting.GRAY),
                            false
                    );
                }
                if (reqPlayer != null) {
                    reqPlayer.sendMessage(
                            Text.literal("Votre TPA envoyée à ")
                                    .formatted(Formatting.GRAY)
                                    .append(tgtPlayer != null
                                            ? tgtPlayer.getDisplayName()
                                            : Text.literal("un joueur déconnecté"))
                                    .append(Text.literal(" a expiré.")).formatted(Formatting.GRAY),
                            false
                    );
                }
            }
        }
    }

    // --- utilitaires ---
    private static boolean isExpired(TeleportRequest r) {
        return System.currentTimeMillis() - r.requestTime > TIMEOUT_DURATION_MS;
    }

    private static void removeRequest(UUID targetUuid, boolean notifyRequesterIfOffline) {
        TeleportRequest r = pendingRequests.remove(targetUuid);
        if (r != null) activeOutgoingRequests.remove(r.requesterUuid);
    }

    private static boolean isInSpecialDimension(ServerPlayerEntity player) {
        String[] dims = {
                "monde:ressource",
                "monde:event",
                "monde:aventure"
        };
        String cur = player.getWorld().getRegistryKey().getValue().toString();
        for (String d : dims) if (cur.equals(d)) return true;
        return false;
    }
}