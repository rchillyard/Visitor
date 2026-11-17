package com.phasmidsoftware.visitor.java;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the Java façade over Visitor's traversal engine.
 *
 * <p>The graph used throughout is the diamond
 *
 * <pre>
 *       A
 *      / \
 *     B   C
 *      \ /
 *       D
 * </pre>
 *
 * with adjacency held in insertion order, so neighbour order is deterministic.
 */
class TraversalsTest {

    /** A -&gt; B, C; B -&gt; D; C -&gt; D; D -&gt; nothing. Insertion-ordered. */
    private static final Map<String, List<String>> DIAMOND = diamond();

    private static Map<String, List<String>> diamond() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("A", List.of("B", "C"));
        m.put("B", List.of("D"));
        m.put("C", List.of("D"));
        m.put("D", List.of());
        return m;
    }

    private static final NeighbourFunction<String, String> ADJACENCY =
            v -> DIAMOND.getOrDefault(v, List.of());

    // ------------------------------------------------------------------------
    // BFS
    // ------------------------------------------------------------------------

    @Test
    void bfsVisitsEveryNodeInBreadthFirstOrder() {
        TraversalResult<String, String> r = Traversals.bfs("A", ADJACENCY);
        assertEquals(List.of("A", "B", "C", "D"), r.visited());
    }

    @Test
    void bfsEvaluatesEachNodeToItselfByDefault() {
        TraversalResult<String, String> r = Traversals.bfs("A", ADJACENCY);
        assertEquals(r.visited(), r.results());
    }

    @Test
    void bfsRecordsTheNodeEachNodeWasFirstReachedFrom() {
        TraversalResult<String, String> r = Traversals.bfs("A", ADJACENCY);
        // D is reachable from both B and C; first discovery wins, and B is expanded first.
        assertEquals(Map.of("B", "A", "C", "A", "D", "B"), r.cameFrom());
    }

    @Test
    void bfsLeavesTheStartNodeOutOfTheCameFromMap() {
        TraversalResult<String, String> r = Traversals.bfs("A", ADJACENCY);
        assertTrue(r.cameFrom().get("A") == null, "start node was reached from nowhere");
    }

    @Test
    void bfsHaltsAfterRecordingTheFirstNodeSatisfyingTheGoal() {
        TraversalResult<String, String> r = Traversals.bfs("A", ADJACENCY, "C"::equals);
        // The goal node is recorded, and nothing beyond it is visited.
        assertEquals(List.of("A", "B", "C"), r.visited());
    }

    @Test
    void cameFromRecordsDiscoveryNotVisiting() {
        TraversalResult<String, String> r = Traversals.bfs("A", ADJACENCY, "C"::equals);
        // D was discovered when B was expanded, then never visited because the goal fired first.
        // So came-from legitimately holds a node absent from visited().
        assertEquals("B", r.cameFrom().get("D"));
        assertTrue(!r.visited().contains("D"), "D should not have been visited");
    }

    // ------------------------------------------------------------------------
    // DFS
    // ------------------------------------------------------------------------

    @Test
    void dfsPreOrderRecordsAParentBeforeItsDescendants() {
        TraversalResult<String, String> r = Traversals.dfs("A", ADJACENCY, Order.PRE);
        assertEquals(List.of("A", "B", "D", "C"), r.visited());
    }

    @Test
    void dfsPostOrderRecordsAParentAfterItsDescendants() {
        TraversalResult<String, String> r = Traversals.dfs("A", ADJACENCY, Order.POST);
        assertEquals(List.of("D", "B", "C", "A"), r.visited());
    }

    @Test
    void dfsRejectsANullOrder() {
        IllegalArgumentException e = assertThrows(
                IllegalArgumentException.class,
                () -> Traversals.dfs("A", ADJACENCY, null));
        assertTrue(e.getMessage().contains("Order"), "message should name the offending argument");
    }

    // ------------------------------------------------------------------------
    // Evaluation
    // ------------------------------------------------------------------------

    @Test
    void anEvaluatorTransformsTheResults() {
        TraversalResult<String, String> r =
                Traversals.bfsEvaluating("A", ADJACENCY, v -> Optional.of(v.toLowerCase()));
        assertEquals(List.of("a", "b", "c", "d"), r.results());
        assertEquals(List.of("A", "B", "C", "D"), r.visited());
    }

    @Test
    void anEvaluatorReturningEmptyFiltersWithoutAffectingTheTraversal() {
        TraversalResult<String, Integer> r = Traversals.bfsEvaluating(
                "A", ADJACENCY,
                v -> v.equals("A") || v.equals("D") ? Optional.empty() : Optional.of(v.length()));
        // Every node is still visited; only the results are filtered.
        assertEquals(List.of("A", "B", "C", "D"), r.visited());
        assertEquals(List.of(1, 1), r.results());
    }

    // ------------------------------------------------------------------------
    // Tree traversal
    // ------------------------------------------------------------------------

    /** Root "tree" has one child, 1; 1 has children 2 and 3. */
    private static final NeighbourFunction<String, Integer> ROOT_CHILDREN =
            root -> root.equals("tree") ? List.of(1) : List.of();

    private static final NeighbourFunction<Integer, Integer> TREE_CHILDREN =
            n -> n == 1 ? List.of(2, 3) : List.of();

    @Test
    void traverseTreeDoesNotVisitTheRootItself() {
        TraversalResult<Integer, Integer> r =
                Traversals.traverseTree("tree", ROOT_CHILDREN, TREE_CHILDREN, Order.PRE);
        assertEquals(List.of(1, 2, 3), r.visited());
    }

    @Test
    void traverseTreePostOrderRecordsChildrenFirst() {
        TraversalResult<Integer, Integer> r =
                Traversals.traverseTree("tree", ROOT_CHILDREN, TREE_CHILDREN, Order.POST);
        assertEquals(List.of(2, 3, 1), r.visited());
    }

    @Test
    void traverseTreeProducesNoCameFromMap() {
        TraversalResult<Integer, Integer> r =
                Traversals.traverseTree("tree", ROOT_CHILDREN, TREE_CHILDREN, Order.PRE);
        // The engine does not record came-from for a tree: the parent is evident from its shape.
        assertTrue(r.cameFrom().isEmpty(), "tree traversal should record no came-from");
    }

    @Test
    void traverseTreeHaltsAtTheGoal() {
        TraversalResult<Integer, Integer> r = Traversals.traverseTree(
                "tree", ROOT_CHILDREN, TREE_CHILDREN, Order.PRE, n -> n == 2);
        assertEquals(List.of(1, 2), r.visited());
    }

    @Test
    void traverseTreeEvaluates() {
        TraversalResult<Integer, String> r = Traversals.traverseTreeEvaluating(
                "tree", ROOT_CHILDREN, TREE_CHILDREN, Order.PRE, n -> Optional.of("n" + n));
        assertEquals(List.of("n1", "n2", "n3"), r.results());
    }

    // ------------------------------------------------------------------------
    // Result object
    // ------------------------------------------------------------------------

    @Test
    void theResultCollectionsAreUnmodifiable() {
        TraversalResult<String, String> r = Traversals.bfs("A", ADJACENCY);
        assertThrows(UnsupportedOperationException.class, () -> r.visited().add("X"));
        assertThrows(UnsupportedOperationException.class, () -> r.results().add("X"));
        assertThrows(UnsupportedOperationException.class, () -> r.cameFrom().put("X", "Y"));
    }

    @Test
    void aNodeWithNoNeighboursTraversesToItselfAlone() {
        TraversalResult<String, String> r = Traversals.bfs("Z", v -> List.of());
        assertEquals(List.of("Z"), r.visited());
        assertTrue(r.cameFrom().isEmpty(), "a lone node was reached from nowhere");
    }
}
