package com.NextLVLHasH.Websockets.rpg.attributes;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a modifier that can be applied to an attribute.
 * Modifiers can be temporary (with expiration) or permanent, and can come from
 * various sources like equipment, buffs, skills, or status effects.
 * 
 * <p>Each modifier has:
 * <ul>
 *   <li>A unique identifier for tracking and removal</li>
 *   <li>A source indicating where the modifier came from</li>
 *   <li>The target attribute type</li>
 *   <li>The modifier type (FLAT, PERCENTAGE, or MULTIPLICATIVE)</li>
 *   <li>The modifier value</li>
 *   <li>Optional expiration time</li>
 *   <li>Priority for ordering within the same modifier type</li>
 * </ul>
 * 
 * <p>Use the {@link Builder} to create instances:
 * <pre>
 * AttributeModifier mod = AttributeModifier.builder()
 *     .id("strength_potion_001")
 *     .source("Potion of Strength")
 *     .attribute(AttributeType.STRENGTH)
 *     .type(ModifierType.FLAT)
 *     .value(10)
 *     .duration(60000) // 60 seconds
 *     .build();
 * </pre>
 * 
 * @author NextLVLHasH
 * @version 1.0
 */
public class AttributeModifier implements Comparable<AttributeModifier> {

    private final String id;
    private final String source;
    private final AttributeType attribute;
    private final ModifierType type;
    private final double value;
    private final long expiresAt;
    private final int priority;
    private final long createdAt;

    /**
     * Private constructor - use {@link Builder} to create instances.
     */
    private AttributeModifier(Builder builder) {
        this.id = builder.id;
        this.source = builder.source;
        this.attribute = builder.attribute;
        this.type = builder.type;
        this.value = builder.value;
        this.expiresAt = builder.expiresAt;
        this.priority = builder.priority;
        this.createdAt = builder.createdAt;
    }

    /**
     * Gets the unique identifier for this modifier.
     *
     * @return the modifier ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the source of this modifier (e.g., "Potion of Strength", "Equipment: Iron Helm").
     *
     * @return the source description
     */
    public String getSource() {
        return source;
    }

    /**
     * Gets the attribute type this modifier affects.
     *
     * @return the target attribute type
     */
    public AttributeType getAttribute() {
        return attribute;
    }

    /**
     * Gets the type of modification (FLAT, PERCENTAGE, or MULTIPLICATIVE).
     *
     * @return the modifier type
     */
    public ModifierType getType() {
        return type;
    }

    /**
     * Gets the modifier value.
     *
     * @return the value (interpretation depends on modifier type)
     */
    public double getValue() {
        return value;
    }

    /**
     * Gets the expiration timestamp in milliseconds since epoch.
     * A value of 0 indicates a permanent modifier.
     *
     * @return the expiration timestamp, or 0 if permanent
     */
    public long getExpiresAt() {
        return expiresAt;
    }

    /**
     * Gets the priority for sorting modifiers of the same type.
     * Higher priority modifiers are applied first within their type group.
     *
     * @return the priority value
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Gets the timestamp when this modifier was created.
     *
     * @return creation timestamp in milliseconds since epoch
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Checks if this modifier has expired.
     *
     * @return true if the modifier has expired, false if permanent or still active
     */
    public boolean isExpired() {
        return !isPermanent() && System.currentTimeMillis() >= expiresAt;
    }

    /**
     * Checks if this modifier is permanent (never expires).
     *
     * @return true if permanent, false if it has an expiration time
     */
    public boolean isPermanent() {
        return expiresAt == 0;
    }

