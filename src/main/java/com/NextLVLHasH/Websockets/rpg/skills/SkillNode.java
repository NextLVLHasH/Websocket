package com.NextLVLHasH.Websockets.rpg.skills;

import com.google.gson.annotations.SerializedName;

import java.util.*;

/**
 * Represents a node in a skill tree.
 * <p>
 * A SkillNode wraps a {@link Skill} and provides positioning and connectivity
 * information for displaying skills in a tree structure. Nodes track their
 * tier (depth in tree), position within that tier, and connections to
 * parent and child nodes.
 * </p>
 *
 * <p>Example skill tree structure:</p>
 * <pre>
 *         [Root]           Tier 0
 *        /      \
 *    [Skill1] [Skill2]     Tier 1
 *    /    \       |
 * [Skill3][Skill4][Skill5] Tier 2
 * </pre>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class SkillNode {

    // ==================== Identity ====================

    /**
     * The skill contained in this node.
     */
    @SerializedName("skill")
    private final Skill skill;

    // ==================== Tree Position ====================

    /**
     * The tier (row/depth) of this node in the skill tree.
     * Tier 0 is the root, higher tiers are further down the tree.
     */
    @SerializedName("tier")
    private final int tier;

    /**
     * The position of this node within its tier (0-indexed from left).
     */
    @SerializedName("position")
    private final int position;

    /**
     * The X coordinate for UI rendering.
     */
    @SerializedName("x")
    private final int x;

    /**
     * The Y coordinate for UI rendering.
     */
    @SerializedName("y")
    private final int y;

    // ==================== Connections ====================

    /**
     * List of skill IDs for child nodes (nodes that require this node).
     */
    @SerializedName("child_nodes")
    private final List<String> childNodes;

    /**
     * List of skill IDs for parent nodes (prerequisites for this node).
     */
    @SerializedName("parent_nodes")
    private final List<String> parentNodes;

    // ==================== Constructor ====================

    /**
     * Constructs a SkillNode with all properties.
     *
     * @param skill the skill for this node
     * @param tier the tier in the skill tree
     * @param position the position within the tier
     * @param x the X coordinate for UI
     * @param y the Y coordinate for UI
     * @param childNodes list of child node skill IDs
     * @param parentNodes list of parent node skill IDs
     */
    public SkillNode(
            Skill skill,
            int tier,
            int position,
            int x,
            int y,
            List<String> childNodes,
            List<String> parentNodes) {

        this.skill = Objects.requireNonNull(skill, "Skill cannot be null");
        this.tier = Math.max(0, tier);
        this.position = Math.max(0, position);
        this.x = x;
        this.y = y;
        this.childNodes = childNodes != null
                ? Collections.unmodifiableList(new ArrayList<>(childNodes))
                : Collections.emptyList();
        this.parentNodes = parentNodes != null
                ? Collections.unmodifiableList(new ArrayList<>(parentNodes))
                : Collections.emptyList();
    }

    /**
     * Simplified constructor for basic positioning.
     *
     * @param skill the skill for this node
     * @param tier the tier in the skill tree
     * @param position the position within the tier
     */
    public SkillNode(Skill skill, int tier, int position) {
        this(skill, tier, position, position * 100, tier * 80, null, null);
    }

    // ==================== Getters ====================

    /**
     * Gets the skill contained in this node.
     *
     * @return the skill
     */
    public Skill getSkill() {
        return skill;
    }

    /**
     * Gets the skill ID (convenience method).
     *
     * @return the skill ID
     */
    public String getSkillId() {
        return skill.getId();
    }

    /**
     * Gets the skill ID (convenience method).
     * @return the skill ID
     */
    public String getId() {
        return skill.getId();
    }

    /**
     * Gets the skill display name (convenience method).
     * @return the display name
     */
    public String getName() {
        return skill.getDisplayName();
    }

    /**
     * Gets the required level to unlock this skill (convenience method).
     * @return the required level
     */
    public int getRequiredLevel() {
        return skill.getRequiredLevel();
    }

    /**
     * Gets the tier (depth) of this node in the tree.
     *
     * @return the tier (0 = root)
     */
    public int getTier() {
        return tier;
    }

    /**
     * Gets the position of this node within its tier.
     *
     * @return the position (0-indexed)
     */
    public int getPosition() {
        return position;
    }

    /**
     * Gets the X coordinate for UI rendering.
     *
     * @return the X coordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the Y coordinate for UI rendering.
     *
     * @return the Y coordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the list of child node skill IDs.
     *
     * @return unmodifiable list of child skill IDs
     */
    public List<String> getChildNodes() {
        return childNodes;
    }

    /**
     * Gets the list of parent node skill IDs.
     *
     * @return unmodifiable list of parent skill IDs
     */
    public List<String> getParentNodes() {
        return parentNodes;
    }

    // ==================== Tree Navigation ====================

    /**
     * Checks if this node is a root node (has no parents).
     *
     * @return true if this is a root node
     */
    public boolean isRoot() {
        return parentNodes.isEmpty();
    }

    /**
     * Checks if this node is a leaf node (has no children).
     *
     * @return true if this is a leaf node
     */
    public boolean isLeaf() {
        return childNodes.isEmpty();
    }

    /**
     * Gets the number of child nodes.
     *
     * @return the number of children
     */
    public int getChildCount() {
        return childNodes.size();
    }

    /**
     * Gets the number of parent nodes.
     *
     * @return the number of parents
     */
    public int getParentCount() {
        return parentNodes.size();
    }

    /**
     * Checks if a specific skill is a parent of this node.
     *
     * @param skillId the skill ID to check
     * @return true if the skill is a parent
     */
    public boolean hasParent(String skillId) {
        return parentNodes.contains(skillId);
    }

    /**
     * Checks if a specific skill is a child of this node.
     *
     * @param skillId the skill ID to check
     * @return true if the skill is a child
     */
    public boolean hasChild(String skillId) {
        return childNodes.contains(skillId);
    }

    // ==================== Unlock Logic ====================

    /**
     * Checks if this skill node can be unlocked by a player.
     * <p>
     * A skill can be unlocked if:
     * <ul>
     *   <li>All parent nodes (prerequisites from tree) are unlocked</li>
     *   <li>All prerequisite skills (from skill definition) are unlocked</li>
     *   <li>The player meets the level requirement</li>
     *   <li>The player has enough skill points</li>
     * </ul>
     * </p>
     *
     * @param unlockedSkills set of skill IDs the player has unlocked
     * @param playerLevel the player's current level
     * @param availablePoints the player's available skill points
     * @return true if all unlock conditions are met
     */
    public boolean canUnlock(Set<String> unlockedSkills, int playerLevel, int availablePoints) {
        // Already unlocked
        if (unlockedSkills.contains(skill.getId())) {
            return false;
        }

        // Check parent nodes in tree
        if (!parentNodes.isEmpty()) {
            for (String parentId : parentNodes) {
                if (!unlockedSkills.contains(parentId)) {
                    return false;
                }
            }
        }

        // Check skill prerequisites (may differ from tree parents)
        if (!skill.meetsPrerequisites(unlockedSkills)) {
            return false;
        }

        // Check level requirement
        if (!skill.meetsLevelRequirement(playerLevel)) {
            return false;
        }

        // Check skill points
        if (!skill.hasEnoughPoints(availablePoints)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if this skill node can be unlocked, without checking skill points.
     * <p>
     * Useful for showing "affordable if you had points" state in UI.
     * </p>
     *
     * @param unlockedSkills set of skill IDs the player has unlocked
     * @param playerLevel the player's current level
     * @return true if prerequisites and level are met
     */
    public boolean canUnlockIgnoringPoints(Set<String> unlockedSkills, int playerLevel) {
        // Already unlocked
        if (unlockedSkills.contains(skill.getId())) {
            return false;
        }

        // Check parent nodes in tree
        for (String parentId : parentNodes) {
            if (!unlockedSkills.contains(parentId)) {
                return false;
            }
        }

        // Check skill prerequisites
        if (!skill.meetsPrerequisites(unlockedSkills)) {
            return false;
        }

        // Check level requirement
        return skill.meetsLevelRequirement(playerLevel);
    }

    /**
     * Gets the reasons why this skill cannot be unlocked.
     *
     * @param unlockedSkills set of skill IDs the player has unlocked
     * @param playerLevel the player's current level
     * @param availablePoints the player's available skill points
     * @return list of reasons (empty if can unlock)
     */
    public List<String> getUnlockBlockers(Set<String> unlockedSkills, int playerLevel, int availablePoints) {
        List<String> blockers = new ArrayList<>();

        if (unlockedSkills.contains(skill.getId())) {
            blockers.add("Already unlocked");
            return blockers;
        }

        // Check parent nodes
        for (String parentId : parentNodes) {
            if (!unlockedSkills.contains(parentId)) {
                blockers.add("Requires: " + parentId);
            }
        }

        // Check skill prerequisites not covered by parents
        for (String prereq : skill.getPrerequisiteSkills()) {
            if (!unlockedSkills.contains(prereq) && !parentNodes.contains(prereq)) {
                blockers.add("Requires skill: " + prereq);
            }
        }

        // Check level
        if (!skill.meetsLevelRequirement(playerLevel)) {
            blockers.add("Requires level " + skill.getRequiredLevel() + " (current: " + playerLevel + ")");
        }

        // Check points
        if (!skill.hasEnoughPoints(availablePoints)) {
            blockers.add("Requires " + skill.getSkillPointCost() + " skill points (available: " + availablePoints + ")");
        }

        return blockers;
    }

    // ==================== Builder ====================

    /**
     * Creates a new Builder for constructing SkillNodes.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing SkillNode instances.
     */
    public static class Builder {
        private Skill skill;
        private int tier = 0;
        private int position = 0;
        private int x = 0;
        private int y = 0;
        private final List<String> childNodes = new ArrayList<>();
        private final List<String> parentNodes = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the skill for this node.
         *
         * @param skill the skill
         * @return this builder
         */
        public Builder skill(Skill skill) {
            this.skill = skill;
            return this;
        }

        /**
         * Sets the tier of this node.
         *
         * @param tier the tier
         * @return this builder
         */
        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        /**
         * Sets the position within the tier.
         *
         * @param position the position
         * @return this builder
         */
        public Builder position(int position) {
            this.position = position;
            return this;
        }

        /**
         * Sets the X coordinate for UI.
         *
         * @param x the X coordinate
         * @return this builder
         */
        public Builder x(int x) {
            this.x = x;
            return this;
        }

        /**
         * Sets the Y coordinate for UI.
         *
         * @param y the Y coordinate
         * @return this builder
         */
        public Builder y(int y) {
            this.y = y;
            return this;
        }

        /**
         * Sets both coordinates for UI.
         *
         * @param x the X coordinate
         * @param y the Y coordinate
         * @return this builder
         */
        public Builder coordinates(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
         * Adds a child node.
         *
         * @param skillId the child skill ID
         * @return this builder
         */
        public Builder childNode(String skillId) {
            this.childNodes.add(skillId);
            return this;
        }

        /**
         * Sets all child nodes.
         *
         * @param skillIds the list of child skill IDs
         * @return this builder
         */
        public Builder childNodes(List<String> skillIds) {
            this.childNodes.clear();
            if (skillIds != null) {
                this.childNodes.addAll(skillIds);
            }
            return this;
        }

        /**
         * Adds a parent node.
         *
         * @param skillId the parent skill ID
         * @return this builder
         */
        public Builder parentNode(String skillId) {
            this.parentNodes.add(skillId);
            return this;
        }

        /**
         * Sets all parent nodes.
         *
         * @param skillIds the list of parent skill IDs
         * @return this builder
         */
        public Builder parentNodes(List<String> skillIds) {
            this.parentNodes.clear();
            if (skillIds != null) {
                this.parentNodes.addAll(skillIds);
            }
            return this;
        }

        /**
         * Builds the SkillNode instance.
         *
         * @return the constructed SkillNode
         * @throws IllegalStateException if required fields are missing
         */
        public SkillNode build() {
            if (skill == null) {
                throw new IllegalStateException("Skill is required for SkillNode");
            }
            return new SkillNode(skill, tier, position, x, y, childNodes, parentNodes);
        }
    }

    // ==================== Object Methods ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillNode skillNode = (SkillNode) o;
        return Objects.equals(skill.getId(), skillNode.skill.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(skill.getId());
    }

    @Override
    public String toString() {
        return "SkillNode{" +
                "skillId='" + skill.getId() + '\'' +
                ", tier=" + tier +
                ", position=" + position +
                ", children=" + childNodes.size() +
                ", parents=" + parentNodes.size() +
                '}';
    }
}
