# Visitor — Design Document

## Overview

This document captures the design decisions behind the Visitor library — a purely
functional, typeclass-driven graph and tree traversal engine for Scala 3. It covers
the priority queue hierarchy, the `CostUpdate` typeclass, the `Zero`/`Monoid`
hierarchy, the came-from mechanism, and the tuple frontier approach used by weighted
traversals.

For the design of the graph algorithms built on top of Visitor (Dijkstra, Prim,
Kruskal, Kosaraju, etc.) see `GraphTraversalDesign.md` in the Gryphon repository.

---

## Current State (Visitor V1.6.0)

- `Traversal` engine with `traverse`, `dfs`, `bfs`, `bestFirst`, `bestFirstMax`,
  `bestFirstWeighted`
- Three-type priority queue hierarchy: `BinaryHeap` → `PrioQueue` → `IndexedPrioQueue`
  (see §Priority Queue Design Decision below)
- `Frontier[F[_]]` typeclass with implementations for `Stack`, `Queue`, `PrioQueue`,
  and `IndexedPrioQueue`
- `CostUpdate[W, F[_]]` typeclass with default no-op given
- `TupleVisitedSet[(E,V)]` and `given [E, V]: VisitedSet[(E, V)]` — supports
  weighted tuple frontiers for Dijkstra/Prim
- `Evaluable[V, R]`, `Neighbours[V, V]`, `VisitedSet[V]` typeclasses
- `Zero[A]` typeclass (identity only); `Monoid[A] extends Zero[A]` (adds `combine`)
- `given` instances for `Zero` and `Monoid` for `Int`, `Long`, `Double`, `Float`;
  derived `given [A: Monoid]: Zero[A]`
- `Visitor` trait with `visit` and `discover(v, cameFrom)` (default no-op)
- `JournaledVisitor` with optional `CameFromJournal[V]`; `cameFrom: Option[Map[V,V]]`
  accessor; factory methods `withQueueJournalAndCameFrom` and
  `withListJournalAndCameFrom`
- `CameFromJournal[V]` — first-discovery-wins; start vertex absent
- `ListJournal` (prepend) and `QueueJournal` (append/FIFO)
- `discover` called in `traverse` and `dfs` at the point of frontier expansion
- `Tracer[V]` typeclass for optional debug tracing at configurable verbosity levels

---

## File Structure

| File | Contents |
|------|----------|
| `Behaviours.scala` | `Zero`, `Monoid`, `Evaluable`, `Neighbours`, `VisitedSet`, `Frontier`, `CostUpdate`; all given instances |
| `Visitor.scala` | `Visitor` trait, `JournaledVisitor` case class and companion |
| `Journal.scala` | `Appendable`, `Journal`, `IterableJournal`, `ListJournal`, `QueueJournal`, `CameFromJournal` |
| `Traversal.scala` | `Traversal` object, `DfsOrder` enum |
| `PrioQueue.scala` | `BinaryHeap`, `PrioQueue`, `IndexedPrioQueue` |
| `Tracer.scala` | `Tracer[V]` typeclass |

---

## Priority Queue Design Decision

### Context

The original `PrioQueue` was a single class backed by an immutable `BinaryHeap`.
To support `decreaseKey` for Dijkstra/Prim, we needed to add an index map
(`Map[T, Int]`: element → heap-array position). Several design options were considered.

### Options Considered

#### Option A: Single `PrioQueue` with embedded index (rejected)

Add `Map[T, Int]` directly to `BinaryHeap` / `PrioQueue`. Deduplicate on `insert`
(no-op if element already present).

**Pro:** Simple, single type, minimal API surface.

**Con:** Breaks general-purpose use — a priority queue is a valid ADT that permits
duplicates. The deduplication guard (`if index.contains(t) then this`) caused the
duplicate-elements test to fail with `removeMin on empty heap`. The `index` map has
ambiguous semantics when duplicates exist (tracks only the most-recently-inserted
position). Mixes two concerns (data structure policy and structural mechanics) in
one class.

