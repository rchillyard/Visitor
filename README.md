# Visitor

![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.phasmidsoftware/visitor_3/badge.svg?color=blue)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e898359ab5574493b4e607aa8267e9fc)](https://app.codacy.com/gh/rchillyard/Visitor/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/rchillyard/Visitor/tree/main.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/gh/rchillyard/Visitor/tree/main)
![GitHub Top Languages](https://img.shields.io/github/languages/top/rchillyard/Visitor)
![GitHub](https://img.shields.io/github/license/rchillyard/Visitor)
![GitHub last commit](https://img.shields.io/github/last-commit/rchillyard/Visitor)
![GitHub issues](https://img.shields.io/github/issues-raw/rchillyard/Visitor)
![GitHub issues by-label](https://img.shields.io/github/issues/rchillyard/Visitor/bug)

A purely functional, typeclass-driven graph and tree traversal library for Scala 3.

## Core Idea

The central design principle is a strict separation of concerns.
Traversal algorithms (BFS, DFS, best-first, Dijkstra-style weighted traversal) know nothing about the domain — they only know that a data structure has neighbours.
Everything derived from visiting a node is captured in a `Visitor`, which accumulates results immutably into a `Journal`.

This is achieved entirely through typeclasses rather than inheritance hierarchies, making the library highly composable and easy to extend.

## Architecture

The library is built from seven orthogonal typeclasses:

### `Evaluable[V, R]`

Answers: *what do we want to know about a node?*

```scala
trait Evaluable[V, R]:
  def evaluate(v: V): Option[R]
```

Domain knowledge lives here.
The traversal engine is entirely ignorant of it.

### `Neighbours[H, V]`

Answers: *how do we find adjacent nodes?*

```scala
trait Neighbours[H, V]:
  def neighbours(h: H): Iterator[V]
```

`H` is the structure type (e.g. a graph, a tree root), `V` is the node type.
For homogeneous graphs use the alias `GraphNeighbours[V] = Neighbours[V, V]`.
For trees, `H` and `V` can differ (e.g. `H = Tree[A]`, `V = A`).
American English aliases `Neighbors` and `GraphNeighbors` are also provided.

### `VisitedSet[V]`

Answers: *have we been here before?*

```scala
trait VisitedSet[V]:
  def isVisited(v: V): Boolean
  def markVisited(v: V): VisitedSet[V]
```

Immutable and purely functional.
A default `given` instance backed by an immutable `Set` is provided automatically.

For weighted traversals where the frontier holds `(cost, vertex)` tuples, a
`TupleVisitedSet[E, V]` is also provided. It tracks visited-ness on the vertex
component `V` alone — stale `(higherCost, v)` frontier entries are correctly
recognised as already visited:

```scala
given [E, V]: VisitedSet[(E, V)] = TupleVisitedSet(Set.empty[V])
```

### `Frontier[F[_]]`

Answers: *in what order do we explore?*

```scala
trait Frontier[F[_]]:
  def empty[T]: F[T]
  def offer[T](f: F[T])(t: T): F[T]
  def take[T](f: F[T]): (T, F[T])
  def isEmpty[T](f: F[T]): Boolean
```

This is the key to getting BFS, DFS, and best-first traversal from a single algorithm.
Four `given` instances are provided:

| Instance | Type | Traversal order |
|---|---|---|
| `Frontier[Queue]` | FIFO queue | Breadth-first (BFS) |
| `Frontier[Stack]` | LIFO stack (as `List`) | Depth-first (DFS) |
| `Frontier[PrioQueue]` | Indexed binary min-heap | Best-first (duplicates permitted) |
| `Frontier[IndexedPrioQueue]` | Indexed binary min-heap | Best-first with `decreaseKey` (no duplicates) |

### `CostUpdate[W, F[_]]`

Answers: *does any frontier entry need its priority updated after a node is settled?*

```scala
trait CostUpdate[W, F[_]]:
  def update(frontier: F[W], w: W): F[W]
```

A default no-op `given` is provided, resolved automatically for DFS and BFS.
For Dijkstra and Prim, the consuming library provides a `given CostUpdate[W, IndexedPrioQueue]`
that calls `decreaseKey` whenever a cheaper path to a frontier vertex is found.
This keeps all domain knowledge out of `Traversal` itself.

### `Zero[A]`

Answers: *what is the identity cost?*

```scala
trait Zero[A]:
  def identity: A
```

The minimal requirement for weighted traversals that need a seed cost but do not
accumulate costs along a path — specifically Prim's MST algorithm. `given` instances
are provided for `Int`, `Long`, `Double`, and `Float`.

`Monoid[A]` extends `Zero[A]`, so any `given Monoid[A]` automatically satisfies
`Zero[A]`. A derived `given [A: Monoid]: Zero[A]` is also provided for contexts
where only `Monoid` is in scope.

### `Monoid[A]`

Answers: *what is the identity cost, and how do costs combine?*

```scala
trait Monoid[A] extends Zero[A]:
  def combine(x: A, y: A): A
```

Extends `Zero[A]` with an associative binary operation. Mirrors the Cats `Monoid`
typeclass without the Cats dependency. Used by Dijkstra to accumulate path costs.
`given` instances are provided for `Int`, `Long`, `Double`, and `Float`.

The distinction between `Zero` and `Monoid` is intentional and pedagogically
significant: Prim's MST algorithm compares edge weights but never combines them,
so requiring `Monoid` for Prim would be dishonest. The correct context bound for
weighted traversals is `E: {Zero, Ordering}` for Prim and `E: {Monoid, Ordering}`
for Dijkstra.

### `Visitor[V, R, J]`

Answers: *where do the results go, and how do we track came-from relationships?*

```scala
trait Visitor[V, R, J <: Appendable[(V, Option[R])]]:
  def journal: J
  def visit(v: V)(using ev: Evaluable[V, R]): Visitor[V, R, J]
  def discover(v: V, cameFrom: V): Visitor[V, R, J] = this  // default no-op
  def result: J
```

Two events are distinguished:
- `visit(v)` — called when a vertex is *settled* (dequeued/popped and processed).
- `discover(v, cameFrom)` — called when a vertex is first *discovered* as a neighbour
  of `cameFrom`, before it is added to the frontier. Default is a no-op.

The canonical implementation is `JournaledVisitor`. When constructed with a
`CameFromJournal`, it overrides `discover` to record came-from relationships.

## Priority Queue Hierarchy

Visitor provides a three-type priority queue hierarchy:

- **`BinaryHeap[T]`** — pure data structure; array-based min-heap; `insert` and `removeMin` only; duplicates permitted.
- **`PrioQueue[T]`** — ADT backed by `BinaryHeap`; captures `Ordering[T]` at construction; duplicates permitted; no index.
- **`IndexedPrioQueue[T]`** — extends `PrioQueue` with a `Map[T, Int]` position index; enforces one-entry-per-key; adds `decreaseKey` and `contains`.

`Ordering[T]` is captured at construction time in both `PrioQueue` and `IndexedPrioQueue`, so `Frontier[PrioQueue]` and `Frontier[IndexedPrioQueue]` remain fully polymorphic — no `Ordering` context is needed at `offer`/`take` call sites.

Use `PrioQueue` for general best-first traversal. Use `IndexedPrioQueue` when you need `decreaseKey` — i.e. for Dijkstra and Prim via `bestFirstWeighted`.

## Traversal Engine

`Traversal` is the single traversal engine.
Its internal `loop` is a tail-recursive function shared across all traversal strategies.

```scala
object Traversal:
  def bfs[V, R, J](start: V, visitor: Visitor[V, R, J], goal: V => Boolean = _ => false)
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def dfs[V, R, J](start: V, visitor: Visitor[V, R, J],
                   order: DfsOrder = DfsOrder.Pre, goal: V => Boolean = _ => false)
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def bestFirst[V: Ordering, R, J](start: V, visitor: Visitor[V, R, J], goal: V => Boolean = _ => false)
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def bestFirstMax[V: Ordering, R, J](start: V, visitor: Visitor[V, R, J], goal: V => Boolean = _ => false)
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def bestFirstWeighted[W: Ordering, R, J](start: W, visitor: Visitor[W, R, J], goal: W => Boolean = _ => false)
      (using Neighbours[W, W], Evaluable[W, R], VisitedSet[W],
             CostUpdate[W, IndexedPrioQueue], IndexedPrioQueue[W]): Visitor[W, R, J]

  def traverseTree[H, V, R, J](start: H, visitor: Visitor[V, R, J],
                                order: DfsOrder = DfsOrder.Pre, goal: V => Boolean = _ => false)
      (using Neighbours[H, V], Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]
```

`bestFirstWeighted` is the entry point for Dijkstra- and Prim-style traversals.
The frontier element type `W` is typically `(E, V)` where `E` is the cost type.
The caller supplies a `given CostUpdate[W, IndexedPrioQueue]` and `given IndexedPrioQueue[W]`.

## Journals

A `Journal` is an immutable, appendable log of visited results. Two standard
implementations and one specialised implementation are provided:

- `ListJournal[X]` — prepends elements; most recently visited node is at the head.
- `QueueJournal[X]` — enqueues elements; preserves visit order (FIFO).
- `CameFromJournal[V]` — records came-from relationships established during traversal.

Note: `ListJournal` prepends, so post-order DFS results appear reversed. Use
`QueueJournal` when visit order must be preserved, or reverse the `ListJournal`
result after traversal.

### `CameFromJournal[V]`

Records the came-from relationship for each discovered vertex: `map(v)` is the
vertex that was being visited when `v` was first added to the frontier. The start
vertex is absent — it has no predecessor.

```scala
case class CameFromJournal[V](map: Map[V, V]):
  def cameFrom(v: V): Option[V]  // None for start vertex or unreachable vertices
  def asMap: Map[V, V]
```

First-discovery-wins semantics: if a vertex is reachable via multiple paths (as
in BFS on a graph with cycles or multiple routes), only the first discovery is
recorded. For BFS this guarantees the came-from vertex is always from the correct
BFS level, giving the shortest-path predecessor.

To enable came-from tracking, use the factory methods on `JournaledVisitor`:

```scala
// BFS with came-from tracking
val visitor = JournaledVisitor.withQueueJournalAndCameFrom[V, R]

// DFS with came-from tracking
val visitor = JournaledVisitor.withListJournalAndCameFrom[V, R]
```

The result's came-from map is accessible via:

```scala
val result = Traversal.bfs(start, visitor)
               .asInstanceOf[JournaledVisitor[V, R, ?]]
result.cameFrom  // Option[Map[V, V]] — None if no CameFromJournal was supplied
```

## Usage Example

```scala
import com.phasmidsoftware.visitor.core.*

// 1. Define your graph
val adjacency = Map(
  1 -> List(2, 3),
  2 -> List(4),
  3 -> List(4, 5),
  4 -> List(),
  5 -> List()
)

// 2. Provide typeclass instances
given GraphNeighbours[Int] with
  def neighbours(h: Int): Iterator[Int] =
    adjacency.getOrElse(h, Nil).iterator

given Evaluable[Int, Int] with
  def evaluate(v: Int): Option[Int] = Some(v)

// 3. Choose a visitor and run
val visitor = JournaledVisitor.withQueueJournal[Int, Int]

val bfsResult     = Traversal.bfs(1, visitor)
val dfsPreResult  = Traversal.dfs(1, visitor)
val dfsPostResult = Traversal.dfs(1, visitor, DfsOrder.Post)
val bestResult    = Traversal.bestFirst(1, visitor)

// Goal-directed search: stop when node 4 is found
val goalResult = Traversal.bfs(1, visitor, goal = _ == 4)

// 4. Inspect results
bfsResult.result.map(_._1).toList     // nodes in BFS order
dfsPreResult.result.map(_._1).toList  // nodes in DFS pre-order
dfsPostResult.result.map(_._1).toList // nodes in DFS post-order (topological sort)
goalResult.result.map(_._1).toList    // nodes visited up to and including node 4
```

## Came-From Example

```scala
import com.phasmidsoftware.visitor.core.*

// Using the same adjacency and given instances as above

// BFS with came-from tracking
val visitor = JournaledVisitor.withQueueJournalAndCameFrom[Int, Int]
val result  = Traversal.bfs(1, visitor)
                .asInstanceOf[JournaledVisitor[Int, Int, ?]]

val cf: Map[Int, Int] = result.cameFrom.get
// cf == Map(2 -> 1, 3 -> 1, 4 -> 2, 5 -> 3)
// Start vertex 1 is absent — it has no predecessor

// Path reconstruction from vertex 5 back to start:
def pathTo(target: Int): List[Int] =
  def walk(v: Int, acc: List[Int]): List[Int] =
    cf.get(v) match
      case None       => v :: acc  // reached start vertex
      case Some(from) => walk(from, v :: acc)
  walk(target, Nil)

pathTo(5)  // List(1, 3, 5)
pathTo(4)  // List(1, 2, 4)
```

## Weighted Traversal Example

For Dijkstra-style traversal, the frontier holds `(cost, vertex)` tuples.
The consuming library provides `Neighbours[(E,V),(E,V)]` for cost accumulation
and `CostUpdate[(E,V), IndexedPrioQueue]` for `decreaseKey`:

```scala
given Ordering[(Double, Int)] = Ordering.by(_._1)

given Neighbours[(Double, Int), (Double, Int)] with
  def neighbours(ev: (Double, Int)): Iterator[(Double, Int)] =
    val accCost: Double = ev._1
    val v: Int          = ev._2
    // expand v's adjacencies, accumulating cost
    adjacency(v).iterator.map(e => (accCost + e.weight, e.to))

given CostUpdate[(Double, Int), IndexedPrioQueue] with
  def update(frontier: IndexedPrioQueue[(Double, Int)], ev: (Double, Int)): IndexedPrioQueue[(Double, Int)] =
    // call decreaseKey for any frontier entry whose cost has improved
    ...

given IndexedPrioQueue[(Double, Int)] = IndexedPrioQueue.empty[(Double, Int)]

val visitor = JournaledVisitor.withQueueJournal[(Double, Int), MyEdge]
val result  = Traversal.bestFirstWeighted((0.0, startVertex), visitor)
```

## Goal Predicate

All traversal methods accept an optional `goal: V => Boolean` parameter for early termination:

```scala
val result = Traversal.bfs(start, visitor, goal = _ == 4)
```

**Semantics:**

- The goal node is always recorded in the journal before traversal halts.
- The goal node's neighbours are never expanded.
- If the start node satisfies the goal, traversal stops immediately with just the start node recorded.
- If the goal is never met, the full graph is traversed (equivalent to `goal = _ => false`, the default).

This works uniformly across `bfs`, `dfs`, `bestFirst`, `bestFirstMax`, and `bestFirstWeighted`.
Combined with `bestFirst` and an appropriate `Ordering`, it gives you A\*-style goal-directed search.

## Immutability

Everything is immutable. Visiting a node yields a new `Visitor`.
Marking a node visited yields a new `VisitedSet`.
The traversal loop threads all state explicitly — there are no `var`s anywhere in the traversal engine.

## Revision History

| Version | Notes |
|---------|-------|
| 0.0.1   | First version |
| 0.0.2   | Added `AutoCloseable` to `Appendable` and `Visitor` |
| 0.0.3   | Added `FunctionMapJournal` |
| 1.0.0   | Complete redesign: typeclass-driven architecture, `Frontier` abstraction, binary heap priority queue, Scala 3 throughout |
| 1.1.0   | Added `DfsOrder` (pre/post-order DFS), `bestFirstMax`, American English type aliases |
| 1.2.0   | Added `goal` predicate for early termination across all traversal methods; `traverseTree` now uses `Either`-stack supporting `DfsOrder` |
| 1.3.0   | Three-type priority queue hierarchy (`BinaryHeap`, `PrioQueue`, `IndexedPrioQueue`); `CostUpdate[W, F[_]]` typeclass; `TupleVisitedSet[(E,V)]`; `bestFirstWeighted` entry point for Dijkstra/Prim |
| 1.4.0   | `Monoid[A]` typeclass with `given` instances for `Int`, `Long`, `Double`, `Float`; replaces `Numeric` in weighted traversal context bounds |
| 1.5.0   | Added `Tracer`; replaced com.novocode with junit.jupiter.xxx |
| 1.6.0   | `Zero[A]` typeclass (identity only); `Monoid[A]` extends `Zero[A]`; `given Zero[A]` instances for numeric types; derived `given [A: Monoid]: Zero[A]`; `CameFromJournal[V]` with first-discovery-wins semantics; `discover(v, cameFrom)` on `Visitor` (default no-op); `JournaledVisitor` records came-from when journal present; `withQueueJournalAndCameFrom` and `withListJournalAndCameFrom` factory methods; `discover` called in `traverse` and `dfs` at discovery time |