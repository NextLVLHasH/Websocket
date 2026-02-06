package com.NextLVLHasH.Websockets.systems;

import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.NextLVLHasH.Websockets.PlayerStatisticsManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;

/**
 * ECS System to track blocks broken by players.
 * Listens to BreakBlockEvent and updates player statistics.
 */
public class BlockBreakTrackingSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    
    private static final Logger LOGGER = Logger.getLogger("BlockBreakTrackingSystem");
    private final PlayerStatisticsManager statisticsManager;
    
    public BlockBreakTrackingSystem(PlayerStatisticsManager statisticsManager) {
        super(BreakBlockEvent.class);
        this.statisticsManager = statisticsManager;
    }
    
    @Override
    public void handle(int i,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull BreakBlockEvent event) {
        
        // Skip air/empty blocks - BreakBlockEvent fires for air blocks too
        if (event.getBlockType() == null || event.getBlockType() == BlockType.EMPTY) {
            return;
        }
        
        try {
            // Get entity reference from the chunk
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
            
            // Get Player component
            Player player = store.getComponent(ref, Player.getComponentType());
            
            if (player == null) {
                return;
            }
            
            // Get player UUID from PlayerRef
            @SuppressWarnings("removal")
            PlayerRef playerRef = player.getPlayerRef();
            String playerUuid = playerRef.getUuid().toString();
            
            // Update statistics
            if (statisticsManager != null) {
                statisticsManager.onBlockBroken(playerUuid);
                LOGGER.fine("Player " + playerRef.getUsername() + " broke block: " + event.getBlockType().getId());
            }
            
        } catch (Exception e) {
            LOGGER.warning("Error tracking block break: " + e.getMessage());
        }
    }
    
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        // Query entities that have PlayerRef component
        return PlayerRef.getComponentType();
    }
}
