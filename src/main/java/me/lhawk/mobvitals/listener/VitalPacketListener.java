package me.lhawk.mobvitals.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import me.lhawk.mobvitals.death.CorpseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VitalPacketListener extends PacketListenerAbstract {
    private static final Set<PacketTypeCommon> FROZEN_FOR_CORPSES = Set.of(
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE,
            PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION,
            PacketType.Play.Server.ENTITY_ROTATION,
            PacketType.Play.Server.ENTITY_HEAD_LOOK,
            PacketType.Play.Server.ENTITY_VELOCITY,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_POSITION_SYNC,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_EQUIPMENT,
            PacketType.Play.Server.ENTITY_ANIMATION,
            PacketType.Play.Server.HURT_ANIMATION,
            PacketType.Play.Server.DAMAGE_EVENT
    );

    private final CorpseManager corpseManager;

    public VitalPacketListener(CorpseManager corpseManager) {
        super(PacketListenerPriority.NORMAL);
        this.corpseManager = corpseManager;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!corpseManager.hasCorpseBodies()) return;

        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            handleDestroyEntities(event);
        } else if (type == PacketType.Play.Server.ENTITY_STATUS) {
            if (corpseManager.isDelayedDestroyFor(peekEntityId(event, false), recipient(event))) {
                event.setCancelled(true);
            }
        } else if (FROZEN_FOR_CORPSES.contains(type)) {
            if (corpseManager.isDelayedDestroyFor(peekEntityId(event, true), recipient(event))) {
                event.setCancelled(true);
            }
        }
    }

    private void handleDestroyEntities(PacketSendEvent event) {
        WrapperPlayServerDestroyEntities destroy = new WrapperPlayServerDestroyEntities(event);
        int[] ids = destroy.getEntityIds();
        if (ids == null || ids.length == 0) return;

        UUID recipient = recipient(event);
        List<Integer> kept = new ArrayList<>(ids.length);
        boolean interceptedAny = false;

        for (int id : ids) {
            if (corpseManager.isDelayedDestroyFor(id, recipient)) {
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

    private static int peekEntityId(PacketSendEvent event, boolean varInt) {
        Object buffer = event.getByteBuf();
        int index = ByteBufHelper.readerIndex(buffer);
        try {
            return varInt ? ByteBufHelper.readVarInt(buffer) : ByteBufHelper.readInt(buffer);
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        } finally {
            ByteBufHelper.readerIndex(buffer, index);
        }
    }

    private static UUID recipient(PacketSendEvent event) {
        try {
            return event.getUser() != null ? event.getUser().getUUID() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
