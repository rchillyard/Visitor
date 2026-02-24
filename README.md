# Visitor

A purely functional, typeclass-driven graph and tree traversal library for Scala 3.

## Core Idea

The central design principle is a strict separation of concerns. 
Traversal algorithms (BFS, DFS, best-first) know nothing about the domain — they only know that a data structure has neighbours. 
Everything derived from visiting a node is captured in a `Visitor`, which accumulates results immutably into a `Journal`.

This is achieved entirely through typeclasses rather than inheritance hierarchies, making the library highly composable and easy to extend.

## Architecture

The library is built from five orthogonal typeclasses:

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

### `Frontier[F[_]]`
Answers: *in what order do we explore?*

```scala
trait Frontier[F[_]]:
  def empty[T]: F[T]
  def offer[T](f: F[T])(t: T): F[T]
  def take[T](f: F[T]): (T, F[T])
  def isEmpty[T](f: F[T]): Boolean
  def offerAll[T](f: F[T])(ts: List[T]): F[T]
```

This is the key to getting BFS, DFS, and best-first traversal from a single algorithm. 
Three `given` instances are provided:

| Instance | Type | Traversal order |
|---|---|---|
| `Frontier[Queue]` | FIFO queue | Breadth-first (BFS) |
| `Frontier[Stack]` | LIFO stack (as `List`) | Depth-first (DFS) |
| `Frontier[PrioQueue]` | Binary min-heap | Best-first / Dijkstra-style |

### `Visitor[V, R, J]`
Answers: *where do the results go?*

```scala
trait Visitor[V, R, J <: Appendable[(V, Option[R])]]:
  def journal: J
  def visit(v: V)(using ev: Evaluable[V, R]): Visitor[V, R, J]
  def result: J
```

The canonical implementation is `JournaledVisitor`, which appends `(node, Option[result])` pairs to a `Journal` on each visit.

## Traversal Engine

`Traversal` is the single traversal engine. 
BFS, best-first, and `traverseTree` share a common tail-recursive `loop` parameterised over the `Frontier` type. 
DFS uses its own tail-recursive loop based on an `Either`-tagged stack, which supports both pre- and post-order recording.

```scala
object Traversal:
  def bfs[V, R, J <: Appendable[(V, Option[R])]](start: V, visitor: Visitor[V, R, J])
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def dfs[V, R, J <: Appendable[(V, Option[R])]](start: V, visitor: Visitor[V, R, J],
      order: DfsOrder = DfsOrder.Pre)
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def bestFirst[V : Ordering, R, J <: Appendable[(V, Option[R])]](start: V, visitor: Visitor[V, R, J])
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def bestFirstMax[V : Ordering, R, J <: Appendable[(V, Option[R])]](start: V, visitor: Visitor[V, R, J])
      (using Neighbours[V, V], Evaluable[V, R], VisitedSet[V]): Visitor[V, R, J]

  def traverseTree[H, V, R, J <: Appendable[(V, Option[R])], F[_]](start: H, visitor: Visitor[V, R, J])
      (using Neighbours[H, V], Neighbours[V, V], Evaluable[V, R], VisitedSet[V], Frontier[F], F[V]): Visitor[V, R, J]
```

## DFS Ordering

DFS supports two recording orders, controlled by the `DfsOrder` enum:

```scala
enum DfsOrder:
  case Pre, Post
```

| Order | Meaning | Typical use |
|---|---|---|
| `DfsOrder.Pre` (default) | Node recorded before its children are expanded | Tree printing, copying |
| `DfsOrder.Post` | Node recorded after all its descendants | Topological sort, dependency ordering |

The implementation uses an `Either[V, V]`-tagged stack internally:
- `Left(v)` — expand node: mark visited, push children and a record frame
- `Right(v)` — record node: call `visitor.visit(v)`

This avoids any recursion while still capturing the natural unwinding behaviour needed for post-order.

## Journals

A `Journal` is an immutable, appendable log of visited results. Two implementations are provided:

- `ListJournal[X]` — prepends elements; most recently visited node is at the head
- `QueueJournal[X]` — enqueues elements; preserves visit order (FIFO)

Note that `ListJournal` reverses the visit order in its output, which can be useful (e.g. post-order DFS with a `ListJournal` yields reverse post-order directly). 
Use `QueueJournal` when you want results in the order they were visited.

The `FunctionMapJournal` and `MapJournal` from the companion package also satisfy `Appendable` and can be used directly as journals.

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

// 4. Inspect results
bfsResult.result.map(_._1).toList     // nodes in BFS order
dfsPreResult.result.map(_._1).toList  // nodes in DFS pre-order
dfsPostResult.result.map(_._1).toList // nodes in DFS post-order (topological sort)
```

## Priority Queue

The `PrioQueue[T]` type is backed by an immutable binary min-heap (`BinaryHeap`). 
It captures `Ordering[T]` at construction time, so the `Frontier[PrioQueue]` instance remains fully polymorphic. 
Use `PrioQueue.empty[T]` for min-priority (smallest first) and `PrioQueue.emptyMax[T]` for max-priority (largest first). 
For weighted graph traversal, wrap your nodes as `(priority, node)` tuples with an appropriate `Ordering`.

## Immutability

Everything is immutable. 
Visiting a node yields a new `Visitor`. 
Marking a node visited yields a new `VisitedSet`. 
The traversal loops thread all state explicitly — there are no `var`s anywhere in the traversal engine.

## Revision History

| Version | Notes |
|---------|---|
| 0.0.1   | First version |
| 0.0.2   | Added `AutoCloseable` to `Appendable` and `Visitor` |
| 0.0.3   | Added `FunctionMapJournal` |
| 1.0.0   | Complete redesign: typeclass-driven architecture, `Frontier` abstraction, binary heap priority queue, Scala 3 throughout |
| 1.1.0   | Added `DfsOrder` (pre/post-order DFS), `bestFirstMax`, American English type aliases |