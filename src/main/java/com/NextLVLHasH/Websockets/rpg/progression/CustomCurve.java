package com.NextLVLHasH.Websockets.rpg.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * A custom experience curve with defined XP breakpoints per level.
 * <p>
 * This implementation allows defining specific XP requirements at certain
 * levels, with linear interpolation between undefined breakpoints. This is
 * useful for hand-tuned progression curves where exact control over
 * specific milestones is needed.
 * </p>
 * <p>
 * Example with breakpoints {1: 100, 5: 500, 10: 2000}:
 * <ul>
 *   <li>Level 1→2: 100 XP (defined)</li>
 *   <li>Level 2→3: ~200 XP (interpolated)</li>
 *   <li>Level 3→4: ~300 XP (interpolated)</li>
 *   <li>Level 4→5: ~400 XP (interpolated)</li>
 *   <li>Level 5→6: 500 XP (defined)</li>
 *   <li>Level 6→7: ~800 XP (interpolated)</li>
 *   <li>Level 10→11: 2000 XP (defined, extrapolated beyond)</li>
 * </ul>
 * </p>
 *
 * @author NextLVLHasH
 * @version 1.0
 */
public class CustomCurve implements ExperienceCurve {

    /**
     * Map of level to XP required for that level-up.
     * TreeMap ensures ordered access for interpolation.
     */
    private final TreeMap<Integer, Long> levelBreakpoints;

    /**
     * Cached calculated cumulative XP for efficiency.
     */
    private final Map<Integer, Long> cumulativeXPCache;

    /**
     * Maximum cached level for performance.
     */
    private static final int MAX_CACHE_LEVEL = 200;

    /**
     * Creates a new CustomCurve with the specified breakpoints.
     *
     * @param breakpoints map of level to XP required (must include at least level 1)
     * @throws IllegalArgumentException if breakpoints is null, empty, or missing level 1
     */
    public CustomCurve(Map<Integer, Long> breakpoints) {
        if (breakpoints == null || breakpoints.isEmpty()) {
            throw new IllegalArgumentException("Breakpoints cannot be null or empty");
        }
        if (!breakpoints.containsKey(1)) {
            throw new IllegalArgumentException("Breakpoints must include level 1");
        }
        for (Map.Entry<Integer, Long> entry : breakpoints.entrySet()) {
            if (entry.getKey() < 1) {
                throw new IllegalArgumentException("Level must be at least 1");
            }
            if (entry.getValue() < 1) {
                throw new IllegalArgumentException("XP requirement must be at least 1");
            }
        }

        this.levelBreakpoints = new TreeMap<>(breakpoints);
        this.cumulativeXPCache = new HashMap<>();
        
        // Pre-calculate cumulative XP for common levels
        buildCumulativeCache();
    }

    /**
     * Creates a CustomCurve from a JSON array.
     * <p>
     * Expected format: [{"level": 1, "xp": 100}, {"level": 5, "xp": 500}, ...]
     * or simplified: [{"1": 100}, {"5": 500}, ...]
     * </p>
     *
     * @param jsonArray the JSON array containing breakpoints
     * @return a new CustomCurve
     * @throws IllegalArgumentException if JSON format is invalid
     */
    public static CustomCurve fromJsonArray(JsonArray jsonArray) {
        if (jsonArray == null || jsonArray.isEmpty()) {
            throw new IllegalArgumentException("JSON array cannot be null or empty");
        }

        Map<Integer, Long> breakpoints = new HashMap<>();

        for (JsonElement element : jsonArray) {
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                
                // Try standard format: {"level": 1, "xp": 100}
                if (obj.has("level") && obj.has("xp")) {
                    int level = obj.get("level").getAsInt();
                    long xp = obj.get("xp").getAsLong();
                    breakpoints.put(level, xp);
                } else {
                    // Try simplified format: {"1": 100}
                    for (String key : obj.keySet()) {
                        try {
                            int level = Integer.parseInt(key);
                            long xp = obj.get(key).getAsLong();
                            breakpoints.put(level, xp);
                        } catch (NumberFormatException e) {
                            // Skip non-numeric keys
                        }
                    }
                }
            }
        }

        if (breakpoints.isEmpty()) {
            throw new IllegalArgumentException("No valid breakpoints found in JSON array");
        }

        // Ensure level 1 exists, or infer it
        if (!breakpoints.containsKey(1)) {
            // Find lowest level and use its value
            int lowestLevel = Collections.min(breakpoints.keySet());
            breakpoints.put(1, breakpoints.get(lowestLevel));
        }