#### Option B: Two types — `BinaryHeap` (pure) + `PrioQueue` (with index) (rejected)

Move the index into `PrioQueue`, keep `BinaryHeap` pure. `PrioQueue` rebuilds the
index via `heap.data.zipWithIndex.toMap` (O(n)) after each structural operation.

**Pro:** Clean separation of data structure from policy. `BinaryHeap` becomes simple.

**Con:** Still conflates two different `PrioQueue` use cases: general-purpose
(duplicates OK) and indexed (no duplicates, `decreaseKey` needed). A single
`PrioQueue` type cannot honestly serve both.

#### Option C: Three types — `BinaryHeap` + `PrioQueue` + `IndexedPrioQueue` ✓ CHOSEN

```
BinaryHeap[T]        — pure data structure; sift-up/down, insert, removeMin only
PrioQueue[T]         — ADT; delegates to heap; duplicates permitted; no index
IndexedPrioQueue[T]  — ADT; adds Map[T,Int] index; no duplicates; decreaseKey/contains
```

**Pro:** Each type is an honest ADT with clear invariants. Pedagogically valuable —
directly illustrates the textbook principle that a priority queue ADT delegates to
a binary heap. `BinaryHeap` is visibly a pure data structure. `PrioQueue` and
`IndexedPrioQueue` are visibly policy layers. `Frontier[PrioQueue]` (for
`bestFirst`/`bestFirstMax`) and `Frontier[IndexedPrioQueue]` (for
`bestFirstWeighted`) are clearly distinct at the call site.

**Con:** Three types instead of one; two `Frontier` given instances;
`bestFirstWeighted` uses `IndexedPrioQueue` while `bestFirst`/`bestFirstMax` use
`PrioQueue` — callers must choose the right entry point.

**Why chosen:** The teaching context makes the clean three-way separation a feature,
not a cost. The type-level distinction between a plain priority queue and an indexed
one also enforces correct usage at compile time.

### Implementation Detail: Index Rebuild Strategy

`IndexedPrioQueue` rebuilds its `Map[T, Int]` from scratch after each `offer` or
`take` via `heap.data.zipWithIndex.toMap` — O(n). An alternative would be to have
`BinaryHeap` expose position information from sift operations so the index could be
maintained incrementally (O(log n)). This was rejected because:

- The O(n) rebuild cost is dominated by the O(E log V) traversal cost in Dijkstra/Prim
- Incremental maintenance would require threading index-awareness back into
  `BinaryHeap`, partially defeating the purpose of the split
- O(n) rebuild keeps `BinaryHeap` pure and the overall design simple

`decreaseKey` in `IndexedPrioQueue` removes the old element by patching the raw
array, rebuilds the heap from scratch via a fold of `insert`s, then inserts the new
element. This is O(n log n) rather than O(log n) but correct. Can be optimised later
if profiling ever justifies it.

---

## CostUpdate Typeclass

### Purpose

After a node is settled (dequeued and marked visited) and its neighbours offered to
the frontier, `CostUpdate` provides a hook for updating the priorities of frontier
entries that have improved. This keeps all domain knowledge (cost maps, edge weights,
the notion of "improvement") out of `Traversal` itself.

```scala
trait CostUpdate[W, F[_]]:
  def update(frontier: F[W], w: W): F[W]

// Default no-op — resolved automatically for DFS and BFS
given [W, F[_]]: CostUpdate[W, F] with
  def update(frontier: F[W], w: W): F[W] = frontier
```

`Traversal.traverse` calls `cu.update` in two places:
1. After the seed (`cu.update(seedOffered, start)`)
2. After each `offerAll` in the main loop (`cu.update(offered, node)`)

### Relationship to IndexedPrioQueue

For Dijkstra and Prim, the consuming library (`WeightedTraversal` in Gryphon)
provides a `given CostUpdate[W, IndexedPrioQueue]` (where `W = (E, V)`) that calls
`decreaseKey` for any frontier entry whose cost has improved since it was first
offered. The `CostUpdate` instance closes over two mutable maps:
- `pred: mutable.Map[V, Edge]` — the cheapest known incoming edge per vertex
- `bestCost: mutable.Map[V, E]` — current best known cost per vertex, needed
  to locate the old frontier entry for `decreaseKey`

