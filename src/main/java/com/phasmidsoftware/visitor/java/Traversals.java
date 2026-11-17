package com.phasmidsoftware.visitor.java;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * The Java entry points to Visitor's traversal engine.
 *
 * <p>Visitor's Scala API is driven by type classes supplied implicitly. This façade takes the two
 * that matter — {@link NeighbourFunction} and {@link Evaluator} — as ordinary arguments, so a Java
 * caller supplies them as lambdas or method references and needs to know nothing about Scala
 * {@code given} instances:
 *
 * <pre>{@code
 * Map<String, List<String>> graph = Map.of(
 *         "A", List.of("B", "C"),
 *         "B", List.of("D"),
 *         "C", List.of("D"),
 *         "D", List.of());
 *
 * TraversalResult<String, String> r =
 *         Traversals.bfs("A", v -> graph.getOrDefault(v, List.of()));
 *
 * r.visited();   // [A, B, C, D]
 * r.cameFrom();  // {B=A, C=A, D=B}
 * }</pre>
 *
 * <p>Each traversal comes in four forms: with and without an early-termination {@code goal}, and
 * with or without an {@link Evaluator}. The plain forms evaluate each node to itself, so
 * {@link TraversalResult#results()} equals {@link TraversalResult#visited()}; the
 * {@code ...Evaluating} forms let the evaluator transform and filter.
 *
 * <p>A {@code goal} predicate halts the traversal <em>after</em> recording the first node it
 * accepts. That node is present in the result; nothing beyond it is visited.
 *
 * <p>Every method is a static method on a final class that cannot be instantiated.
 */
public final class Traversals {

    private Traversals() {
        throw new AssertionError("Traversals is a utility class and must not be instantiated");
    }

    /** A goal predicate that never fires, so the traversal runs to exhaustion. */
    private static <V> Predicate<V> never() {
        return v -> false;
    }

    /** An evaluator that yields each node unchanged, so results mirror the visit order. */
    private static <V> Evaluator<V, V> identity() {
        return Optional::of;
    }

    // ------------------------------------------------------------------------
    // Breadth-first search
    // ------------------------------------------------------------------------

    /**
     * Breadth-first traversal from {@code start}, running to exhaustion.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param <V>        the node type
     * @return the nodes in breadth-first order, with a came-from map
     */
    public static <V> TraversalResult<V, V> bfs(V start, NeighbourFunction<V, V> neighbours) {
        return bfs(start, neighbours, Traversals.never());
    }

    /**
     * Breadth-first traversal from {@code start}, halting after the first node that satisfies
     * {@code goal}.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param goal       halts the traversal after recording the first node it accepts
     * @param <V>        the node type
     * @return the nodes in breadth-first order, with a came-from map
     */
    public static <V> TraversalResult<V, V> bfs(
            V start, NeighbourFunction<V, V> neighbours, Predicate<V> goal) {
        return bfsEvaluating(start, neighbours, Traversals.identity(), goal);
    }

    /**
     * Breadth-first traversal from {@code start}, evaluating each node as it is visited.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param evaluator  what to extract from each node
     * @param <V>        the node type
     * @param <R>        the result type
     * @return the visited nodes and the evaluator's results, both in breadth-first order
     */
    public static <V, R> TraversalResult<V, R> bfsEvaluating(
            V start, NeighbourFunction<V, V> neighbours, Evaluator<V, R> evaluator) {
        return bfsEvaluating(start, neighbours, evaluator, Traversals.never());
    }

    /**
     * Breadth-first traversal from {@code start}, evaluating each node and halting after the first
     * node that satisfies {@code goal}.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param evaluator  what to extract from each node
     * @param goal       halts the traversal after recording the first node it accepts
     * @param <V>        the node type
     * @param <R>        the result type
     * @return the visited nodes and the evaluator's results, both in breadth-first order
     */
    public static <V, R> TraversalResult<V, R> bfsEvaluating(
            V start, NeighbourFunction<V, V> neighbours, Evaluator<V, R> evaluator, Predicate<V> goal) {
        return JavaBridge$.MODULE$.bfs(start, neighbours, evaluator, goal);
    }

    // ------------------------------------------------------------------------
    // Depth-first search
    // ------------------------------------------------------------------------

    /**
     * Depth-first traversal from {@code start}, running to exhaustion.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param order      whether a node is recorded before or after its descendants
     * @param <V>        the node type
     * @return the nodes in depth-first order, with a came-from map
     */
    public static <V> TraversalResult<V, V> dfs(
            V start, NeighbourFunction<V, V> neighbours, Order order) {
        return dfs(start, neighbours, order, Traversals.never());
    }

    /**
     * Depth-first traversal from {@code start}, halting after the first node that satisfies
     * {@code goal}.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param order      whether a node is recorded before or after its descendants
     * @param goal       halts the traversal after recording the first node it accepts
     * @param <V>        the node type
     * @return the nodes in depth-first order, with a came-from map
     */
    public static <V> TraversalResult<V, V> dfs(
            V start, NeighbourFunction<V, V> neighbours, Order order, Predicate<V> goal) {
        return dfsEvaluating(start, neighbours, order, Traversals.identity(), goal);
    }

    /**
     * Depth-first traversal from {@code start}, evaluating each node as it is visited.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param order      whether a node is recorded before or after its descendants
     * @param evaluator  what to extract from each node
     * @param <V>        the node type
     * @param <R>        the result type
     * @return the visited nodes and the evaluator's results, both in depth-first order
     */
    public static <V, R> TraversalResult<V, R> dfsEvaluating(
            V start, NeighbourFunction<V, V> neighbours, Order order, Evaluator<V, R> evaluator) {
        return dfsEvaluating(start, neighbours, order, evaluator, Traversals.never());
    }

    /**
     * Depth-first traversal from {@code start}, evaluating each node and halting after the first
     * node that satisfies {@code goal}.
     *
     * @param start      the node to start from
     * @param neighbours the adjacency of the graph
     * @param order      whether a node is recorded before or after its descendants
     * @param evaluator  what to extract from each node
     * @param goal       halts the traversal after recording the first node it accepts
     * @param <V>        the node type
     * @param <R>        the result type
     * @return the visited nodes and the evaluator's results, both in depth-first order
     */
    public static <V, R> TraversalResult<V, R> dfsEvaluating(
            V start, NeighbourFunction<V, V> neighbours, Order order,
            Evaluator<V, R> evaluator, Predicate<V> goal) {
        return JavaBridge$.MODULE$.dfs(start, neighbours, order, evaluator, goal);
    }

    // ------------------------------------------------------------------------
    // Tree traversal
    // ------------------------------------------------------------------------

    /**
     * Depth-first traversal of a tree whose root may have a different type from its descendants.
     *
     * <p>The root itself is <strong>not</strong> visited: {@code rootChildren} yields the nodes
     * the traversal begins from, and {@code children} carries it down from there. Where the root
     * is of the same type as its descendants, pass the same function twice.
     *
     * <p>{@link TraversalResult#cameFrom()} is always empty for a tree traversal, because the
     * engine does not record came-from where the parent is already evident from the tree's shape.
     *
     * @param root         the root structure
     * @param rootChildren the children of the root
     * @param children     the children of each node below the root
     * @param order        whether a node is recorded before or after its descendants
     * @param <H>          the root type
     * @param <V>          the node type
     * @return the nodes in depth-first order
     */
    public static <H, V> TraversalResult<V, V> traverseTree(
            H root, NeighbourFunction<H, V> rootChildren,
            NeighbourFunction<V, V> children, Order order) {
        return traverseTree(root, rootChildren, children, order, Traversals.never());
    }

    /**
     * Depth-first traversal of a tree, halting after the first node that satisfies {@code goal}.
     *
     * @param root         the root structure
     * @param rootChildren the children of the root
     * @param children     the children of each node below the root
     * @param order        whether a node is recorded before or after its descendants
     * @param goal         halts the traversal after recording the first node it accepts
     * @param <H>          the root type
     * @param <V>          the node type
     * @return the nodes in depth-first order
     */
    public static <H, V> TraversalResult<V, V> traverseTree(
            H root, NeighbourFunction<H, V> rootChildren,
            NeighbourFunction<V, V> children, Order order, Predicate<V> goal) {
        return traverseTreeEvaluating(root, rootChildren, children, order, Traversals.identity(), goal);
    }

    /**
     * Depth-first traversal of a tree, evaluating each node as it is visited.
     *
     * @param root         the root structure
     * @param rootChildren the children of the root
     * @param children     the children of each node below the root
     * @param order        whether a node is recorded before or after its descendants
     * @param evaluator    what to extract from each node
     * @param <H>          the root type
     * @param <V>          the node type
     * @param <R>          the result type
     * @return the visited nodes and the evaluator's results, both in depth-first order
     */
    public static <H, V, R> TraversalResult<V, R> traverseTreeEvaluating(
            H root, NeighbourFunction<H, V> rootChildren,
            NeighbourFunction<V, V> children, Order order, Evaluator<V, R> evaluator) {
        return traverseTreeEvaluating(root, rootChildren, children, order, evaluator, Traversals.never());
    }

    /**
     * Depth-first traversal of a tree, evaluating each node and halting after the first node that
     * satisfies {@code goal}.
     *
     * @param root         the root structure
     * @param rootChildren the children of the root
     * @param children     the children of each node below the root
     * @param order        whether a node is recorded before or after its descendants
     * @param evaluator    what to extract from each node
     * @param goal         halts the traversal after recording the first node it accepts
     * @param <H>          the root type
     * @param <V>          the node type
     * @param <R>          the result type
     * @return the visited nodes and the evaluator's results, both in depth-first order
     */
    public static <H, V, R> TraversalResult<V, R> traverseTreeEvaluating(
            H root, NeighbourFunction<H, V> rootChildren,
            NeighbourFunction<V, V> children, Order order,
            Evaluator<V, R> evaluator, Predicate<V> goal) {
        return JavaBridge$.MODULE$.traverseTree(root, rootChildren, children, order, evaluator, goal);
    }
}
