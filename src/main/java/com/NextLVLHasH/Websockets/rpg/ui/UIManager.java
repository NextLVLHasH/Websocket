package com.NextLVLHasH.Websockets.rpg.ui;

import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.NextLVLHasH.Websockets.rpg.RPGManager;
import com.NextLVLHasH.Websockets.rpg.skills.SkillManager;
import com.NextLVLHasH.Websockets.rpg.skills.SkillTree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Singleton coordinator for all RPG UI screens.
 */
public final class UIManager {

    private static final Logger LOGGER = Logger.getLogger(UIManager.class.getName());
    private static volatile UIManager instance;
    private static final Object LOCK = new Object();

    public static UIManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new UIManager();
                }
            }
        }
        return instance;
    }

    public static void resetInstance() {
        synchronized (LOCK) {
            if (instance != null) {
                instance.shutdown();
                instance = null;
            }
        }
    }

    // UI State Tracking
    private final Map<UUID, Set<UIScreenType>> activeScreens;
    private final Map<UUID, CharacterSelectionUI> characterSelectionUIs;
    private final Map<UUID, CharacterSheetUI> characterSheetUIs;
    private final Map<UUID, SkillTreeUI> skillTreeUIs;
    private final Map<UUID, CombatLogUI> combatLogUIs;
    private Function<UUID, PlayerRPGData> rpgDataResolver;

    private UIManager() {
        this.activeScreens = new ConcurrentHashMap<>();
        this.characterSelectionUIs = new ConcurrentHashMap<>();
        this.characterSheetUIs = new ConcurrentHashMap<>();
        this.skillTreeUIs = new ConcurrentHashMap<>();
        this.combatLogUIs = new ConcurrentHashMap<>();
    }

    public void initialize() {
        LOGGER.info("UIManager initialized");
    }

    public void shutdown() {
        activeScreens.clear();
        characterSelectionUIs.clear();
        characterSheetUIs.clear();
        skillTreeUIs.clear();
        combatLogUIs.clear();
        LOGGER.info("UIManager shut down");
    }

    public void setRPGDataResolver(Function<UUID, PlayerRPGData> resolver) {
        this.rpgDataResolver = resolver;
    }

    // ==================== Character Selection ====================

    public void openCharacterCreation(UUID playerId) {
        closeConflictingScreens(playerId, UIScreenType.CHARACTER_CREATION);
        
        CharacterSelectionUI ui = new CharacterSelectionUI(playerId);
        characterSelectionUIs.put(playerId, ui);
        trackScreen(playerId, UIScreenType.CHARACTER_CREATION);
        ui.show();
    }

    public CharacterSelectionUI getCharacterSelectionUI(UUID playerId) {
        return characterSelectionUIs.get(playerId);
    }

    // ==================== Character Sheet ====================

    public void openCharacterSheet(UUID playerId) {
        closeConflictingScreens(playerId, UIScreenType.CHARACTER_SHEET);
        
        PlayerRPGData rpgData = resolveRPGData(playerId);
        CharacterSheetUI ui = new CharacterSheetUI(playerId, rpgData);
        characterSheetUIs.put(playerId, ui);
        trackScreen(playerId, UIScreenType.CHARACTER_SHEET);
        ui.show();
    }

    public CharacterSheetUI getCharacterSheetUI(UUID playerId) {
        return characterSheetUIs.get(playerId);
    }

    // ==================== Skill Tree ====================

    public void openSkillTree(UUID playerId, String treeId) {
        closeConflictingScreens(playerId, UIScreenType.SKILL_TREE);
        
        PlayerRPGData rpgData = resolveRPGData(playerId);
        SkillTreeUI ui = new SkillTreeUI(playerId, rpgData);
        skillTreeUIs.put(playerId, ui);
        trackScreen(playerId, UIScreenType.SKILL_TREE);
        ui.show(treeId);
    }

    public void openSkillTree(UUID playerId) {
        PlayerRPGData data = resolveRPGData(playerId);
        String className = data != null ? data.getSelectedClass() : null;
        // Look up skill tree by class name, not by tree ID
        SkillTree tree = className != null 
            ? SkillManager.getInstance().getSkillTreeByClass(className) 
            : null;
        String treeId = tree != null ? tree.getId() : "default";
        openSkillTree(playerId, treeId);
    }

    public SkillTreeUI getSkillTreeUI(UUID playerId) {
        return skillTreeUIs.get(playerId);
    }

    // ==================== Combat Log ====================

    public void openCombatLog(UUID playerId) {
        CombatLogUI ui = combatLogUIs.computeIfAbsent(playerId, CombatLogUI::new);
        trackScreen(playerId, UIScreenType.COMBAT_LOG);
        ui.show();
    }

    public void toggleCombatLog(UUID playerId) {
        CombatLogUI ui = combatLogUIs.computeIfAbsent(playerId, CombatLogUI::new);
        ui.toggle();
        if (ui.isVisible()) {
            trackScreen(playerId, UIScreenType.COMBAT_LOG);
        } else {
            untrackScreen(playerId, UIScreenType.COMBAT_LOG);
        }
    }

    public CombatLogUI getCombatLogUI(UUID playerId) {
        return combatLogUIs.get(playerId);
    }

    // ==================== Level Up Notification ====================

    public void showLevelUpNotification(UUID playerId, int newLevel) {
        LevelUpNotification.show(playerId, newLevel);
        
        CombatLogUI combatLog = combatLogUIs.get(playerId);
        if (combatLog != null) {
            combatLog.addLevelUpEntry(newLevel);
        }
    }

    // ==================== Close Operations ====================

    public void closeAllUI(UUID playerId) {
        closeCharacterCreation(playerId);
        closeCharacterSheet(playerId);
        closeSkillTree(playerId);
        closeCombatLog(playerId);
        activeScreens.remove(playerId);
    }

    public void closeCharacterCreation(UUID playerId) {
        CharacterSelectionUI ui = characterSelectionUIs.remove(playerId);
        if (ui != null) ui.close();
        untrackScreen(playerId, UIScreenType.CHARACTER_CREATION);
    }

    public void closeCharacterSheet(UUID playerId) {
        CharacterSheetUI ui = characterSheetUIs.remove(playerId);
        if (ui != null) ui.close();
        untrackScreen(playerId, UIScreenType.CHARACTER_SHEET);
    }

    public void closeSkillTree(UUID playerId) {
        SkillTreeUI ui = skillTreeUIs.remove(playerId);
        if (ui != null) ui.close();
        untrackScreen(playerId, UIScreenType.SKILL_TREE);
    }

    public void closeCombatLog(UUID playerId) {
        CombatLogUI ui = combatLogUIs.get(playerId);
        if (ui != null) ui.hide();
        untrackScreen(playerId, UIScreenType.COMBAT_LOG);
    }

    // ==================== Screen Tracking ====================

    private void trackScreen(UUID playerId, UIScreenType screenType) {
        activeScreens.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(screenType);
    }

    private void untrackScreen(UUID playerId, UIScreenType screenType) {
        Set<UIScreenType> screens = activeScreens.get(playerId);
        if (screens != null) screens.remove(screenType);
    }

    private void closeConflictingScreens(UUID playerId, UIScreenType opening) {
        Set<UIScreenType> conflicts = opening.getConflicts();
        for (UIScreenType conflict : conflicts) {
            if (isScreenOpen(playerId, conflict)) {
                closeScreen(playerId, conflict);
            }
        }
    }

    private void closeScreen(UUID playerId, UIScreenType screenType) {
        switch (screenType) {
            case CHARACTER_CREATION -> closeCharacterCreation(playerId);
            case CHARACTER_SHEET -> closeCharacterSheet(playerId);
            case SKILL_TREE -> closeSkillTree(playerId);
            case COMBAT_LOG -> closeCombatLog(playerId);
        }
    }

    public boolean isScreenOpen(UUID playerId, UIScreenType screenType) {
        Set<UIScreenType> screens = activeScreens.get(playerId);
        return screens != null && screens.contains(screenType);
    }

    public Set<UIScreenType> getOpenScreens(UUID playerId) {
        return activeScreens.getOrDefault(playerId, Collections.emptySet());
    }

    private PlayerRPGData resolveRPGData(UUID playerId) {
        if (rpgDataResolver != null) {
            return rpgDataResolver.apply(playerId);
        }
        return RPGManager.getInstance().getPlayerData(playerId);
    }

    // ==================== Screen Type Enum ====================

    public enum UIScreenType {
        CHARACTER_CREATION(Set.of()),
        CHARACTER_SHEET(Set.of()),
        SKILL_TREE(Set.of()),
        COMBAT_LOG(Set.of());

        private final Set<UIScreenType> conflicts;

        UIScreenType(Set<UIScreenType> conflicts) {
            this.conflicts = conflicts;
        }

        public Set<UIScreenType> getConflicts() {
            return conflicts;
        }
    }
}
