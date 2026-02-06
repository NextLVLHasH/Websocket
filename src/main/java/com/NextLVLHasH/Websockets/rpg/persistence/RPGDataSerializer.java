package com.NextLVLHasH.Websockets.rpg.persistence;

import com.NextLVLHasH.Websockets.rpg.core.AttributeType;
import com.NextLVLHasH.Websockets.rpg.core.PlayerRPGData;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

/**
 * JSON serializer/deserializer for PlayerRPGData.
 * Handles conversion between PlayerRPGData objects and JSON strings.
 */
@SuppressWarnings("unused")
public class RPGDataSerializer {

    private final Gson gson;

    /**
     * Creates a new RPGDataSerializer with default configuration.
     */
    public RPGDataSerializer() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(AttributeType.class, new AttributeTypeAdapter())
                .registerTypeAdapter(PlayerRPGData.class, new PlayerRPGDataAdapter())
                .create();
    }

    /**
     * Serializes a PlayerRPGData object to a JSON string.
     *
     * @param data the player data to serialize
     * @return the JSON string representation
     * @throws IllegalArgumentException if data is null
     */
    public String serialize(PlayerRPGData data) {
        if (data == null) {
            throw new IllegalArgumentException("PlayerRPGData cannot be null");
        }
        return gson.toJson(data);
    }

    /**
     * Deserializes a JSON string to a PlayerRPGData object.
     *
     * @param json the JSON string to deserialize
     * @return the PlayerRPGData object
     * @throws IllegalArgumentException if json is null or empty
     * @throws JsonSyntaxException      if the JSON is malformed
     */
    public PlayerRPGData deserialize(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty");
        }
        return gson.fromJson(json, PlayerRPGData.class);
    }

    /**
     * Custom type adapter for AttributeType enum.
     */
    private static class AttributeTypeAdapter implements JsonSerializer<AttributeType>, JsonDeserializer<AttributeType> {
        @Override
        public JsonElement serialize(AttributeType src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }

        @Override
        public AttributeType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String name = json.getAsString();
            try {
                return AttributeType.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Unknown AttributeType: " + name, e);
            }
        }
    }

    /**
     * Custom type adapter for PlayerRPGData to handle complex serialization.
     */
    private static class PlayerRPGDataAdapter implements JsonSerializer<PlayerRPGData>, JsonDeserializer<PlayerRPGData> {

        @Override
        public JsonElement serialize(PlayerRPGData src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();

            // Identity
            json.addProperty("playerId", src.getPlayerId().toString());
            json.addProperty("playerName", src.getPlayerName());

            // Character Creation
            json.addProperty("selectedClass", src.getSelectedClass());
            json.addProperty("selectedRace", src.getSelectedRace());
            json.addProperty("selectedProfession", src.getSelectedProfession());
            json.addProperty("characterCreated", src.isCharacterCreated());

            // Progression
            json.addProperty("level", src.getLevel());
            json.addProperty("currentXP", src.getCurrentXP());
            json.addProperty("xpToNextLevel", src.getXpToNextLevel());
            json.addProperty("skillPoints", src.getSkillPoints());

            // Attributes - store as nested objects
            JsonObject baseAttrs = new JsonObject();
            for (Map.Entry<AttributeType, Integer> entry : src.getBaseAttributes().entrySet()) {
                baseAttrs.addProperty(entry.getKey().name(), entry.getValue());
            }
            json.add("baseAttributes", baseAttrs);

            JsonObject currentAttrs = new JsonObject();
            for (Map.Entry<AttributeType, Integer> entry : src.getCurrentAttributes().entrySet()) {
                currentAttrs.addProperty(entry.getKey().name(), entry.getValue());
            }
            json.add("currentAttributes", currentAttrs);

            // Resources
            json.addProperty("maxHealth", src.getMaxHealth());
            json.addProperty("currentHealth", src.getCurrentHealth());
            json.addProperty("maxMana", src.getMaxMana());
            json.addProperty("currentMana", src.getCurrentMana());
            json.addProperty("maxStamina", src.getMaxStamina());
            json.addProperty("currentStamina", src.getCurrentStamina());

            // Regeneration
            json.addProperty("healthRegen", src.getHealthRegen());
            json.addProperty("manaRegen", src.getManaRegen());
            json.addProperty("staminaRegen", src.getStaminaRegen());

            // Skills
            JsonArray unlockedSkillsArray = new JsonArray();
            for (String skill : src.getUnlockedSkills()) {
                unlockedSkillsArray.add(skill);
            }
            json.add("unlockedSkills", unlockedSkillsArray);

            JsonObject cooldowns = new JsonObject();
            for (Map.Entry<String, Long> entry : src.getSkillCooldowns().entrySet()) {
                cooldowns.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("skillCooldowns", cooldowns);

            // Effects
            JsonArray effectsArray = new JsonArray();
            for (String effect : src.getActiveEffects()) {
                effectsArray.add(effect);
            }
            json.add("activeEffects", effectsArray);

            // Timestamps
            json.addProperty("createdAt", src.getCreatedAt());
            json.addProperty("lastUpdated", src.getLastUpdated());

            return json;
        }

        @Override
        public PlayerRPGData deserialize(JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject json = jsonElement.getAsJsonObject();

            // Extract identity
            UUID playerId = UUID.fromString(getStringOrThrow(json, "playerId"));
            String playerName = getStringOrThrow(json, "playerName");

            // Create the data object
            PlayerRPGData data = new PlayerRPGData(playerId, playerName);

            // Character Creation
            if (json.has("selectedClass") && !json.get("selectedClass").isJsonNull()) {
                data.setSelectedClass(json.get("selectedClass").getAsString());
            }
            if (json.has("selectedRace") && !json.get("selectedRace").isJsonNull()) {
                data.setSelectedRace(json.get("selectedRace").getAsString());
            }
            if (json.has("selectedProfession") && !json.get("selectedProfession").isJsonNull()) {
                data.setSelectedProfession(json.get("selectedProfession").getAsString());
            }
            if (json.has("characterCreated")) {
                data.setCharacterCreated(json.get("characterCreated").getAsBoolean());
            }

            // Progression
            if (json.has("level")) {
                data.setLevel(json.get("level").getAsInt());
            }
            if (json.has("currentXP")) {
                data.setCurrentXP(json.get("currentXP").getAsLong());
            }
            if (json.has("xpToNextLevel")) {
                data.setXpToNextLevel(json.get("xpToNextLevel").getAsLong());
            }
            if (json.has("skillPoints")) {
                data.setSkillPoints(json.get("skillPoints").getAsInt());
            }

            // Base Attributes
            if (json.has("baseAttributes") && json.get("baseAttributes").isJsonObject()) {
                JsonObject baseAttrs = json.getAsJsonObject("baseAttributes");
                for (String key : baseAttrs.keySet()) {
                    try {
                        AttributeType type = AttributeType.valueOf(key.toUpperCase());
                        data.setBaseAttribute(type, baseAttrs.get(key).getAsInt());
                    } catch (IllegalArgumentException ignored) {
                        // Skip unknown attributes
                    }
                }
            }

            // Current Attributes
            if (json.has("currentAttributes") && json.get("currentAttributes").isJsonObject()) {
                JsonObject currentAttrs = json.getAsJsonObject("currentAttributes");
                for (String key : currentAttrs.keySet()) {
                    try {
                        AttributeType type = AttributeType.valueOf(key.toUpperCase());
                        data.setCurrentAttribute(type, currentAttrs.get(key).getAsInt());
                    } catch (IllegalArgumentException ignored) {
                        // Skip unknown attributes
                    }
                }
            }

            // Resources
            if (json.has("maxHealth")) {
                data.setMaxHealth(json.get("maxHealth").getAsDouble());
            }
            if (json.has("currentHealth")) {
                data.setCurrentHealth(json.get("currentHealth").getAsDouble());
            }
            if (json.has("maxMana")) {
                data.setMaxMana(json.get("maxMana").getAsDouble());
            }
            if (json.has("currentMana")) {
                data.setCurrentMana(json.get("currentMana").getAsDouble());
            }
            if (json.has("maxStamina")) {
                data.setMaxStamina(json.get("maxStamina").getAsDouble());
            }
            if (json.has("currentStamina")) {
                data.setCurrentStamina(json.get("currentStamina").getAsDouble());
            }

            // Regeneration
            if (json.has("healthRegen")) {
                data.setHealthRegen(json.get("healthRegen").getAsDouble());
            }
            if (json.has("manaRegen")) {
                data.setManaRegen(json.get("manaRegen").getAsDouble());
            }
            if (json.has("staminaRegen")) {
                data.setStaminaRegen(json.get("staminaRegen").getAsDouble());
            }

            // Unlocked Skills
            if (json.has("unlockedSkills") && json.get("unlockedSkills").isJsonArray()) {
                JsonArray skills = json.getAsJsonArray("unlockedSkills");
                for (JsonElement skill : skills) {
                    data.unlockSkill(skill.getAsString());
                }
            }

            // Skill Cooldowns
            if (json.has("skillCooldowns") && json.get("skillCooldowns").isJsonObject()) {
                JsonObject cooldowns = json.getAsJsonObject("skillCooldowns");
                for (String key : cooldowns.keySet()) {
                    long cooldownEnd = cooldowns.get(key).getAsLong();
                    // Only set cooldown if it's still active
                    long remaining = cooldownEnd - System.currentTimeMillis();
                    if (remaining > 0) {
                        data.setCooldown(key, remaining);
                    }
                }
            }

            // Active Effects
            if (json.has("activeEffects") && json.get("activeEffects").isJsonArray()) {
                JsonArray effects = json.getAsJsonArray("activeEffects");
                for (JsonElement effect : effects) {
                    data.addActiveEffect(effect.getAsString());
                }
            }

            return data;
        }

        private String getStringOrThrow(JsonObject json, String field) throws JsonParseException {
            if (!json.has(field) || json.get(field).isJsonNull()) {
                throw new JsonParseException("Missing required field: " + field);
            }
            return json.get(field).getAsString();
        }
    }

    /**
     * Validates if a JSON string is valid PlayerRPGData format.
     *
     * @param json the JSON string to validate
     * @return true if valid, false otherwise
     */
    public boolean isValid(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            PlayerRPGData data = deserialize(json);
            return data != null && data.getPlayerId() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a compact (non-pretty) JSON string for storage efficiency.
     *
     * @param data the player data to serialize
     * @return compact JSON string
     */
    public String serializeCompact(PlayerRPGData data) {
        if (data == null) {
            throw new IllegalArgumentException("PlayerRPGData cannot be null");
        }
        Gson compactGson = new GsonBuilder()
                .registerTypeAdapter(AttributeType.class, new AttributeTypeAdapter())
                .registerTypeAdapter(PlayerRPGData.class, new PlayerRPGDataAdapter())
                .create();
        return compactGson.toJson(data);
    }
}
