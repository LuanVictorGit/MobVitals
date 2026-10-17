package me.lhawk.mobvitals.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import me.lhawk.mobvitals.death.CorpseManager;

import java.util.ArrayList;
import java.util.List;

public final class VitalPacketListener extends PacketListenerAbstract {
    private final CorpseManager corpseManager;

    public VitalPacketListener(CorpseManager corpseManager) {
        super(PacketListenerPriority.NORMAL);
        this.corpseManager = corpseManager;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
            handleDestroyEntities(event);
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_STATUS) {
            handleEntityStatus(event);
        }
    }

    private void handleDestroyEntities(PacketSendEvent event) {
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);
        int[] ids = destroy.getEntityIds();
        if (ids == null || ids.length == 0) return;

        int recipientEntityId = -1;
        try {
            Object playerObj = event.getPlayer();
            if (playerObj instanceof org.bukkit.entity.Player bp) {
                recipientEntityId = bp.getEntityId();
            }
        } catch (Throwable ignored) {}

        List<Integer> kept = new ArrayList<>(ids.length);
        boolean interceptedAny = false;

        for (int id : ids) {
            if (id == recipientEntityId) {
                kept.add(id);
                continue;
            }
            if (corpseManager.isDelayedDestroy(id)) {
                interceptedAny = true;
            } else {
                kept.add(id);
            }
        }

        if (!interceptedAny) return;

        if (kept.isEmpty()) {
            event.setCancelled(true);
        } else {
            int[] remaining = new int[kept.size()];
            for (int i = 0; i < kept.size(); i++) {
                remaining[i] = kept.get(i);
            }
            destroy.setEntityIds(remaining);
        }
    }

    private void handleEntityStatus(PacketSendEvent event) {
        WrapperPlayServerEntityStatus status = new WrapperPlayServerEntityStatus(event);
        int st = status.getStatus();
        if (st == 60 || st == 3) {
            int entityId = status.getEntityId();
            if (corpseManager.isDelayedDestroy(entityId)) {
                event.setCancelled(true);
            }
        }
    }
}
