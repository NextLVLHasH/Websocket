package com.NextLVLHasH.Websockets.systems;

import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.NextLVLHasH.Websockets.PlayerStatisticsManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

/**
 * ECS System to track mob kills by players using KillFeedEvent.
 * Listens to KillFeedEvent.KillerMessage which fires when an entity kills another.
 * 
 * Note: This requires the DamageModule dependency in manifest.json:
 * "Dependencies": { "Hytale:DamageModule": "*" }
 */
public class KillFeedTrackingSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    
    private static final Logger LOGGER = Logger.getLogger("KillFeedTrackingSystem");
    private final PlayerStatisticsManager statisticsManager;
    
    public KillFeedTrackingSystem(PlayerStatisticsManager statisticsManager) {
        super(KillFeedEvent.KillerMessage.class);
        this.statisticsManager = statisticsManager;
    }
    
    @Override
    public void handle(int i,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull KillFeedEvent.KillerMessage event) {
        
        try {
            // Get the killer entity (this system fires for the killer)
            Ref<EntityStore> killerRef = archetypeChunk.getReferenceTo(i);
            
            // Check if the killer is a player
            Player player = store.getComponent(killerRef, Player.getComponentType());
            
            if (player == null) {
                // Not a player kill, ignore
                return;
            }
            
            // Player killed something! Update statistics
            @SuppressWarnings("removal")
            PlayerRef playerRef = player.getPlayerRef();
            String playerUuid = playerRef.getUuid().toString();
            
            if (statisticsManager != null) {
                statisticsManager.onMobKilled(playerUuid);
                LOGGER.fine("Player " + playerRef.getUsername() + " killed an entity");
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error tracking kill feed: " + e.getMessage());
        }
    }
    
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        // Query entities that have PlayerRef component (only track player kills)
        return PlayerRef.getComponentType();
    }
}