### Bookkeeping Ownership

A critical design invariant: **`Neighbours` is pure — it generates `(cost, vertex)`
tuples with no side effects**. All writes to `bestCost` and `pred` are owned
exclusively by `CostUpdate.update`, which handles two cases:
- `None` — first discovery of vertex `w`; `offerAll` has already offered the
  tuple, `CostUpdate` records `bestCost(w)` and `pred(w)`
- `Some(oldCost)` with improvement and `w` still in frontier — calls `decreaseKey`
  and updates both maps

This separation was discovered through debugging: when `Neighbours` also wrote to
`bestCost`, the `CostUpdate` `decreaseKey` check would find a stale `oldCost`
value and either miss improvements or look up the wrong frontier entry.

---

## Zero and Monoid Typeclass Hierarchy

### Motivation

Weighted traversals need different things from their cost type:

- **Prim** needs only an identity element (`Zero.identity`) to seed the initial
  frontier cost. It compares edge weights but never accumulates them.
- **Dijkstra** needs both identity (`Zero.identity` as the start cost) and
  combination (`Monoid.combine` for cumulative path cost accumulation).

Making both use `Monoid[E]` was dishonest for Prim — it required a stub `combine`
that returned `x` unchanged and was never called. The `Zero`/`Monoid` hierarchy
makes the requirements explicit and honest.

### Design

```scala
trait Zero[A]:
  def identity: A

trait Monoid[A] extends Zero[A]:
  def combine(x: A, y: A): A
```

`given` instances for `Zero[Int]`, `Zero[Long]`, `Zero[Double]`, `Zero[Float]` are
provided separately from the `Monoid` instances. A derived given:

```scala
given [A: Monoid]: Zero[A] with
  def identity: A = summon[Monoid[A]].identity
```

allows any `Monoid[A]` in scope to satisfy a `Zero[A]` requirement. This is needed
because Scala 3's given resolution does not automatically project a `given Monoid[A]`
to satisfy `given Zero[A]` without this bridge.

### Context Bounds

| Algorithm | Context bound | Reason |
|-----------|--------------|--------|
| `WeightedTraversal` | `E: {Zero, Ordering}` | Base class; only needs identity and ordering |
| `PrimTraversal` | `E: {Zero, Ordering}` | Compares but never combines costs |
| `DijkstraTraversal` | `E: {Monoid, Ordering}` | Accumulates cumulative path costs |
| `MST.prim` | `E: {Zero, Ordering}` | Entry point mirrors `PrimTraversal` |
| `ShortestPaths.dijkstra` | `E: {Monoid, Ordering}` | Entry point mirrors `DijkstraTraversal` |

---

## Came-From Mechanism

### Motivation

BFS and DFS traversals are often used to find paths between vertices. The standard
result is a came-from map: for each discovered vertex `v`, the map records which
vertex was being visited when `v` was first added to the frontier. Walking the map
backwards from any vertex reconstructs the path to the start.

This is sometimes called a "parent map" but "came-from" is more accurate — the
relationship is an artifact of traversal order, not a structural property of the
graph.

### Design

Two events are distinguished in `Visitor`:
- `visit(v)` — called at *settle* time (when a vertex is dequeued/popped).
- `discover(v, cameFrom)` — called at *discovery* time (when a vertex is first
  seen as a neighbour), before it is added to the frontier.

`discover` has a default no-op implementation so existing visitors are unaffected.
`JournaledVisitor` overrides `discover` to record `(v, cameFrom)` in its
`CameFromJournal` when one is present.

`CameFromJournal.append` uses first-discovery-wins semantics:
```scala
def append(x: (V, V)): CameFromJournal[V] =
  if map.contains(x._1) then this  // already discovered — keep first
  else copy(map + x)
```

