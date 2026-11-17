# Deferred Issues

A running list of design decisions, features, and tasks that have been explicitly
postponed for future attention.

## Design / Features

1. **In-order DFS**
   For binary trees only. Requires a `BinaryNeighbours[H, V]` typeclass yielding
   exactly `(left, right)` and a separate `dfsInOrder` entry point.

2. ~~**Java API**~~ — **DONE in 1.8.0.** `com.phasmidsoftware.visitor.java` covers `bfs`, `dfs`
   and `traverseTree`. The interop story turned out to be straightforward: the Java-facing types
   (`NeighbourFunction`, `Evaluator`) are ordinary functional interfaces, and a package-private
   Scala bridge turns them into the `given` instances the engine wants. What is *not* yet
   exposed is the weighted family (`bestFirst`, `bestFirstMax`, `bestFirstWeighted`), which
   needs an `Ordering` and a `CostUpdate` from Java — that is the remaining piece.

3. **Cats integration**
   Settled for the cost type classes: `Zero` and `Monoid` were **removed** in 1.8.0, not
   migrated. Nothing in Visitor consumed them, so taking on `cats-kernel` at the base of the
   Phasmid stack would have bought an abstraction no code here uses
   ([#21](https://github.com/rchillyard/Visitor/issues/21)). The question that remains open is
   `Monoid[J]` on `Journal` and `Monoid[VisitedSet[V]]` — those *would* be consumed, for
   parallel traversal below, so they would justify the dependency on their own merits.

4. **Parallel traversal**
   Splitting the frontier and merging results. Would require `Monoid[J]` and
   `Monoid[VisitedSet[V]]`, which is the real argument for a `cats-kernel` dependency.

5. **`Monoid` vs `Ordering` relationship** — *historical; both types are gone as of 1.8.0.*
   Considered making `Monoid[A] extends Ordering[A]` to allow a single context
   bound `E: Monoid` for weighted traversals. Rejected: a monoid does not imply
   an ordering (`String` concatenation is a valid monoid with no natural ordering),
   and an ordering does not imply a monoid. The two context bounds `E: {Monoid, Ordering}`
   are the honest design. Recorded here in case the question resurfaces — it applies equally
   to `cats.kernel.Monoid`, which a consumer would now use instead.
   See also [Issue #9](https://github.com/rchillyard/Visitor/issues/9#issue-4240254773).
6. **CameFrom pointers in the Visitor engine.** Adding a `CameFromJournal[V]` to
  Visitor would allow `GraphTraversal.bfs` / `dfs` to delegate fully to the
  Scala engine. This is a Visitor library change, not a Gryphon change.
  See [Issue #10](https://github.com/rchillyard/Visitor/issues/10#issue-4240254800)


## Code / Architecture

6. **Visiting the root in `traverseTree`**
   The root `H` is currently never visited (no `Evaluable[H, R]` in scope).
   If visiting the root is needed, a variant taking an explicit `Evaluable[H, R]`
   parameter should be provided as a separate entry point.

7. **Old `com.phasmidsoftware.visitor` package**
   `AppendableWriter` and `NonAppendable` have no equivalents in `core`. Decide
   whether they migrate into `core`, remain in a companion package, or are dropped.

8. **`IndexedPrioQueue.decreaseKey` performance**
   Currently O(n log n): removes the old element by patching the array, rebuilds
   the heap via a fold, then inserts the new element. Could be O(log n) by having
   `BinaryHeap` expose position information from sift operations so the index can
   be maintained incrementally. Acceptable for current graph sizes; revisit if
   profiling ever justifies it. Target: Visitor V1.3.1 (patch, no API change).

9. **`TraversalResult` vertex/edge duality**
   `VertexTraversalResult` has an `edgeTraverse` stub that throws `GraphException`.
   The design intends `TraversalResult` to support both vertex-keyed and edge-keyed
   results, but only one is ever populated. The right solution is probably two
   distinct result types rather than a single type with dead methods. Revisit when
   Kruskal's MST (edge-keyed result) is implemented.

## Gryphon — Future Work

10. **Kosaraju's Strongly Connected Components**
    Two DFS passes: first on the original graph (post-order), then on the reversed
    graph. Requires `DirectedGraph.reverse` (not yet implemented). Lives alongside
    `ConnectedComponents` in the `traverse` package.

11. **Kruskal's MST**
    Requires Union-Find. `UnionFindSpec` is currently entirely commented out;
    `WeightedUnionFind` is in the attic. Does not use the `Traversal` engine —
    it is edge-sorting plus Union-Find.

12. **Verify Sedgewick & Wayne book coverage**
    Systematically check that all graph algorithms from the course textbook are
    implemented in Gryphon.

13. **Merge `DijkstraTraversal` / `PrimTraversal` further**
    Already merged into `WeightedTraversal`. The remaining duplication is in
    `filterEdge` — Dijkstra admits only `AttributedDirectedEdge`, Prim admits all
    edges. Consider whether a more general edge-filter abstraction is warranted,
    or whether the current two-line override is clean enough as-is.

## Testing

14. **More complex graph tests**
    The more complex graph-based tests live in Gryphon, not Visitor. Importing them
    directly would create an upward dependency (Visitor knowing about Gryphon), which
    contradicts the architecture. Instead, those tests naturally exercise the Visitor
    traversal engine as part of Gryphon's own test suite once Gryphon depends on Visitor.
    The only candidates for migration would be pure graph-structural tests (cycle detection,
    disconnected components, deep graphs stressing tail-recursion) that have no dependency
    on Gryphon-specific types.