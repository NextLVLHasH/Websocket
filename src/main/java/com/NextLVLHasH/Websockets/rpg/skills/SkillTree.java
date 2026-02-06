package com.NextLVLHasH.Websockets.rpg.skills;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a complete skill tree in the RPG system.
 * <p>
 * A SkillTree contains all the {@link SkillNode}s that form a progression tree
 * for a particular class or specialization. It manages the relationships between
 * nodes and provides methods for querying tree structure and determining which
 * skills are available to unlock.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SkillTree warriorTree = SkillTree.builder()
 *     .id("warrior_combat")
 *     .displayName("Combat Mastery")
 *     .associatedClass("warrior")
 *     .rootSkillId("basic_attack")
 *     .node(basicAttackNode)
 *     .node(powerStrikeNode)
 *     .node(whirlwindNode)
 *     .build();
 *
 * List<SkillNode> unlockable = warriorTree.getUnlockableNodes(playerSkills, level, points);
 * }</pre>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class SkillTree {

    // ==================== Identity ====================

    /**
     * Unique identifier for this skill tree (e.g., "warrior_combat", "mage_fire").
     */
    @SerializedName("id")
    private final String id;

    /**
     * Human-readable display name (e.g., "Combat Mastery", "Fire Magic").
     */
    @SerializedName("display_name")
    private final String displayName;

    /**
     * The character class this tree is associated with (e.g., "warrior", "mage").
     */
    @SerializedName("associated_class")
    private final String associatedClass;

    /**
     * Description of this skill tree.
     */
    @SerializedName("description")
    private final String description;

    /**
     * Path to the skill tree icon for UI display.
     */
    @SerializedName("icon_path")
    private final String iconPath;

    // ==================== Structure ====================

    /**
     * Map of skill ID to SkillNode for all nodes in this tree.
     */
    @SerializedName("nodes")
    private final Map<String, SkillNode> nodes;

    /**
     * The skill ID of the root node (entry point to the tree).
     */
    @SerializedName("root_skill_id")
    private final String rootSkillId;

    // ==================== Cached Data ====================

    /**
     * Cached tiers map for quick lookup (not serialized).
     */
    private transient Map<Integer, List<SkillNode>> tierCache;

    /**
     * Maximum tier in this tree (cached, not serialized).
     */
    private transient int maxTier = -1;

    // ==================== GSON Configuration ====================

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // ==================== Constructor ====================

    /**
     * Private constructor - use Builder or fromJson() to create instances.
     */
    private SkillTree(
            String id,
            String displayName,
            String associatedClass,
            String description,
            String iconPath,
            Map<String, SkillNode> nodes,
            String rootSkillId) {

        this.id = Objects.requireNonNull(id, "SkillTree id cannot be null");
        this.displayName = displayName != null ? displayName : id;
        this.associatedClass = associatedClass != null ? associatedClass : "";
        this.description = description != null ? description : "";
        this.iconPath = iconPath != null ? iconPath : "";
        this.nodes = nodes != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(nodes))
                : Collections.emptyMap();
        this.rootSkillId = rootSkillId != null ? rootSkillId : "";
    }

    // ==================== Getters ====================

    /**
     * Gets the unique identifier of this skill tree.
     *
     * @return the tree ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the associated character class.
     *
     * @return the class ID
     */
    public String getAssociatedClass() {
        return associatedClass;
    }

    /**
     * Gets the description of this skill tree.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the icon path for UI display.
     *
     * @return the icon path
     */
    public String getIconPath() {
        return iconPath;
    }

    /**
     * Gets the map of all nodes in this tree.
     *
     * @return unmodifiable map of skill ID to SkillNode
     */
    public Map<String, SkillNode> getNodes() {
        return nodes;
    }

    /**
     * Gets all skill nodes in this tree as a collection.
     * @return collection of all SkillNodes
     */
    public Collection<SkillNode> getAllNodes() {
        return nodes.values();
    }

    /**
     * Gets the root skill ID.
     *
     * @return the root skill ID
     */
    public String getRootSkillId() {
        return rootSkillId;
    }

    /**
     * Gets the total number of nodes in this tree.
     *
     * @return the node count
     */
    public int getNodeCount() {
        return nodes.size();
    }

    // ==================== Node Access ====================

    /**
     * Gets a specific node by skill ID.
     *
     * @param skillId the skill ID
     * @return the SkillNode, or null if not found
     */
    public SkillNode getNode(String skillId) {
        return nodes.get(skillId);
    }

    /**
     * Gets a specific node by skill ID, throwing if not found.
     *
     * @param skillId the skill ID
     * @return the SkillNode
     * @throws NoSuchElementException if node not found
     */
    public SkillNode getNodeOrThrow(String skillId) {
        SkillNode node = nodes.get(skillId);
        if (node == null) {
            throw new NoSuchElementException("No node found with skill ID: " + skillId);
        }
        return node;
    }

    /**
     * Checks if a node exists in this tree.
     *
     * @param skillId the skill ID
     * @return true if the node exists
     */
    public boolean hasNode(String skillId) {
        return nodes.containsKey(skillId);
    }

    /**
     * Gets the root node of this tree.
     *
     * @return the root SkillNode, or null if not found
     */
    public SkillNode getRootNode() {
        return nodes.get(rootSkillId);
    }

    /**
     * Gets all skill IDs in this tree.
     *
     * @return unmodifiable set of skill IDs
     */
    public Set<String> getAllSkillIds() {
        return nodes.keySet();
    }

    /**
     * Gets all skills in this tree.
     *
     * @return list of all Skill objects
     */
    public List<Skill> getAllSkills() {
        return nodes.values().stream()
                .map(SkillNode::getSkill)
                .collect(Collectors.toList());
    }

    // ==================== Tier Access ====================

    /**
     * Gets all nodes at a specific tier.
     *
     * @param tier the tier number (0 = root)
     * @return list of nodes at that tier
     */
    public List<SkillNode> getTier(int tier) {
        buildTierCache();
        return tierCache.getOrDefault(tier, Collections.emptyList());
    }

    /**
     * Gets the maximum tier in this tree.
     *
     * @return the highest tier number
     */
    public int getMaxTier() {
        buildTierCache();
        return maxTier;
    }

    /**
     * Gets all tier numbers in this tree.
     *
     * @return sorted list of tier numbers
     */
    public List<Integer> getAllTiers() {
        buildTierCache();
        return new ArrayList<>(tierCache.keySet());
    }

    /**
     * Builds the tier cache if not already built.
     */
    private void buildTierCache() {
        if (tierCache != null) {
            return;
        }

        tierCache = new TreeMap<>();
        maxTier = 0;

        for (SkillNode node : nodes.values()) {
            int tier = node.getTier();
            tierCache.computeIfAbsent(tier, k -> new ArrayList<>()).add(node);
            if (tier > maxTier) {
                maxTier = tier;
            }
        }

        // Sort nodes within each tier by position
        for (List<SkillNode> tierNodes : tierCache.values()) {
            tierNodes.sort(Comparator.comparingInt(SkillNode::getPosition));
        }
    }

    // ==================== Unlock Queries ====================

    /**
     * Gets all nodes that a player can currently unlock.
     *
     * @param unlockedSkills set of skill IDs the player has unlocked
     * @param playerLevel the player's current level
     * @param availablePoints the player's available skill points
     * @return list of unlockable nodes
     */
    public List<SkillNode> getUnlockableNodes(Set<String> unlockedSkills, int playerLevel, int availablePoints) {
        List<SkillNode> unlockable = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (node.canUnlock(unlockedSkills, playerLevel, availablePoints)) {
                unlockable.add(node);
            }
        }
        return unlockable;
    }

    /**
     * Gets all nodes that a player could unlock if they had enough points.
     *
     * @param unlockedSkills set of skill IDs the player has unlocked
     * @param playerLevel the player's current level
     * @return list of potentially unlockable nodes
     */
    public List<SkillNode> getPotentiallyUnlockableNodes(Set<String> unlockedSkills, int playerLevel) {
        List<SkillNode> unlockable = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (node.canUnlockIgnoringPoints(unlockedSkills, playerLevel)) {
                unlockable.add(node);
            }
        }
        return unlockable;
    }

    /**
     * Gets all nodes that a player has unlocked in this tree.
     *
     * @param unlockedSkills set of all skill IDs the player has unlocked
     * @return list of unlocked nodes in this tree
     */
    public List<SkillNode> getUnlockedNodes(Set<String> unlockedSkills) {
        List<SkillNode> unlocked = new ArrayList<>();
        for (SkillNode node : nodes.values()) {
            if (unlockedSkills.contains(node.getSkillId())) {
                unlocked.add(node);
            }
        }
        return unlocked;
    }

    /**
     * Gets the count of unlocked skills in this tree.
     *
     * @param unlockedSkills set of all skill IDs the player has unlocked
     * @return count of unlocked skills
     */
    public int getUnlockedCount(Set<String> unlockedSkills) {
        int count = 0;
        for (String skillId : nodes.keySet()) {
            if (unlockedSkills.contains(skillId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the total skill points spent in this tree.
     *
     * @param unlockedSkills set of all skill IDs the player has unlocked
     * @return total points spent
     */
    public int getPointsSpent(Set<String> unlockedSkills) {
        int total = 0;
        for (SkillNode node : nodes.values()) {
            if (unlockedSkills.contains(node.getSkillId())) {
                total += node.getSkill().getSkillPointCost();
            }
        }
        return total;
    }

    /**
     * Gets the completion percentage of this tree.
     *
     * @param unlockedSkills set of all skill IDs the player has unlocked
     * @return completion percentage (0.0 to 1.0)
     */
    public double getCompletionPercentage(Set<String> unlockedSkills) {
        if (nodes.isEmpty()) {
            return 1.0;
        }
        return (double) getUnlockedCount(unlockedSkills) / nodes.size();
    }

    // ==================== Tree Navigation ====================

    /**
     * Gets the child nodes of a specific node.
     *
     * @param skillId the skill ID
     * @return list of child nodes
     */
    public List<SkillNode> getChildNodes(String skillId) {
        SkillNode node = nodes.get(skillId);
        if (node == null) {
            return Collections.emptyList();
        }
        List<SkillNode> children = new ArrayList<>();
        for (String childId : node.getChildNodes()) {
            SkillNode child = nodes.get(childId);
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    /**
     * Gets the parent nodes of a specific node.
     *
     * @param skillId the skill ID
     * @return list of parent nodes
     */
    public List<SkillNode> getParentNodes(String skillId) {
        SkillNode node = nodes.get(skillId);
        if (node == null) {
            return Collections.emptyList();
        }
        List<SkillNode> parents = new ArrayList<>();
        for (String parentId : node.getParentNodes()) {
            SkillNode parent = nodes.get(parentId);
            if (parent != null) {
                parents.add(parent);
            }
        }
        return parents;
    }

    /**
     * Gets all leaf nodes (nodes with no children).
     *
     * @return list of leaf nodes
     */
    public List<SkillNode> getLeafNodes() {
        return nodes.values().stream()
                .filter(SkillNode::isLeaf)
                .collect(Collectors.toList());
    }

    /**
     * Gets all root nodes (nodes with no parents).
     *
     * @return list of root nodes
     */
    public List<SkillNode> getRootNodes() {
        return nodes.values().stream()
                .filter(SkillNode::isRoot)
                .collect(Collectors.toList());
    }

    // ==================== Validation ====================

    /**
     * Validates the integrity of this skill tree.
     *
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        // Check root exists
        if (rootSkillId != null && !rootSkillId.isEmpty() && !nodes.containsKey(rootSkillId)) {
            errors.add("Root skill ID '" + rootSkillId + "' not found in nodes");
        }

        // Check all parent/child references are valid
        for (SkillNode node : nodes.values()) {
            for (String parentId : node.getParentNodes()) {
                if (!nodes.containsKey(parentId)) {
                    errors.add("Node '" + node.getSkillId() + "' references missing parent '" + parentId + "'");
                }
            }
            for (String childId : node.getChildNodes()) {
                if (!nodes.containsKey(childId)) {
                    errors.add("Node '" + node.getSkillId() + "' references missing child '" + childId + "'");
                }
            }
        }

        // Check for bidirectional consistency
        for (SkillNode node : nodes.values()) {
            for (String childId : node.getChildNodes()) {
                SkillNode child = nodes.get(childId);
                if (child != null && !child.getParentNodes().contains(node.getSkillId())) {
                    errors.add("Node '" + node.getSkillId() + "' has child '" + childId + 
                            "' but child doesn't list it as parent");
                }
            }
        }

        return errors;
    }

    /**
     * Checks if this skill tree is valid.
     *
     * @return true if the tree is valid
     */
    public boolean isValid() {
        return validate().isEmpty();
    }

    // ==================== JSON Serialization ====================

    /**
     * Loads a SkillTree from a JSON file.
     *
     * @param path the path to the JSON file
     * @return the loaded SkillTree
     * @throws IOException if the file cannot be read
     */
    public static SkillTree fromJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return fromJson(reader);
        }
    }

    /**
     * Loads a SkillTree from a JSON reader.
     *
     * @param reader the JSON reader
     * @return the loaded SkillTree
     */
    public static SkillTree fromJson(Reader reader) {
        JsonSkillTree json = GSON.fromJson(reader, JsonSkillTree.class);
        if (json == null) {
            return null;
        }
        return json.toSkillTree();
    }

    /**
     * Loads a SkillTree from a JSON string.
     *
     * @param json the JSON string
     * @return the loaded SkillTree
     */
    public static SkillTree fromJsonString(String json) {
        JsonSkillTree jsonTree = GSON.fromJson(json, JsonSkillTree.class);
        return jsonTree.toSkillTree();
    }

    /**
     * Converts this skill tree to a JSON string.
     *
     * @return the JSON representation
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Loads multiple skill trees from a JSON file containing an array.
     *
     * @param path the path to the JSON file
     * @return list of loaded skill trees
     * @throws IOException if the file cannot be read
     */
    public static List<SkillTree> loadTreesFromJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            Type listType = new TypeToken<List<JsonSkillTree>>(){}.getType();
            List<JsonSkillTree> jsonTrees = GSON.fromJson(reader, listType);
            List<SkillTree> trees = new ArrayList<>();
            for (JsonSkillTree json : jsonTrees) {
                trees.add(json.toSkillTree());
            }
            return trees;
        }
    }

    // ==================== JSON Helper Class ====================

    /**
     * Internal class for JSON deserialization.
     */
    private static class JsonSkillTree {
        String id;
        @SerializedName("display_name")
        String displayName;
        @SerializedName("associated_class")
        String associatedClass;
        String description;
        @SerializedName("icon_path")
        String iconPath;
        @SerializedName("root_skill_id")
        String rootSkillId;
        // JSON format uses object with skill IDs as keys, not an array
        Map<String, JsonSkillNode> nodes;

        SkillTree toSkillTree() {
            Map<String, SkillNode> nodeMap = new LinkedHashMap<>();
            if (nodes != null) {
                for (Map.Entry<String, JsonSkillNode> entry : nodes.entrySet()) {
                    JsonSkillNode jsonNode = entry.getValue();
                    SkillNode node = jsonNode.toSkillNode();
                    nodeMap.put(node.getSkillId(), node);
                }
            }
            return new SkillTree(id, displayName, associatedClass, description, iconPath, nodeMap, rootSkillId);
        }
    }

    /**
     * Internal class for JSON node deserialization.
     */
    private static class JsonSkillNode {
        JsonSkill skill;
        int tier;
        int position;
        int x;
        int y;
        @SerializedName("child_nodes")
        List<String> childNodes;
        @SerializedName("parent_nodes")
        List<String> parentNodes;

        SkillNode toSkillNode() {
            return new SkillNode(
                    skill.toSkill(),
                    tier,
                    position,
                    x,
                    y,
                    childNodes,
                    parentNodes
            );
        }
    }

    /**
     * Internal class for JSON skill deserialization within tree.
     */
    private static class JsonSkill {
        String id;
        @SerializedName("display_name")
        String displayName;
        String description;
        @SerializedName("icon_path")
        String iconPath;
        String type;
        @SerializedName("target_type")
        String targetType;
        @SerializedName("required_level")
        int requiredLevel = 1;
        @SerializedName("prerequisite_skills")
        List<String> prerequisiteSkills;
        @SerializedName("skill_point_cost")
        int skillPointCost = 1;
        @SerializedName("required_permissions")
        List<String> requiredPermissions;
        @SerializedName("cooldown_millis")
        long cooldownMillis;
        @SerializedName("mana_cost")
        double manaCost;
        @SerializedName("stamina_cost")
        double staminaCost;
        @SerializedName("cast_time_millis")
        int castTimeMillis;
        double range;
        @SerializedName("effect_data")
        Map<String, Object> effectData;
        @SerializedName("scaling_attributes")
        Map<String, Double> scalingAttributes;

        Skill toSkill() {
            return Skill.builder()
                    .id(id)
                    .displayName(displayName)
                    .description(description)
                    .iconPath(iconPath)
                    .type(SkillType.fromName(type))
                    .targetType(TargetType.fromName(targetType))
                    .requiredLevel(requiredLevel)
                    .prerequisiteSkills(prerequisiteSkills)
                    .skillPointCost(skillPointCost)
                    .requiredPermissions(requiredPermissions)
                    .cooldownMillis(cooldownMillis)
                    .manaCost(manaCost)
                    .staminaCost(staminaCost)
                    .castTimeMillis(castTimeMillis)
                    .range(range)
                    .effectData(effectData)
                    .build();
        }
    }

    // ==================== Builder ====================

    /**
     * Creates a new Builder for constructing SkillTrees.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing SkillTree instances.
     */
    public static class Builder {
        private String id;
        private String displayName;
        private String associatedClass;
        private String description;
        private String iconPath;
        private String rootSkillId;
        private final Map<String, SkillNode> nodes = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Sets the unique identifier for this skill tree.
         *
         * @param id the tree ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the display name for this skill tree.
         *
         * @param displayName the display name
         * @return this builder
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        /**
         * Sets the associated character class.
         *
         * @param associatedClass the class ID
         * @return this builder
         */
        public Builder associatedClass(String associatedClass) {
            this.associatedClass = associatedClass;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the icon path for UI display.
         *
         * @param iconPath the icon path
         * @return this builder
         */
        public Builder iconPath(String iconPath) {
            this.iconPath = iconPath;
            return this;
        }

        /**
         * Sets the root skill ID.
         *
         * @param rootSkillId the root skill ID
         * @return this builder
         */
        public Builder rootSkillId(String rootSkillId) {
            this.rootSkillId = rootSkillId;
            return this;
        }

        /**
         * Adds a node to the tree.
         *
         * @param node the skill node
         * @return this builder
         */
        public Builder node(SkillNode node) {
            this.nodes.put(node.getSkillId(), node);
            return this;
        }

        /**
         * Adds multiple nodes to the tree.
         *
         * @param nodes the skill nodes
         * @return this builder
         */
        public Builder nodes(Collection<SkillNode> nodes) {
            for (SkillNode node : nodes) {
                this.nodes.put(node.getSkillId(), node);
            }
            return this;
        }

        /**
         * Sets all nodes from a map.
         *
         * @param nodeMap map of skill ID to node
         * @return this builder
         */
        public Builder nodes(Map<String, SkillNode> nodeMap) {
            this.nodes.clear();
            if (nodeMap != null) {
                this.nodes.putAll(nodeMap);
            }
            return this;
        }

        /**
         * Builds the SkillTree instance.
         *
         * @return the constructed SkillTree
         * @throws IllegalStateException if required fields are missing
         */
        public SkillTree build() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("SkillTree id is required");
            }
            return new SkillTree(id, displayName, associatedClass, description, iconPath, nodes, rootSkillId);
        }
    }

    // ==================== Object Methods ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillTree skillTree = (SkillTree) o;
        return Objects.equals(id, skillTree.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SkillTree{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", associatedClass='" + associatedClass + '\'' +
                ", nodeCount=" + nodes.size() +
                ", maxTier=" + getMaxTier() +
                '}';
    }
}