For BFS this guarantees the came-from vertex is always from the correct BFS level
(shortest-path predecessor). For DFS it records the tree-edge predecessor.

The start vertex is absent from the map — it has no predecessor.

### `discover` Call Sites in `Traversal`

- **`traverse`** — called for each unvisited neighbour before `offerAll`, both at
  the seed step and in the main loop.
- **`dfs`** — called for each unvisited child at expand time (the `Left(node)` branch),
  before child frames are pushed to the stack. Also called for the start vertex's
  children before the loop begins.
- **`traverseTree`** — `discover` is NOT called; tree structure implies came-from.
- **`bestFirstWeighted`** — `discover` is NOT called; came-from is tracked via the
  `pred` map in `WeightedTraversal.CostUpdate`, which owns all predecessor bookkeeping
  for weighted traversals.

---

## Tuple Frontier Approach for Dijkstra/Prim

### Motivation

The original Dijkstra/Prim used `Ordering[V]` derived from a mutable cost map,
with the frontier holding plain `V` vertices. This was replaced with an explicit
`(E, V)` tuple frontier:

- **Correctness:** Cost is encoded in the frontier element itself — no implicit
  dependency on external mutable state for ordering.
- **Purity:** `Ordering[(E, V)]` is `Ordering.by(_._1)`, derived purely from
  `E`'s `Ordering`. No mutable state leaks into the ordering.
- **Clarity:** The frontier element type makes the cost explicit at every step.

### Design

The frontier element type is `W = (E, V)` where `E` is the cost type and `V` is
the vertex type.

**`Neighbours[(E, V), (E, V)]`** — pure cost expansion, no side effects.

**`VisitedSet[(E, V)]`** — tracks visited-ness on `V` alone, ignoring the cost
component, so stale `(higherCost, v)` frontier entries are correctly skipped:
```scala
given [E, V]: VisitedSet[(E, V)] = TupleVisitedSet(Set.empty[V])
```

The external `TraversalResult[V, Edge[V, E]]` API is unchanged — tuple unwrapping
happens inside `DijkstraTraversal.run` / `PrimTraversal.run`.

### Lazy Evaluation Bug (Heisenbug)

During implementation, a subtle Scala lazy evaluation bug was discovered. Inside
a `given Neighbours` that returns an `Iterator`, tuple pattern matching:

```scala
val (accCost, v) = ev  // WRONG — can generate lazy binding in Iterator context
```

caused `accCost` to not be materialised at the point of destructuring but at the
point of use inside the `map` lambda — by which time `ev` may have been rebound.
This caused `en.plus(accCost, e.attribute)` to return just `e.attribute` (as if
`accCost = zero`), producing wrong cumulative costs in Dijkstra.

The bug was discovered because adding a `println(s"accCost=$accCost")` inside
the lambda forced strict evaluation as a side effect and made the bug disappear —
a classic Heisenbug.

**Fix:** Always use strict `val` extraction with explicit type ascriptions when
destructuring tuples inside `given` instances that return lazy collections:

```scala
val accCost: E = ev._1  // CORRECT — strict binding
val v: V       = ev._2
```

---

## Future Work

- **`IndexedPrioQueue.decreaseKey` performance** — currently O(n log n); could be
  O(log n) by having `BinaryHeap` expose position information from sift operations.
  Acceptable for current graph sizes; revisit if profiling justifies it.

- **In-order DFS** — for binary trees only; requires a `BinaryNeighbours[H,V]`
  typeclass yielding exactly `(left, right)` and a separate `dfsInOrder` entry
  point in `Traversal`.

- **Cats integration** — `Monoid[A]` was added as a home-grown typeclass to avoid
  the Cats dependency. The remaining question is whether to provide `Monoid[J]` on
  `Journal` and `Monoid[VisitedSet[V]]` as natural entry points for parallel
  traversal.

- **Parallel traversal** — splitting the frontier and merging results. Would require
  `Monoid[J]` and `Monoid[VisitedSet[V]]` — a natural consequence of Cats
  integration above.