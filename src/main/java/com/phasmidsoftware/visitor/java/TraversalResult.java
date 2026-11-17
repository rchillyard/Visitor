package com.phasmidsoftware.visitor.java;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a traversal found.
 *
 * <p>Instances are created by {@link Traversals}; there is no public constructor. All three
 * accessors return unmodifiable views.
 *
 * @param <V> the node type
 * @param <R> the result type produced by the {@link Evaluator}
 */
public final class TraversalResult<V, R> {

    private final List<V> visited;
    private final List<R> results;
    private final Map<V, V> cameFrom;

    /**
     * Package-private: only the Scala bridge constructs these.
     *
     * @param visited  the nodes visited, in visit order
     * @param results  the results the evaluator produced, in visit order
     * @param cameFrom the predecessor map
     */
    TraversalResult(List<V> visited, List<R> results, Map<V, V> cameFrom) {
        this.visited = Collections.unmodifiableList(visited);
        this.results = Collections.unmodifiableList(results);
        this.cameFrom = Collections.unmodifiableMap(cameFrom);
    }

    /**
     * The nodes that were visited, in the order they were visited.
     *
     * <p>No node appears twice: the traversal keeps a visited set.
     *
     * @return the visited nodes, in visit order
     */
    public List<V> visited() {
        return visited;
    }

    /**
     * The results the {@link Evaluator} produced, in visit order.
     *
     * <p>Nodes for which the evaluator returned {@link Optional#empty()} are absent, so this list
     * may be shorter than {@link #visited()}. For the traversals that take no evaluator, each node
     * evaluates to itself and this list equals {@link #visited()}.
     *
     * @return the results, in visit order
     */
    public List<R> results() {
        return results;
    }

    /**
     * Which node each node was first reached from.
     *
     * <p>The start node is absent, having been reached from nowhere. The map records
     * <em>first</em> discovery: a node reached again by a longer route keeps its original
     * predecessor.
     *
     * <p>This map is <strong>always empty for
     * {@link Traversals#traverseTree(Object, NeighbourFunction, NeighbourFunction, Order)}</strong>,
     * because the traversal engine does not record came-from for a tree — the parent of each node
     * is already known from the tree's own shape.
     *
     * @return the predecessor map, empty if the traversal did not record one
     */
    public Map<V, V> cameFrom() {
        return cameFrom;
    }

    @Override
    public String toString() {
        return "TraversalResult(visited=" + visited.size()
                + ", results=" + results.size()
                + ", cameFrom=" + cameFrom.size() + ")";
    }
}
