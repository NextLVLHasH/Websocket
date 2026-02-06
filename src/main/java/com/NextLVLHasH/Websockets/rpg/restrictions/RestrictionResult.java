package com.NextLVLHasH.Websockets.rpg.restrictions;

import java.util.*;

/**
 * Represents the result of a restriction check.
 * Contains whether the action is allowed and any failure reasons.
 */
public final class RestrictionResult {

    private final boolean allowed;
    private final List<String> failureReasons;

    /**
     * Private constructor - use static factory methods.
     *
     * @param allowed        whether the action is allowed
     * @param failureReasons list of reasons if not allowed
     */
    private RestrictionResult(boolean allowed, List<String> failureReasons) {
        this.allowed = allowed;
        this.failureReasons = Collections.unmodifiableList(new ArrayList<>(failureReasons));
    }

    // ==================== Static Factory Methods ====================

    /**
     * Creates a successful result indicating the action is allowed.
     *
     * @return a RestrictionResult with allowed=true
     */
    public static RestrictionResult allowed() {
        return new RestrictionResult(true, Collections.emptyList());
    }

    /**
     * Creates a denied result with the specified failure reasons.
     *
     * @param reasons one or more reasons why the action is denied
     * @return a RestrictionResult with allowed=false
     */
    public static RestrictionResult denied(String... reasons) {
        List<String> reasonList = new ArrayList<>();
        if (reasons != null) {
            for (String reason : reasons) {
                if (reason != null && !reason.isBlank()) {
                    reasonList.add(reason.trim());
                }
            }
        }
        if (reasonList.isEmpty()) {
            reasonList.add("Action denied");
        }
        return new RestrictionResult(false, reasonList);
    }

    /**
     * Creates a denied result with a list of failure reasons.
     *
     * @param reasons list of reasons why the action is denied
     * @return a RestrictionResult with allowed=false
     */
    public static RestrictionResult denied(List<String> reasons) {
        List<String> reasonList = new ArrayList<>();
        if (reasons != null) {
            reasons.stream()
                    .filter(r -> r != null && !r.isBlank())
                    .map(String::trim)
                    .forEach(reasonList::add);
        }
        if (reasonList.isEmpty()) {
            reasonList.add("Action denied");
        }
        return new RestrictionResult(false, reasonList);
    }

    // ==================== Getters ====================

    /**
     * Checks if the action is allowed.
     *
     * @return true if allowed, false if denied
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * Checks if the action is denied.
     *
     * @return true if denied, false if allowed
     */
    public boolean isDenied() {
        return !allowed;
    }

    /**
     * Gets the list of failure reasons.
     * Empty if the action is allowed.
     *
     * @return unmodifiable list of failure reasons
     */
    public List<String> getFailureReasons() {
        return failureReasons;
    }

    /**
     * Gets the first failure reason, or null if allowed.
     *
     * @return the first failure reason, or null
     */
    public String getFirstReason() {
        return failureReasons.isEmpty() ? null : failureReasons.get(0);
    }

    /**
     * Gets all failure reasons as a single formatted string.
     *
     * @param separator the separator between reasons
     * @return formatted string of all reasons
     */
    public String getReasonsFormatted(String separator) {
        return String.join(separator, failureReasons);
    }

    /**
     * Gets all failure reasons as a comma-separated string.
     *
     * @return comma-separated reasons
     */
    public String getReasonsAsString() {
        return getReasonsFormatted(", ");
    }

    // ==================== Merge Operations ====================

    /**
     * Merges this result with another result.
     * The merged result is allowed only if both results are allowed.
     * All failure reasons are combined.
     *
     * @param other the other result to merge with
     * @return a new merged RestrictionResult
     */
    public RestrictionResult merge(RestrictionResult other) {
        if (other == null) {
            return this;
        }
        
        boolean mergedAllowed = this.allowed && other.allowed;
        
        if (mergedAllowed) {
            return RestrictionResult.allowed();
        }
        
        List<String> combinedReasons = new ArrayList<>();
        combinedReasons.addAll(this.failureReasons);
        combinedReasons.addAll(other.failureReasons);
        
        return new RestrictionResult(false, combinedReasons);
    }

    /**
     * Merges multiple results into one.
     * The merged result is allowed only if all results are allowed.
     *
     * @param results the results to merge
     * @return a new merged RestrictionResult
     */
    public static RestrictionResult mergeAll(RestrictionResult... results) {
        if (results == null || results.length == 0) {
            return RestrictionResult.allowed();
        }
        
        RestrictionResult merged = results[0];
        for (int i = 1; i < results.length; i++) {
            if (results[i] != null) {
                merged = merged.merge(results[i]);
            }
        }
        return merged;
    }

    /**
     * Merges a collection of results into one.
     *
     * @param results the results to merge
     * @return a new merged RestrictionResult
     */
    public static RestrictionResult mergeAll(Collection<RestrictionResult> results) {
        if (results == null || results.isEmpty()) {
            return RestrictionResult.allowed();
        }
        
        RestrictionResult merged = RestrictionResult.allowed();
        for (RestrictionResult result : results) {
            if (result != null) {
                merged = merged.merge(result);
            }
        }
        return merged;
    }

    // ==================== Utility Methods ====================

    /**
     * Executes an action if this result is allowed.
     *
     * @param action the action to execute
     * @return this result for chaining
     */
    public RestrictionResult ifAllowed(Runnable action) {
        if (allowed && action != null) {
            action.run();
        }
        return this;
    }

    /**
     * Executes an action if this result is denied.
     *
     * @param action the action to execute, receives failure reasons
     * @return this result for chaining
     */
    public RestrictionResult ifDenied(java.util.function.Consumer<List<String>> action) {
        if (!allowed && action != null) {
            action.accept(failureReasons);
        }
        return this;
    }

    /**
     * Throws an exception if this result is denied.
     *
     * @throws IllegalStateException if denied
     */
    public void orThrow() {
        if (!allowed) {
            throw new IllegalStateException("Restriction check failed: " + getReasonsAsString());
        }
    }

    /**
     * Throws a custom exception if this result is denied.
     *
     * @param exceptionSupplier supplier for the exception
     * @param <X>               the exception type
     * @throws X if denied
     */
    public <X extends Throwable> void orThrow(
            java.util.function.Function<List<String>, X> exceptionSupplier) throws X {
        if (!allowed) {
            throw exceptionSupplier.apply(failureReasons);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RestrictionResult that = (RestrictionResult) obj;
        return allowed == that.allowed && Objects.equals(failureReasons, that.failureReasons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowed, failureReasons);
    }

    @Override
    public String toString() {
        if (allowed) {
            return "RestrictionResult{allowed=true}";
        }
        return "RestrictionResult{allowed=false, reasons=" + failureReasons + "}";
    }
}
