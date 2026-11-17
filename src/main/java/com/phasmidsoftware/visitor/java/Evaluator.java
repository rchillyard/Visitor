package com.phasmidsoftware.visitor.java;

import java.util.Optional;

/**
 * Extracts an optional result from a node as it is visited.
 *
 * <p>This is the Java-facing form of Visitor's {@code Evaluable[V, R]} type class: it is the
 * "what do we want to know about this node" concern. Returning {@link Optional#empty()} records
 * the visit but contributes nothing to {@link TraversalResult#results()}, so an evaluator doubles
 * as a filter over the visited nodes.
 *
 * <p>An evaluator sees the node alone. It is not told which edge the node was reached by; if your
 * decision depends on that, the traversal result's {@link TraversalResult#cameFrom()} map carries
 * the predecessor after the fact.
 *
 * @param <V> the node type
 * @param <R> the result type
 */
@FunctionalInterface
public interface Evaluator<V, R> {

    /**
     * Evaluates {@code node}.
     *
     * @param node the node being visited
     * @return the result for this node, or {@link Optional#empty()} to contribute none
     */
    Optional<R> evaluate(V node);
}
