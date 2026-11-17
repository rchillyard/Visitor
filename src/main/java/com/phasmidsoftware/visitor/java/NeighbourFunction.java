package com.phasmidsoftware.visitor.java;

/**
 * Supplies the neighbours (or children) of a node.
 *
 * <p>This is the Java-facing form of Visitor's {@code Neighbours[H, V]} type class: it is the
 * "how do we traverse" concern, completely decoupled from the domain. For an ordinary graph the
 * two type parameters are the same, so a homogeneous adjacency is a
 * {@code NeighbourFunction<V, V>}. They differ only for a tree whose root has a different type
 * from its descendants — see
 * {@link Traversals#traverseTree(Object, NeighbourFunction, NeighbourFunction, Order)}.
 *
 * <p>The returned {@code Iterable} may be empty but must not be {@code null}. Its order is the
 * order in which neighbours are offered to the traversal frontier, so it determines tie-breaking
 * between equally eligible neighbours.
 *
 * @param <H> the node type whose neighbours are being requested
 * @param <V> the neighbour type
 */
@FunctionalInterface
public interface NeighbourFunction<H, V> {

    /**
     * Returns the neighbours of {@code node}.
     *
     * @param node the node whose neighbours are wanted
     * @return the neighbours, possibly empty, never {@code null}
     */
    Iterable<V> neighbours(H node);
}
