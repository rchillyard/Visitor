# Deferred Issues

A running list of design decisions, features, and tasks that have been explicitly
postponed for future attention.

## Design / Features

1. **In-order DFS**
   For binary trees only. Requires a `BinaryNeighbours[H, V]` typeclass yielding
   exactly `(left, right)` and a separate `dfsInOrder` entry point.

2. **Java API**
   A clean Java facade over the typeclass machinery. Waiting for the Scala API
   to fully stabilise before tackling the Scala 3 / Java interop story for
   `given`/`using`.

3. **Cats integration**
   `Monoid` on `Journal` and `VisitedSet` as the natural entry point. Especially
   relevant if parallel traversal is added. Intentionally kept separate for now
   to avoid the dependency.

4. **Parallel traversal**
   Splitting the frontier and merging results. Would require `Monoid[J]` and
   `Monoid[VisitedSet[V]]` — a natural consequence of the Cats integration above.

## Code / Architecture

5. **Visiting the root in `traverseTree`**
   The root `H` is currently never visited (no `Evaluable[H, R]` in scope).
   If visiting the root is needed, a variant taking an explicit `Evaluable[H, R]`
   parameter should be provided as a separate entry point.

6. **`PrioQueue` and weighted graphs**
   `BinaryHeap` allows duplicate nodes, which is correct for general use. For
   weighted graph traversal with `(priority, node)` tuples, a decrease-key
   operation or deduplication strategy may be needed.

7. **Old `com.phasmidsoftware.visitor` package**
   `AppendableWriter` and `NonAppendable` have no equivalents in `core`. Decide
   whether they migrate into `core`, remain in a companion package, or are dropped.

8. **Gryphon integration**
   Wiring up Gryphon's graph types as `given Neighbours` and `given Evaluable`
   instances. This will be the first real-world validation of the typeclass design.
   Gryphon should depend on Visitor, not the other way around.

## Testing

9. **More complex graph tests**
   The more complex graph-based tests live in Gryphon, not Visitor. Importing them
   directly would create an upward dependency (Visitor knowing about Gryphon), which
   contradicts the architecture. Instead, those tests naturally exercise the Visitor
   traversal engine as part of Gryphon's own test suite once Gryphon depends on Visitor.
   The only candidates for migration would be pure graph-structural tests (cycle detection,
   disconnected components, deep graphs stressing tail-recursion) that have no dependency
   on Gryphon-specific types.