    /**
     * Gets the remaining time until this modifier expires.
     *
     * @return remaining time in milliseconds, 0 if expired, or Long.MAX_VALUE if permanent
     */
    public long getRemainingTime() {
        if (isPermanent()) {
            return Long.MAX_VALUE;
        }
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets a human-readable description of the remaining time.
     *
     * @return formatted time string (e.g., "2:30", "Permanent", "Expired")
     */
    public String getRemainingTimeFormatted() {
        if (isPermanent()) {
            return "Permanent";
        }
        if (isExpired()) {
            return "Expired";
        }
        long remaining = getRemainingTime();
        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d:%02d", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Gets a formatted display string for this modifier.
     *
     * @return formatted string (e.g., "+10 Strength from Potion of Strength")
     */
    public String getDisplayString() {
        String valueStr = type.formatValue(value);
        return String.format("%s %s from %s", valueStr, attribute.getDisplayName(), source);
    }

    /**
     * Compares modifiers for ordering.
     * First by modifier type order, then by priority (descending), then by creation time.
     *
     * @param other the other modifier to compare to
     * @return negative if this should be applied first, positive if after
     */
    @Override
    public int compareTo(AttributeModifier other) {
        // First compare by modifier type order
        int typeCompare = Integer.compare(this.type.getOrder(), other.type.getOrder());
        if (typeCompare != 0) {
            return typeCompare;
        }
        // Then by priority (higher priority first within same type)
        int priorityCompare = Integer.compare(other.priority, this.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // Finally by creation time (older first)
        return Long.compare(this.createdAt, other.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeModifier that = (AttributeModifier) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AttributeModifier{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", attribute=" + attribute +
                ", type=" + type +
                ", value=" + value +
                ", expiresAt=" + expiresAt +
                ", priority=" + priority +
                '}';
    }

    /**
     * Creates a new Builder for constructing AttributeModifier instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder initialized with this modifier's values.
     * Useful for creating modified copies.
     *
     * @return a new Builder with this modifier's values
     */
    public Builder toBuilder() {
        return new Builder()
                .id(this.id)
                .source(this.source)
                .attribute(this.attribute)
                .type(this.type)
                .value(this.value)
                .expiresAt(this.expiresAt)
                .priority(this.priority);
    }

    /**
     * Builder class for creating AttributeModifier instances.
     * Provides a fluent API for setting modifier properties.
     */
    public static class Builder {
        private String id;
        private String source = "Unknown";
        private AttributeType attribute;
        private ModifierType type = ModifierType.FLAT;
        private double value = 0;
        private long expiresAt = 0; // 0 = permanent
        private int priority = 0;
        private long createdAt = System.currentTimeMillis();

        /**
         * Creates a new Builder with default values.
         */
        public Builder() {
        }

        /**
         * Sets the unique identifier for this modifier.
         * If not set, a random UUID will be generated.
         *
         * @param id the modifier ID
         * @return this builder for chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Generates a random UUID as the modifier ID.
         *
         * @return this builder for chaining
         */
        public Builder randomId() {
            this.id = UUID.randomUUID().toString();
            return this;
        }

        /**
         * Sets the source description for this modifier.
         *
         * @param source the source (e.g., "Potion of Strength", "Equipment: Iron Helm")
         * @return this builder for chaining
         */
        public Builder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the target attribute type.
         *
         * @param attribute the attribute this modifier affects
         * @return this builder for chaining
         */
        public Builder attribute(AttributeType attribute) {
            this.attribute = attribute;
            return this;
        }

        /**
         * Sets the modifier type.
         *
         * @param type the type of modification (FLAT, PERCENTAGE, MULTIPLICATIVE)
         * @return this builder for chaining
         */
        public Builder type(ModifierType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the modifier value.
         *
         * @param value the value (interpretation depends on type)
         * @return this builder for chaining
         */
        public Builder value(double value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the expiration timestamp.
         * Use 0 for permanent modifiers.
         *
         * @param expiresAt expiration time in milliseconds since epoch
         * @return this builder for chaining
         */
        public Builder expiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        /**
         * Sets the duration from now until expiration.
         *
         * @param durationMs duration in milliseconds
         * @return this builder for chaining
         */
        public Builder duration(long durationMs) {
            this.expiresAt = System.currentTimeMillis() + durationMs;
            return this;
        }

        /**
         * Sets this modifier as permanent (never expires).
         *
         * @return this builder for chaining
         */
        public Builder permanent() {
            this.expiresAt = 0;
            return this;
        }

        /**
         * Sets the priority for ordering within the same modifier type.
         * Higher priority modifiers are applied first.
         *
         * @param priority the priority value
         * @return this builder for chaining
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the creation timestamp.
         * Normally this is set automatically to the current time.
         *
         * @param createdAt creation time in milliseconds since epoch
         * @return this builder for chaining
         */
        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Builds the AttributeModifier instance.
         *
         * @return the constructed AttributeModifier
         * @throws IllegalStateException if required fields are not set
         */
        public AttributeModifier build() {
            if (id == null || id.isEmpty()) {
                id = UUID.randomUUID().toString();
            }
            if (attribute == null) {
                throw new IllegalStateException("Attribute type must be specified");
            }
            if (type == null) {
                throw new IllegalStateException("Modifier type must be specified");
            }
            return new AttributeModifier(this);
        }
    }
}