        return new CustomCurve(breakpoints);
    }

    /**
     * Creates a CustomCurve from a JSON object.
     * <p>
     * Expected format: {"1": 100, "5": 500, "10": 2000}
     * </p>
     *
     * @param jsonObject the JSON object containing breakpoints
     * @return a new CustomCurve
     * @throws IllegalArgumentException if JSON format is invalid
     */
    public static CustomCurve fromJsonObject(JsonObject jsonObject) {
        if (jsonObject == null || jsonObject.size() == 0) {
            throw new IllegalArgumentException("JSON object cannot be null or empty");
        }

        Map<Integer, Long> breakpoints = new HashMap<>();

        for (String key : jsonObject.keySet()) {
            try {
                int level = Integer.parseInt(key);
                long xp = jsonObject.get(key).getAsLong();
                breakpoints.put(level, xp);
            } catch (NumberFormatException e) {
                // Skip non-numeric keys
            }
        }

        if (breakpoints.isEmpty()) {
            throw new IllegalArgumentException("No valid breakpoints found in JSON object");
        }

        // Ensure level 1 exists
        if (!breakpoints.containsKey(1)) {
            int lowestLevel = Collections.min(breakpoints.keySet());
            breakpoints.put(1, breakpoints.get(lowestLevel));
        }

        return new CustomCurve(breakpoints);
    }

    /**
     * Pre-builds the cumulative XP cache for performance.
     */
    private void buildCumulativeCache() {
        long cumulative = 0;
        cumulativeXPCache.put(1, 0L); // Level 1 requires 0 XP

        for (int level = 1; level <= MAX_CACHE_LEVEL; level++) {
            cumulative += getXPRequiredForLevel(level);
            cumulativeXPCache.put(level + 1, cumulative);
        }
    }

    /**
     * Gets the XP required for a specific level using interpolation.
     *
     * @param level the level to get XP for
     * @return the interpolated or defined XP requirement
     */
    private long interpolateXP(int level) {
        // Direct lookup
        if (levelBreakpoints.containsKey(level)) {
            return levelBreakpoints.get(level);
        }

        // Find surrounding breakpoints
        Integer lowerKey = levelBreakpoints.floorKey(level);
        Integer upperKey = levelBreakpoints.ceilingKey(level);

        if (lowerKey == null) {
            // Below all breakpoints, use first
            return levelBreakpoints.firstEntry().getValue();
        }

        if (upperKey == null) {
            // Above all breakpoints, extrapolate from last two
            Map.Entry<Integer, Long> lastEntry = levelBreakpoints.lastEntry();
            Integer secondLastKey = levelBreakpoints.lowerKey(lastEntry.getKey());
            
            if (secondLastKey == null) {
                // Only one breakpoint, use its value
                return lastEntry.getValue();
            }

            // Linear extrapolation
            long xpDiff = lastEntry.getValue() - levelBreakpoints.get(secondLastKey);
            int levelDiff = lastEntry.getKey() - secondLastKey;
            double slope = (double) xpDiff / levelDiff;
            int extraLevels = level - lastEntry.getKey();
            return Math.max(1, Math.round(lastEntry.getValue() + slope * extraLevels));
        }

        // Linear interpolation between breakpoints
        long lowerXP = levelBreakpoints.get(lowerKey);
        long upperXP = levelBreakpoints.get(upperKey);
        int levelRange = upperKey - lowerKey;
        int levelOffset = level - lowerKey;

        double fraction = (double) levelOffset / levelRange;
        return Math.round(lowerXP + fraction * (upperXP - lowerXP));
    }

    @Override
    public long calculateXPForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        if (level == 1) {
            return 0L;
        }

        // Use cache if available
        if (cumulativeXPCache.containsKey(level)) {
            return cumulativeXPCache.get(level);
        }

        // Calculate from cache endpoint
        int cacheEnd = Math.min(level - 1, MAX_CACHE_LEVEL);
        long cumulative = cumulativeXPCache.getOrDefault(cacheEnd + 1, 0L);

        for (int l = cacheEnd + 1; l < level; l++) {
            cumulative += getXPRequiredForLevel(l);
        }

        return cumulative;
    }

    @Override
    public int calculateLevelFromXP(long totalXP) {
        if (totalXP < 0) {
            return 1;
        }
        if (totalXP == 0) {
            return 1;
        }

        // Binary search for the correct level
        int low = 1;
        int high = 1000;

        while (calculateXPForLevel(high) <= totalXP) {
            high *= 2;
            if (high > 10000) break;
        }

        while (low < high) {
            int mid = (low + high + 1) / 2;
            long xpRequired = calculateXPForLevel(mid);
            if (xpRequired <= totalXP) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        return low;
    }

    @Override
    public long getXPToNextLevel(int currentLevel, long currentXP) {
        if (currentLevel < 1) {
            throw new IllegalArgumentException("Current level must be at least 1");
        }

        long xpForNextLevel = calculateXPForLevel(currentLevel + 1);
        long remaining = xpForNextLevel - currentXP;
        return Math.max(0, remaining);
    }

    @Override
    public long getXPRequiredForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        return interpolateXP(level);
    }

    @Override
    public String getCurveType() {
        return "custom";
    }

    /**
     * Gets the defined breakpoints (unmodifiable view).
     *
     * @return unmodifiable map of level breakpoints
     */
    public Map<Integer, Long> getBreakpoints() {
        return Collections.unmodifiableMap(levelBreakpoints);
    }

    /**
     * Gets the defined breakpoint levels (unmodifiable view).
     *
     * @return unmodifiable set of breakpoint levels
     */
    public Set<Integer> getDefinedLevels() {
        return Collections.unmodifiableSet(levelBreakpoints.keySet());
    }

    /**
     * Checks if a specific level has a defined breakpoint.
     *
     * @param level the level to check
     * @return true if the level has a defined breakpoint
     */
    public boolean hasBreakpoint(int level) {
        return levelBreakpoints.containsKey(level);
    }

    @Override
    public String toString() {
        return "CustomCurve{" +
                "breakpoints=" + levelBreakpoints +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomCurve that = (CustomCurve) o;
        return Objects.equals(levelBreakpoints, that.levelBreakpoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(levelBreakpoints);
    }
}
