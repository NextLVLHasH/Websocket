# RPG System Design Document
**Project:** NextLVLHasH Hytale Websocket Plugin  
**Date:** February 5, 2026  
**Version:** 1.0

---

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [Data Models](#data-models)
4. [Core Systems](#core-systems)
5. [Implementation Phases](#implementation-phases)
6. [File Structure](#file-structure)
7. [Configuration Format](#configuration-format)
8. [API Reference](#api-reference)

---

## 🎯 System Overview

### Vision
Create a fully-featured, modular RPG system for Hytale that includes:
- **Character Customization**: Class, Race, and Profession selection
- **Progression**: Dynamic leveling with configurable XP curves
- **Restrictions**: Equipment and inventory limitations based on character attributes
- **Modifiers**: Health, Mana, Stamina, and Regeneration scaling
- **Skills**: Permission-based skill trees with unlockable abilities
- **Combat**: Advanced damage calculation and effect system
- **UI**: Interactive GUI for character management
- **Persistence**: JSON-based data storage per player

### Design Principles
1. **Modularity**: Each system is independent and can be enabled/disabled
2. **Performance**: Event-driven architecture with minimal polling
3. **Extensibility**: Easy to add new classes, races, skills, and effects
4. **Balance**: Configurable formulae for game masters to fine-tune
5. **Immersion**: Seamless integration with Hytale's existing systems

---

## 🏗️ Architecture

### High-Level Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    RPGManager (Facade)                       │
│  - Initialize all subsystems                                 │
│  - Coordinate inter-system communication                     │
│  - Handle plugin lifecycle                                   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼──────┐    ┌────────▼─────┐    ┌─────────▼────────┐
│ Character    │    │  Progression │    │    Combat        │
│ System       │    │  System      │    │    System        │
│              │    │              │    │                  │
│ - Classes    │    │ - Leveling   │    │ - Damage Calc   │
│ - Races      │    │ - XP Curves  │    │ - Effects       │
│ - Professions│    │ - Modifiers  │    │ - Status Mgmt   │
└──────┬───────┘    └──────┬───────┘    └────────┬─────────┘
       │                   │                     │
┌──────▼───────────────────▼─────────────────────▼─────────┐
│              PlayerRPGData (Per-Player State)             │
│  - Current Class/Race/Profession                          │
│  - Level, XP, Attributes                                  │
│  - Unlocked Skills                                        │
│  - Active Effects                                         │
└───────────────────────────────────────────────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │  Persistence Layer │
                    │  (JSON Storage)    │
                    └────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Supporting Systems                        │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Skill System │  │ Attribute    │  │ Restriction  │     │
│  │ - Skill Nodes│  │ System       │  │ System       │     │
│  │ - Cooldowns  │  │ - Stats      │  │ - Equipment  │     │
│  │ - Permissions│  │ - Formulas   │  │ - Inventory  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ GUI System   │  │ Event System │                        │
│  │ - CharSelect │  │ - Listeners  │                        │
│  │ - Stats UI   │  │ - Handlers   │                        │
│  │ - Skill Tree │  │ - Dispatchers│                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure
```
com.NextLVLHasH.Websockets.rpg/
├── RPGManager.java                     // Main facade/coordinator
├── core/
│   ├── PlayerRPGData.java              // Per-player data container
│   └── RPGConfig.java                  // Configuration loader
├── character/
│   ├── CharacterClass.java             // Class definition
│   ├── Race.java                       // Race definition
│   ├── Profession.java                 // Profession definition
│   ├── CharacterManager.java           // Character selection logic
│   └── RaceModelManager.java           // Custom race model handling
├── progression/
│   ├── Level.java                      // Level state
│   ├── Experience.java                 // XP tracking
│   ├── ExperienceCurve.java            // XP curve strategies
│   ├── LevelUpModifier.java            // Stat modifiers on level-up
│   └── ProgressionManager.java         // Leveling coordinator
├── attributes/
│   ├── Attribute.java                  // Generic attribute (STR, DEX, etc.)
│   ├── AttributeType.java              // Enum of attribute types
│   ├── AttributeModifier.java          // Temporary/permanent modifiers
│   └── AttributeManager.java           // Attribute calculation
├── skills/
│   ├── Skill.java                      // Skill definition
│   ├── SkillNode.java                  // Skill tree node
│   ├── SkillTree.java                  // Full skill tree
│   ├── SkillCooldown.java              // Cooldown tracker
│   ├── SkillPermission.java            // Permission validation
│   └── SkillManager.java               // Skill unlock/usage
├── combat/
│   ├── Damage.java                     // Damage instance
│   ├── DamageType.java                 // Physical, Magical, etc.
│   ├── DamageCalculator.java           // Damage formula engine
│   ├── CombatEffect.java               // Status effect definition
│   ├── EffectType.java                 // Poison, Burn, Slow, etc.
│   ├── EffectInstance.java             // Active effect on player
│   └── CombatManager.java              // Combat event handler
├── restrictions/
│   ├── EquipmentRestriction.java       // Equipment rules
│   ├── InventoryRestriction.java       // Inventory size rules
│   └── RestrictionManager.java         // Validation logic
├── ui/
│   ├── CharacterSelectionUI.java       // Class/Race/Prof selection
│   ├── AttributesUI.java               // Character stats screen
│   ├── SkillTreeUI.java                // Skill tree interface
│   ├── CombatLogUI.java                // Damage/effect feed
│   └── UIManager.java                  // UI coordinator
├── events/
│   ├── RPGPlayerEvent.java             // Base event
│   ├── LevelUpEvent.java               // Level-up event
│   ├── SkillUnlockEvent.java           // Skill unlock event
│   ├── CombatEvent.java                // Damage/effect event
│   └── CharacterSelectionEvent.java    // Class/Race selection
└── persistence/
    ├── RPGDataSerializer.java          // JSON serialization
    └── RPGDataRepository.java          // File I/O operations
```

---

## 📊 Data Models

### 1. CharacterClass
```java
public class CharacterClass {
    private String id;                          // "warrior", "mage", etc.
    private String displayName;                 // "Warrior"
    private String description;                 // Lore text
    private Map<AttributeType, Integer> baseAttributes;
    private Map<AttributeType, Double> attributeGrowth;  // Per level
    private List<String> startingSkills;        // Skill IDs
    private EquipmentRestriction equipmentRules;
    private Map<String, Double> modifiers;      // Custom multipliers
    private String iconPath;                    // UI icon
}
```

### 2. Race
```java
public class Race {
    private String id;                          // "human", "elf", "orc"
    private String displayName;                 // "Elf"
    private String description;
    private Map<AttributeType, Integer> racialBonuses;
    private List<String> racialAbilities;       // Passive skill IDs
    private double heightScale;                 // Model scaling
    private double speedModifier;               // Movement speed
    private String modelPath;                   // Custom model path
    private Map<DamageType, Double> resistances; // Elemental resistances
}
```

### 3. Profession
```java
public class Profession {
    private String id;                          // "blacksmith", "alchemist"
    private String displayName;                 // "Blacksmith"
    private String description;
    private List<String> allowedClasses;        // Class restrictions
    private SkillTree professionSkills;         // Unique skill tree
    private Map<String, Integer> resourceBonuses; // Gathering/crafting bonuses
    private int maxLevel;                       // Profession level cap
}
```

### 4. PlayerRPGData
```java
public class PlayerRPGData {
    private UUID playerId;
    private String playerName;
    
    // Character Identity
    private String selectedClass;
    private String selectedRace;
    private String selectedProfession;
    private boolean characterCreated;
    
    // Progression
    private int level;
    private long currentXP;
    private long xpToNextLevel;
    private int skillPoints;
    
    // Attributes
    private Map<AttributeType, Integer> baseAttributes;
    private Map<AttributeType, Integer> currentAttributes;
    private List<AttributeModifier> activeModifiers;
    
    // Health/Mana/Stamina
    private double maxHealth;
    private double currentHealth;
    private double maxMana;
    private double currentMana;
    private double maxStamina;
    private double currentStamina;
    private double healthRegen;
    private double manaRegen;
    private double staminaRegen;
    
    // Skills
    private Set<String> unlockedSkills;
    private Map<String, Long> skillCooldowns;
    
    // Combat
    private List<EffectInstance> activeEffects;
    private long lastCombatTime;
    
    // Timestamps
    private long createdAt;
    private long lastUpdated;
}
```

### 5. ExperienceCurve
```java
public interface ExperienceCurve {
    long calculateXPForLevel(int level);
    int calculateLevelFromXP(long totalXP);
}

// Implementations:
// - LinearCurve: baseXP + (level * increment)
// - ExponentialCurve: baseXP * Math.pow(multiplier, level)
// - PolynomialCurve: a*level^3 + b*level^2 + c*level + d
// - CustomCurve: JSON-defined breakpoints
```

### 6. Skill
```java
public class Skill {
    private String id;
    private String displayName;
    private String description;
    private SkillType type;                     // ACTIVE, PASSIVE, TOGGLE
    private int requiredLevel;
    private List<String> prerequisiteSkills;    // Skill tree dependencies
    private int skillPointCost;
    private long cooldownMillis;
    private double manaCost;
    private double staminaCost;
    private List<String> requiredPermissions;   // Permission nodes
    private Map<String, Object> effectData;     // Skill-specific data
    private String iconPath;
}
```

### 7. CombatEffect
```java
public class CombatEffect {
    private String id;
    private String displayName;
    private EffectType type;                    // BUFF, DEBUFF, DOT, HOT
    private int duration;                       // Ticks or milliseconds
    private int tickInterval;                   // For over-time effects
    private Map<AttributeType, Double> attributeModifiers;
    private double damagePerTick;               // For DOT
    private double healingPerTick;              // For HOT
    private boolean stackable;
    private int maxStacks;
    private String particleEffect;              // Visual indicator
}
```

### 8. Damage
```java
public class Damage {
    private UUID attackerId;
    private UUID targetId;
    private DamageType type;                    // PHYSICAL, MAGICAL, TRUE, FIRE, etc.
    private double rawAmount;
    private double mitigatedAmount;
    private boolean isCritical;
    private double critMultiplier;
    private String sourceSkill;                 // Skill that caused damage
    private List<CombatEffect> appliedEffects;
    private long timestamp;
}
```

---

## ⚙️ Core Systems

### 1. Character System

**Responsibilities:**
- Handle character creation flow
- Store class/race/profession selections
- Apply racial and class bonuses
- Manage custom race models

**Key Classes:**
- `CharacterManager`: Main coordinator
- `CharacterClass`, `Race`, `Profession`: Data models
- `RaceModelManager`: Custom model rendering

**Events:**
- `CharacterCreatedEvent`
- `ClassChangedEvent` (if respeccing is allowed)

**Configuration Example:**
```json
{
  "classes": [
    {
      "id": "warrior",
      "displayName": "Warrior",
      "description": "Master of melee combat",
      "baseAttributes": {
        "STRENGTH": 15,
        "DEXTERITY": 10,
        "INTELLIGENCE": 5,
        "CONSTITUTION": 12,
        "WISDOM": 7,
        "CHARISMA": 6
      },
      "attributeGrowth": {
        "STRENGTH": 2.0,
        "CONSTITUTION": 1.5,
        "DEXTERITY": 0.5
      },
      "startingSkills": ["power_strike", "shield_bash"],
      "equipmentRules": {
        "allowedWeaponTypes": ["SWORD", "AXE", "MACE", "SPEAR"],
        "allowedArmorTypes": ["HEAVY", "MEDIUM"],
        "minStrengthForHeavyArmor": 12
      }
    }
  ]
}
```

---

### 2. Progression System

**Responsibilities:**
- Track player XP and level
- Calculate XP requirements using configurable curves
- Apply level-up modifiers (health, mana, stamina, regen)
- Award skill points on level-up

**Key Classes:**
- `ProgressionManager`: Leveling logic
- `ExperienceCurve`: XP calculation strategy
- `LevelUpModifier`: Stat scaling per level

**XP Curve Types:**
1. **Linear**: `XPRequired = 100 * level`
2. **Exponential**: `XPRequired = 100 * 1.15^level`
3. **Polynomial**: `XPRequired = level^3 + 50*level`
4. **Custom**: JSON-defined per-level XP

**Level-Up Formulas:**
```
MaxHealth = baseHealth + (level * healthPerLevel) + (constitution * 5)
MaxMana = baseMana + (level * manaPerLevel) + (intelligence * 3)
MaxStamina = baseStamina + (level * staminaPerLevel) + (constitution * 2)

HealthRegen = baseRegen + (level * 0.05) + (constitution * 0.1)
ManaRegen = baseRegen + (level * 0.03) + (wisdom * 0.15)
StaminaRegen = baseRegen + (level * 0.04) + (constitution * 0.08)
```

**Events:**
- `ExperienceGainedEvent`
- `LevelUpEvent` (includes new stats)
- `SkillPointsAwardedEvent`

---

### 3. Attribute System

**Responsibilities:**
- Manage core attributes (STR, DEX, INT, CON, WIS, CHA)
- Apply temporary and permanent modifiers
- Recalculate derived stats when attributes change

**Attribute Types:**
- **STRENGTH**: Physical damage, carry capacity
- **DEXTERITY**: Attack speed, dodge chance, ranged damage
- **INTELLIGENCE**: Magical damage, mana pool
- **CONSTITUTION**: Health pool, physical resistance
- **WISDOM**: Mana regeneration, magical resistance
- **CHARISMA**: NPC interactions, pet bonuses

**Modifier System:**
```java
public class AttributeModifier {
    private String source;              // "Potion of Strength", "Warrior Passive"
    private AttributeType attribute;
    private ModifierType type;          // FLAT, PERCENTAGE, MULTIPLICATIVE
    private double value;
    private long expiresAt;             // 0 = permanent
    private int priority;               // Application order
}
```

**Calculation Order:**
1. Base attribute (from class + race)
2. Flat modifiers (+10 STR)
3. Percentage modifiers (+20% STR)
4. Multiplicative modifiers (×1.5 STR)

---

### 4. Skill System

**Responsibilities:**
- Manage skill tree progression
- Validate skill prerequisites
- Handle skill cooldowns
- Check permission nodes
- Execute skill effects

**Skill Tree Structure:**
```
                    [Root Skill]
                         │
        ┌────────────────┼────────────────┐
        │                │                │
    [Tier 1A]       [Tier 1B]       [Tier 1C]
        │                │                │
    [Tier 2A]       [Tier 2B]            │
        │                                 │
    [Tier 3A]────────────────────────[Tier 3B]
```

**Permission Integration:**
- Skill unlock grants permission node: `rpg.skill.<skillId>`
- Allows integration with protection plugins
- Can gate commands, items, or areas behind skills

**Cooldown System:**
- Per-skill cooldown tracking
- Configurable cooldown reduction from attributes
- UI indicators for cooldown status

**Configuration Example:**
```json
{
  "skills": [
    {
      "id": "fireball",
      "displayName": "Fireball",
      "description": "Launch a ball of fire at your target",
      "type": "ACTIVE",
      "requiredLevel": 5,
      "prerequisiteSkills": ["fire_mastery_1"],
      "skillPointCost": 1,
      "cooldownMillis": 5000,
      "manaCost": 25,
      "staminaCost": 0,
      "requiredPermissions": ["rpg.magic.fire"],
      "effectData": {
        "damage": 50,
        "damageType": "FIRE",
        "range": 30,
        "aoeRadius": 3,
        "burnDuration": 3000
      }
    }
  ]
}
```

---

### 5. Combat System

**Responsibilities:**
- Calculate damage with mitigation
- Apply combat effects (buffs/debuffs)
- Manage damage-over-time ticks
- Track combat state

**Damage Calculation:**
```java
// Base formula
double finalDamage = rawDamage;

// 1. Apply attacker modifiers
finalDamage *= (1 + getStrengthBonus(attacker));
finalDamage *= (1 + getWeaponModifier(attacker));

// 2. Apply critical hits
if (isCritical) {
    finalDamage *= getCritMultiplier(attacker);
}

// 3. Apply target resistances
double resistance = getResistance(target, damageType);
finalDamage *= (1 - resistance);

// 4. Apply armor mitigation
double armor = getArmor(target);
double mitigation = armor / (armor + 100);
finalDamage *= (1 - mitigation);

// 5. Apply active effects
for (EffectInstance effect : target.getActiveEffects()) {
    if (effect.getType() == DAMAGE_REDUCTION) {
        finalDamage *= (1 - effect.getAmount());
    }
}

return finalDamage;
```

**Effect Types:**
- **DOT (Damage Over Time)**: Poison, Burn, Bleed
- **HOT (Healing Over Time)**: Regeneration, Life Steal
- **BUFF**: Increased stats, speed, damage
- **DEBUFF**: Decreased stats, slow, weakness
- **CROWD_CONTROL**: Stun, Root, Silence

**Effect Stacking:**
- **Replace**: New effect replaces old (Poison II > Poison I)
- **Stack**: Multiple instances can coexist (up to max stacks)
- **Refresh**: New application extends duration

---

### 6. Restriction System

**Responsibilities:**
- Validate equipment based on class/level/attributes
- Restrict inventory size based on character stats
- Enforce profession-specific limitations

**Equipment Restrictions:**
```java
public boolean canEquip(Player player, ItemStack item) {
    PlayerRPGData data = getRPGData(player);
    CharacterClass charClass = getClass(data.getSelectedClass());
    
    // Check weapon type
    if (!charClass.getAllowedWeaponTypes().contains(item.getWeaponType())) {
        return false;
    }
    
    // Check armor type
    if (!charClass.getAllowedArmorTypes().contains(item.getArmorType())) {
        return false;
    }
    
    // Check level requirement
    if (data.getLevel() < item.getRequiredLevel()) {
        return false;
    }
    
    // Check attribute requirements
    if (data.getAttribute(STRENGTH) < item.getRequiredStrength()) {
        return false;
    }
    
    return true;
}
```

**Inventory Restrictions:**
- Base inventory slots: 20
- +2 slots per Constitution point
- +5 slots per level (up to max)
- Profession bonuses (e.g., Merchant +10 slots)

---

### 7. GUI System

**Responsibilities:**
- Character creation wizard
- Stats and level display
- Skill tree interface with drag/zoom
- Equipment screen with restriction indicators
- Combat log/damage numbers

**UI Components:**

1. **Character Selection Screen**
   - Class selection carousel
   - Race selection grid
   - Profession dropdown
   - Preview window (shows stats and starting skills)
   - Confirm/Back buttons

2. **Character Sheet**
   - Level progress bar
   - Attribute stats with tooltips
   - Health/Mana/Stamina bars
   - Active effects list
   - Equipment slots with restrictions highlighted

3. **Skill Tree UI**
   - Interactive node graph
   - Zoom in/out controls
   - Locked/unlocked visual states
   - Skill point counter
   - Hover tooltips with details
   - One-click skill unlock

4. **Combat Log**
   - Scrolling damage feed
   - Color-coded damage types
   - Critical hit animations
   - Effect application/expiration notifications

**UI Files (`.ui` format):**
```
src/main/Resources/Common/UI/
├── character_creation.ui
├── character_sheet.ui
├── skill_tree.ui
├── combat_log.ui
├── level_up_notification.ui
└── effect_indicator.ui
```

---

## 🚀 Implementation Phases

### Phase 1: Foundation (Week 1-2)
**Goal:** Set up core data structures and persistence

- [ ] Create package structure
- [ ] Implement `PlayerRPGData` model
- [ ] Build JSON serialization layer
- [ ] Create `RPGManager` facade
- [ ] Set up event system
- [ ] Write data loader for configs
- [ ] Create example JSON configs for 2 classes, 2 races, 1 profession

**Deliverables:**
- Working persistence (save/load player data)
- Config loader with validation
- Event framework for other systems

---

### Phase 2: Character System (Week 3)
**Goal:** Character creation and selection

- [ ] Implement `CharacterClass`, `Race`, `Profession` models
- [ ] Build `CharacterManager`
- [ ] Create character creation UI (`.ui` file)
- [ ] Implement character selection flow
- [ ] Apply racial/class bonuses
- [ ] Test character persistence

**Deliverables:**
- Working character creation screen
- 3 playable classes (Warrior, Mage, Rogue)
- 2 playable races (Human, Elf)
- 1 profession (Blacksmith)

---

### Phase 3: Progression System (Week 4)
**Goal:** Leveling and XP mechanics

- [ ] Implement `ExperienceCurve` strategies
- [ ] Build `ProgressionManager`
- [ ] Create level-up modifier system
- [ ] Implement XP gain from events (kills, quests, etc.)
- [ ] Fire `LevelUpEvent` with new stats
- [ ] Create level-up UI notification
- [ ] Test all XP curves (linear, exponential, polynomial)

**Deliverables:**
- Working leveling system
- Configurable XP curves
- Level-up stat increases
- UI notification on level-up

---

### Phase 4: Attribute System (Week 5)
**Goal:** Dynamic attribute calculation

- [ ] Implement `Attribute` and `AttributeModifier`
- [ ] Build `AttributeManager`
- [ ] Create modifier application logic (flat, %, multiplicative)
- [ ] Integrate attributes with health/mana/stamina calculations
- [ ] Create attribute UI display
- [ ] Test modifier stacking and priority

**Deliverables:**
- 6 core attributes (STR, DEX, INT, CON, WIS, CHA)
- Working modifier system
- Derived stat calculations
- Character sheet UI showing attributes

---

### Phase 5: Skill System (Week 6-7)
**Goal:** Skill trees and activation

- [ ] Implement `Skill`, `SkillNode`, `SkillTree`
- [ ] Build `SkillManager`
- [ ] Create skill cooldown tracker
- [ ] Implement permission integration
- [ ] Create skill tree UI (`.ui` file)
- [ ] Build skill activation logic
- [ ] Design 3-5 skills per class
- [ ] Test skill prerequisites and cooldowns

**Deliverables:**
- Working skill tree for each class
- Interactive skill tree UI
- Skill cooldown system
- 15+ unique skills

---

### Phase 6: Combat System (Week 8-9)
**Goal:** Damage calculation and effects

- [ ] Implement `Damage`, `DamageCalculator`
- [ ] Build `CombatEffect` and `EffectInstance`
- [ ] Create `CombatManager`
- [ ] Implement damage mitigation formulas
- [ ] Create DOT/HOT tick system
- [ ] Build effect stacking logic
- [ ] Integrate with Hytale's damage events
- [ ] Create combat log UI
- [ ] Design 10+ combat effects

**Deliverables:**
- Full damage calculation system
- 10+ status effects (Poison, Burn, Slow, Haste, Shield, etc.)
- Combat log UI with color-coded damage
- Effect indicators on HUD

---

### Phase 7: Restrictions (Week 10)
**Goal:** Equipment and inventory rules

- [ ] Implement `EquipmentRestriction`
- [ ] Build `RestrictionManager`
- [ ] Create equipment validation logic
- [ ] Implement dynamic inventory sizing
- [ ] Add UI indicators for restrictions
- [ ] Test with various item types

**Deliverables:**
- Working equipment restrictions
- Dynamic inventory sizes
- Clear UI feedback when restrictions apply

---

### Phase 8: Advanced Features (Week 11-12)
**Goal:** Polish and special features

- [ ] Implement custom race models with `RaceModelManager`
- [ ] Create respec system (reset skills for a cost)
- [ ] Build dual-class system (optional)
- [ ] Add prestige/rebirth mechanic (optional)
- [ ] Create admin commands for testing
- [ ] Performance optimization
- [ ] Balance testing

**Deliverables:**
- Custom race models
- Admin tools
- Optimized performance
- Balanced gameplay

---

### Phase 9: Testing & Documentation (Week 13)
**Goal:** Quality assurance

- [ ] Unit tests for all core systems
- [ ] Integration tests
- [ ] Load testing (100+ players)
- [ ] Write API documentation
- [ ] Create user guide
- [ ] Record demo video

**Deliverables:**
- Comprehensive test suite
- API documentation
- User guide
- Demo video

---

## 📁 File Structure

### Configuration Files
```
config/
├── rpg_config.json                     // Master config
├── classes/
│   ├── warrior.json
│   ├── mage.json
│   ├── rogue.json
│   └── cleric.json
├── races/
│   ├── human.json
│   ├── elf.json
│   ├── dwarf.json
│   └── orc.json
├── professions/
│   ├── blacksmith.json
│   ├── alchemist.json
│   └── enchanter.json
├── skills/
│   ├── warrior_skills.json
│   ├── mage_skills.json
│   └── shared_skills.json
├── effects/
│   └── combat_effects.json
├── experience_curves/
│   ├── standard_curve.json
│   ├── fast_curve.json
│   └── slow_curve.json
└── formulas/
    ├── damage_formulas.json
    └── stat_formulas.json
```

### Player Data
```
player_data/
└── <uuid>/
    ├── rpg_data.json                   // PlayerRPGData
    ├── skill_progress.json             // Unlocked skills
    └── combat_history.json             // Combat logs
```

### UI Files
```
src/main/Resources/Common/UI/RPG/
├── character_creation.ui
├── character_sheet.ui
├── skill_tree.ui
├── combat_log.ui
├── level_up_notification.ui
├── effect_indicator.ui
└── stat_allocation.ui
```

---

## 🎮 Configuration Format

### Master Config (`rpg_config.json`)
```json
{
  "enabled": true,
  "startingLevel": 1,
  "maxLevel": 100,
  "skillPointsPerLevel": 1,
  "allowRespec": true,
  "respecCost": 1000,
  "experienceCurve": "standard",
  "enableCustomRaceModels": true,
  "combatLogEnabled": true,
  "permissionIntegration": true,
  "autoSaveInterval": 300,
  "debug": false
}
```

### Class Config Example (`warrior.json`)
```json
{
  "id": "warrior",
  "displayName": "Warrior",
  "description": "A stalwart defender and master of melee combat.",
  "iconPath": "textures/class_icons/warrior.png",
  "baseAttributes": {
    "STRENGTH": 15,
    "DEXTERITY": 10,
    "INTELLIGENCE": 5,
    "CONSTITUTION": 12,
    "WISDOM": 7,
    "CHARISMA": 6
  },
  "attributeGrowth": {
    "STRENGTH": 2.0,
    "CONSTITUTION": 1.5,
    "DEXTERITY": 0.5,
    "WISDOM": 0.3
  },
  "startingSkills": [
    "power_strike",
    "shield_bash"
  ],
  "baseHealth": 120,
  "baseMana": 50,
  "baseStamina": 100,
  "healthPerLevel": 10,
  "manaPerLevel": 2,
  "staminaPerLevel": 5,
  "healthRegen": 0.5,
  "manaRegen": 0.2,
  "staminaRegen": 1.0,
  "equipmentRules": {
    "allowedWeaponTypes": [
      "SWORD",
      "AXE",
      "MACE",
      "SPEAR",
      "SHIELD"
    ],
    "allowedArmorTypes": [
      "HEAVY",
      "MEDIUM"
    ],
    "minStrengthForHeavyArmor": 12
  },
  "modifiers": {
    "physicalDamage": 1.2,
    "magicalDamage": 0.8,
    "criticalChance": 0.05,
    "criticalDamage": 1.5
  }
}
```

### Race Config Example (`elf.json`)
```json
{
  "id": "elf",
  "displayName": "Elf",
  "description": "Ancient and wise, elves excel at magic and archery.",
  "iconPath": "textures/race_icons/elf.png",
  "racialBonuses": {
    "DEXTERITY": 3,
    "INTELLIGENCE": 2,
    "WISDOM": 2,
    "STRENGTH": -1
  },
  "racialAbilities": [
    "keen_senses",
    "natural_magic"
  ],
  "heightScale": 1.05,
  "speedModifier": 1.05,
  "modelPath": "models/races/elf.json",
  "resistances": {
    "NATURE": 0.15,
    "ARCANE": 0.10,
    "PHYSICAL": -0.05
  },
  "passiveEffects": [
    {
      "effectId": "elven_grace",
      "permanent": true
    }
  ]
}
```

### Skill Config Example (`fireball.json`)
```json
{
  "id": "fireball",
  "displayName": "Fireball",
  "description": "Hurl a blazing ball of fire at your foes, dealing area damage.",
  "iconPath": "textures/skill_icons/fireball.png",
  "type": "ACTIVE",
  "requiredLevel": 5,
  "prerequisiteSkills": [
    "fire_mastery_1"
  ],
  "skillPointCost": 1,
  "cooldownMillis": 5000,
  "manaCost": 25,
  "staminaCost": 0,
  "requiredPermissions": [
    "rpg.magic.fire"
  ],
  "castTime": 1000,
  "range": 30,
  "targetType": "ENEMY",
  "effectData": {
    "baseDamage": 50,
    "damageType": "FIRE",
    "aoeRadius": 3,
    "applyEffect": {
      "effectId": "burn",
      "duration": 5000,
      "chance": 0.5
    },
    "particleEffect": "fire_burst",
    "soundEffect": "spell_fire_cast"
  },
  "scalingAttributes": {
    "INTELLIGENCE": 0.8
  }
}
```

### Combat Effect Config (`burn.json`)
```json
{
  "id": "burn",
  "displayName": "Burning",
  "description": "Taking fire damage over time.",
  "iconPath": "textures/effect_icons/burn.png",
  "type": "DEBUFF",
  "effectCategory": "DOT",
  "duration": 5000,
  "tickInterval": 1000,
  "damagePerTick": 5,
  "damageType": "FIRE",
  "stackable": true,
  "maxStacks": 3,
  "particleEffect": "flame_particle",
  "removeOnDeath": true,
  "canDispel": true,
  "dispelType": "CURSE"
}
```

### Experience Curve Config (`standard_curve.json`)
```json
{
  "curveType": "POLYNOMIAL",
  "formula": "level^2.5 * 100 + level * 50",
  "coefficients": {
    "a": 100,
    "power": 2.5,
    "b": 50
  },
  "levelBreakpoints": [
    { "level": 1, "xpRequired": 0 },
    { "level": 10, "xpRequired": 5000 },
    { "level": 50, "xpRequired": 250000 },
    { "level": 100, "xpRequired": 2000000 }
  ]
}
```

---

## 🔌 API Reference

### RPGManager API
```java
public class RPGManager {
    // Singleton instance
    public static RPGManager getInstance();
    
    // Player data access
    public PlayerRPGData getPlayerData(UUID playerId);
    public void savePlayerData(UUID playerId);
    
    // Character management
    public boolean createCharacter(UUID playerId, String classId, String raceId, String professionId);
    public boolean canSelectClass(UUID playerId, String classId);
    
    // Progression
    public void addExperience(UUID playerId, long amount);
    public void setLevel(UUID playerId, int level);
    public int getSkillPoints(UUID playerId);
    
    // Skills
    public boolean unlockSkill(UUID playerId, String skillId);
    public boolean canUseSkill(UUID playerId, String skillId);
    public void activateSkill(UUID playerId, String skillId, Entity target);
    
    // Combat
    public void dealDamage(UUID attackerId, UUID targetId, Damage damage);
    public void applyEffect(UUID targetId, String effectId, int duration);
    
    // Attributes
    public int getAttribute(UUID playerId, AttributeType type);
    public void addAttributeModifier(UUID playerId, AttributeModifier modifier);
    
    // Restrictions
    public boolean canEquipItem(UUID playerId, ItemStack item);
    public int getInventorySize(UUID playerId);
}
```

### Events API
```java
// Listen for RPG events
@EventListener
public void onLevelUp(LevelUpEvent event) {
    UUID playerId = event.getPlayerId();
    int newLevel = event.getNewLevel();
    Map<String, Double> newStats = event.getNewStats();
    
    // Custom logic here
}

@EventListener
public void onSkillUnlock(SkillUnlockEvent event) {
    UUID playerId = event.getPlayerId();
    String skillId = event.getSkillId();
    
    // Grant permission node
    player.grantPermission("rpg.skill." + skillId);
}

@EventListener
public void onCombatDamage(CombatEvent event) {
    Damage damage = event.getDamage();
    // Custom damage logging or modification
}
```

---

## 🎯 Success Criteria

### Functional Requirements
- ✅ Player can select class, race, and profession
- ✅ Experience is gained and levels are awarded
- ✅ Skills can be unlocked in a tree structure
- ✅ Combat effects apply and tick correctly
- ✅ Equipment restrictions are enforced
- ✅ All data persists across server restarts

### Performance Requirements
- ✅ No frame drops during combat with 10+ active effects
- ✅ Skill tree UI loads in <500ms
- ✅ Data save/load completes in <100ms per player
- ✅ Supports 100+ concurrent players without lag

### UX Requirements
- ✅ Intuitive character creation flow
- ✅ Clear visual feedback for level-ups
- ✅ Tooltips explain all stats and skills
- ✅ Error messages are helpful and actionable

---

## 📝 Notes & Considerations

### Balance Philosophy
- Early levels should feel rewarding (frequent level-ups)
- Mid-game should introduce meaningful choices (skill paths)
- End-game should require mastery and optimization
- No class should be strictly better than others

### Extensibility Hooks
- Custom damage types can be added via config
- New attributes can be registered programmatically
- Skill effects support arbitrary data fields
- Event system allows plugins to integrate

### Future Enhancements
- **Dual-Spec System**: Switch between two skill builds
- **Prestige/Rebirth**: Reset level for permanent bonuses
- **Skill Combos**: Chain skills for bonus effects
- **Dynamic Difficulty**: Scale enemies based on player level
- **Guild System**: Shared skill buffs for guild members
- **Achievement System**: Unlock cosmetics and titles

---

## 🚦 Getting Started

### For Developers
1. Review this design document
2. Set up your development environment (Java 21, Gradle)
3. Start with Phase 1: Foundation
4. Use the provided package structure
5. Write unit tests as you go
6. Follow the configuration format specifications

### For Server Admins
1. Configure `rpg_config.json` to your liking
2. Customize classes, races, and professions
3. Adjust XP curves for your server pace
4. Test with a small group before full launch
5. Monitor performance and adjust formulas as needed

---

**End of Design Document**

*This document is a living specification and will be updated as implementation progresses.*
