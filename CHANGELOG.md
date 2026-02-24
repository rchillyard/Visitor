# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

[1.2.0] — 2026-02-24

### Added
- `goal: V => Boolean` early-termination predicate on all traversal methods (`bfs`, `dfs`,
  `bestFirst`, `bestFirstMax`, `traverseTree`). The goal node is always recorded in the
  journal before traversal halts; its neighbours are never expanded. If the start node
  satisfies the goal, traversal stops immediately.

### Changed
- `traverseTree` no longer requires a `Frontier[F]` type parameter or `given` instances
  for `Frontier[F]` and `F[V]`. It now uses the same `Either`-tagged stack as `dfs`,
  which also gives it native support for `DfsOrder`.
- `traverseTree` gains `order: DfsOrder = DfsOrder.Pre` parameter, supporting both
  pre-order and post-order traversal.

## [1.1.0] — 2026-02-24

### Added
- `DfsOrder` enum (`Pre`, `Post`) controlling whether DFS records a node before or after
  processing its children. `DfsOrder.Pre` is the default; `DfsOrder.Post` is useful for
  topological sort and dependency ordering.
- `bestFirstMax` — best-first traversal dequeuing the largest element first
  (backed by `PrioQueue.emptyMax`).
- American English type aliases `Neighbors[H, V]` and `GraphNeighbors[V]`.

### Changed
- `dfs` now uses an `Either[V, V]`-tagged stack internally rather than delegating to
  `traverse`, enabling pre- and post-order recording without recursion.

## [1.0.0] — 2026-02-24

### Added
- Complete redesign of the library as a purely functional, typeclass-driven architecture.
- Five orthogonal typeclasses: `Evaluable[V, R]`, `Neighbours[H, V]`, `VisitedSet[V]`,
  `Frontier[F[_]]`, and `Visitor[V, R, J]`.
- `Frontier[F[_]]` abstraction with three `given` instances:
    - `Frontier[Queue]` — breadth-first (BFS)
    - `Frontier[Stack]` — depth-first (DFS)
    - `Frontier[PrioQueue]` — best-first / Dijkstra-style
- `PrioQueue[T]` backed by an immutable binary min-heap (`BinaryHeap`), capturing
  `Ordering[T]` at construction time. `PrioQueue.empty[T]` for min-priority,
  `PrioQueue.emptyMax[T]` for max-priority.
- `JournaledVisitor` — canonical `Visitor` implementation accumulating
  `(node, Option[result])` pairs into a `Journal`.
- `ListJournal` and `QueueJournal` — immutable journal implementations.
- `traverseTree` for heterogeneous tree traversal where the root type `H` differs
  from the node type `V`.
- `VisitedSet[V]` with a default `given` instance backed by an immutable `Set`,
  preventing node revisitation in cyclic graphs.
- Scala 3 throughout: `given`/`using`, `enum`, type aliases, context functions.

### Removed
- Previous generation visitor classes (`BfsVisitor`, `DfsVisitor`, `DfsVisitorMapped`,
  `DfsOriginVisitor`) replaced by the unified `Traversal` engine.
- `Pre`/`Post`/`In`/`SelfVisit` message dispatch pattern replaced by `DfsOrder` and
  the `Frontier` abstraction.
- `MinPQ`/`MaxPQ`/`PQLike` replaced by `PrioQueue` backed by `BinaryHeap`.

## [0.0.3] — pre-redesign

### Added
- `FunctionMapJournal` — a journal backed by a map and a key-value computing function.

## [0.0.2] — pre-redesign

### Added
- `AutoCloseable` to `Appendable` and `Visitor`.

## [0.0.1] — pre-redesign

### Added
- Initial version: `BfsVisitor`, `DfsVisitor`, `Journal`, `Appendable`, `MapJournal`,
  `ListJournal`, `QueueJournal`, `NonAppendable`, `AppendableWriter`.
