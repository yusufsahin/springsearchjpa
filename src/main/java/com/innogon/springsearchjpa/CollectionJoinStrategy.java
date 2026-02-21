package com.innogon.springsearchjpa;

/**
 * Strategy for handling collection (plural) associations in nested search paths.
 */
public enum CollectionJoinStrategy {

    /**
     * Uses LEFT JOIN with DISTINCT for collection associations. Multiple predicates
     * on the same collection path share the same join, so AND'd conditions apply
     * to the same row. This is the default and preserves backward-compatible semantics.
     */
    SHARED_JOIN,

    /**
     * Uses EXISTS subqueries for paths containing collection associations.
     * Eliminates the need for DISTINCT and enables database semi-join optimization.
     * Better for pagination COUNT queries and large datasets.
     * <p>
     * Note: AND'd predicates on the same collection are evaluated independently —
     * different rows may satisfy different conditions.
     */
    EXISTS
}
