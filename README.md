# Visitor

[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.phasmidsoftware/visitor_3/badge.svg?color=blue)](https://central.sonatype.com/artifact/com.phasmidsoftware/visitor_3)
[![API Docs](https://img.shields.io/badge/api-docs-blue)](https://rchillyard.github.io/Visitor/latest/api/)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e898359ab5574493b4e607aa8267e9fc)](https://app.codacy.com/gh/rchillyard/Visitor/dashboard)
[![Codacy Coverage](https://app.codacy.com/project/badge/Coverage/e898359ab5574493b4e607aa8267e9fc)](https://app.codacy.com/gh/rchillyard/Visitor/dashboard)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/rchillyard/Visitor/tree/main.svg?style=shield)](https://dl.circleci.com/status-badge/redirect/gh/rchillyard/Visitor/tree/main)
[![GitHub Top Languages](https://img.shields.io/github/languages/top/rchillyard/Visitor)](https://github.com/rchillyard/Visitor)
[![GitHub](https://img.shields.io/github/license/rchillyard/Visitor)](https://github.com/rchillyard/Visitor/blob/main/LICENSE)
[![GitHub last commit](https://img.shields.io/github/last-commit/rchillyard/Visitor)](https://github.com/rchillyard/Visitor/commits/main)
[![GitHub issues](https://img.shields.io/github/issues-raw/rchillyard/Visitor)](https://github.com/rchillyard/Visitor/issues)
[![GitHub issues by-label](https://img.shields.io/github/issues/rchillyard/Visitor/bug)](https://github.com/rchillyard/Visitor/issues?q=is%3Aissue+is%3Aopen+label%3Abug)

A purely functional, typeclass-driven graph and tree traversal library for Scala 3.

```scala
libraryDependencies += "com.phasmidsoftware" %% "visitor" % "x.y.z"
```

where `x.y.z` is the latest version on Maven Central — see the
[Sonatype Central badge](https://central.sonatype.com/artifact/com.phasmidsoftware/visitor_3) above.

From Java, or from Maven:

```xml
<dependency>
    <groupId>com.phasmidsoftware</groupId>
    <artifactId>visitor_3</artifactId>
    <version>x.y.z</version>
</dependency>
```

Visitor 1.7.0 and later need Scala 3.9 or later; for Scala 3.7 or 3.8, use 1.6.0. (No release supports Scala 3.3.)

## API Documentation

Full Scaladoc is at <https://rchillyard.github.io/Visitor/latest/api/>.

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

### `Visitor[V, E, J]`

Answers: *where do the results go, and how do we track came-from relationships?*

```scala
trait Visitor[V, E, J <: Appendable[E, J]]:
  def visit(v: V): Visitor[V, E, J]
  def discover(v: V, cameFrom: V): Visitor[V, E, J] = this  // default no-op
  def result: J
  protected def journal: J
```

`E` is the journal's element type, and it is what distinguishes the implementations:
`JournaledVisitor` records `Mapped[V, R]`, `SimpleVisitor` records `V`, and
`ParentedVisitor` records `Mapped[V, R]` having consulted the node's predecessor. All three
are ordinary `Visitor`s, so every `Traversal` entry point accepts any of them.

Note that `visit` takes no `using` clause: a visitor that needs an `Evaluable` holds its own,
bound once when it is built.

Two events are distinguished:
- `visit(v)` — called when a vertex is *settled* (dequeued/popped and processed).
- `discover(v, cameFrom)` — called when a vertex is first *discovered* as a neighbour
  of `cameFrom`, before it is added to the frontier. Default is a no-op. The engine always
  calls `discover(v, parent)` **before** `visit(v)` for a non-start node, which is what lets
  `ParentedVisitor` work.

### `Appendable[X, A]`

```scala
trait Appendable[X, A <: Appendable[X, A]] extends AutoCloseable:
  def append(x: X): A
```

F-bounded, so appending returns the journal's own type rather than the supertype. This is what
makes `journal.append(x): J` type-check without a cast.

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
  def bfs[V, E, J](start: V, visitor: Visitor[V, E, J], goal: V => Boolean = _ => false)
      (using Neighbours[V, V], VisitedSet[V]): Visitor[V, E, J]

  def dfs[V, E, J](start: V, visitor: Visitor[V, E, J],
                   order: DfsOrder = DfsOrder.Pre, goal: V => Boolean = _ => false)
      (using Neighbours[V, V], VisitedSet[V]): Visitor[V, E, J]

  // as dfs, but also returns the VisitedSet it built
  def dfsTracking[V, E, J](start: V, visitor: Visitor[V, E, J], vs: VisitedSet[V],
                           order: DfsOrder = DfsOrder.Pre, goal: V => Boolean = _ => false)
      (using Neighbours[V, V]): Traversed[V, E, J]

  // in-order, for a binary tree: left subtree, node, right subtree
  def inOrder[V, E, J](start: V, visitor: Visitor[V, E, J], goal: V => Boolean = _ => false)
      (using BinaryNeighbours[V], VisitedSet[V]): Visitor[V, E, J]

  // as inOrder, but also returns the VisitedSet it built
  def inOrderTracking[V, E, J](start: V, visitor: Visitor[V, E, J], vs: VisitedSet[V],
                               goal: V => Boolean = _ => false)
      (using BinaryNeighbours[V]): Traversed[V, E, J]

  def bestFirst[V: Ordering, E, J](start: V, visitor: Visitor[V, E, J], goal: V => Boolean = _ => false)
      (using Neighbours[V, V], VisitedSet[V]): Visitor[V, E, J]

  def bestFirstMax[V: Ordering, E, J](start: V, visitor: Visitor[V, E, J], goal: V => Boolean = _ => false)
      (using Neighbours[V, V], VisitedSet[V]): Visitor[V, E, J]

  def bestFirstWeighted[W: Ordering, E, J](start: W, visitor: Visitor[W, E, J], goal: W => Boolean = _ => false)
      (using Neighbours[W, W], VisitedSet[W],
             CostUpdate[W, IndexedPrioQueue], IndexedPrioQueue[W]): Visitor[W, E, J]

  def traverseTree[H, V, E, J](start: H, visitor: Visitor[V, E, J],
                                order: DfsOrder = DfsOrder.Pre, goal: V => Boolean = _ => false)
      (using Neighbours[H, V], Neighbours[V, V], VisitedSet[V]): Visitor[V, E, J]
```

(`J` is bounded `J <: Appendable[E, J]` throughout; elided above for readability. Note that no
entry point asks for an `Evaluable` — the visitor carries whatever it needs.)

`inOrder` is the one entry point that does not take a `Neighbours`. In-order needs to know
*which side* each child is on, and an `Iterator[V]` of children cannot say: a node with a single
child is left-only or right-only, and the two give different sequences. Counting the children
would admit exactly that case, so the property is asserted in the type instead —
`BinaryNeighbours[V]` extends `GraphNeighbours[V]` with `left` and `right`, supplying
`neighbours` from them, so one instance still drives `dfs`, `bfs` and the rest:

```scala
given BinaryNeighbours[Node] with
  def left(n: Node): Option[Node] = n.l
  def right(n: Node): Option[Node] = n.r

Visits.inOrder(root)   // over a binary search tree, the nodes in ascending order
```

A node with no children collapses pre-, in- and post-order to one; a node with one is resolved
by the instance saying which side it is on; a third child cannot be presented at all, so
`inOrder` needs no runtime guard. See
[issue #48](https://github.com/rchillyard/Visitor/issues/48).

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

## Simple Traversals

When the answer is simply *which nodes, in what order*, you don't need an `Evaluable` and you
don't need to read a journal of pairs. `Visits` returns a `List[V]` directly:

```scala
given GraphNeighbours[String] = v => graph.getOrElse(v, Nil).iterator

Visits.bfs("A")                    // List(A, B, C, D)
Visits.dfs("A", DfsOrder.Post)     // post-order
Visits.inOrder(root)               // in-order; needs a BinaryNeighbours
Visits.tree(root)                  // a tree, root not visited
```

These are the Scala counterparts of the Java façade's `Traversals`. The Scala object is named
`Visits` rather than `Traversals` to keep it clearly apart from the `Traversal` engine: it is
named for what it returns, the visits in order, not for what it does.

Underneath, each builds a `SimpleVisitor` — a `Visitor` whose journal is an ordinary
`Appendable[V]` rather than `Appendable[Mapped[V, R]]`. It needs no `Evaluable`, allocates no
pair per visit, and leaves `discover` as the inherited no-op, so it records no came-from. Use it
directly when you want the journal itself:

```scala
Traversal.dfs(start, SimpleVisitor.withQueueJournal[V], DfsOrder.Post)
```

For evaluated results, came-from maps, or a journal of `(node, result)` pairs, use
`JournaledVisitor` as before. The two are interchangeable wherever a `Visitor` is called for:
`Visitor[V, E, J]` abstracts over `E`, the journal's element type, which is `Mapped[V, R]` for
one and `V` for the other.

## Knowing where a node came from

`Evaluable` sees a node alone. Some algorithms decide on the strength of *how* they arrived —
cycle detection in an undirected graph is "a back edge to anything other than my parent", and
edge classification is the same family. `EvaluableFrom` adds the predecessor:

```scala
trait EvaluableFrom[V, R]:
  def evaluate(from: Option[V], v: V): Option[R]
```

`ParentedVisitor` supplies it. It records predecessors as the engine discovers them and reads
them back at visit time, which works because **the engine calls `discover(v, parent)` before
`visit(v)` for every non-start node** — a contract pinned by `VisitorOrderingSpec`. No change
to `Traversal` is involved:

```scala
given EvaluableFrom[V, Option[V]] = EvaluableFrom.parentOf[V]
Traversal.bfs(start, ParentedVisitor.withQueueJournal[V, Option[V]])
```

`from` is `None` for the start node. First discovery wins, as with `CameFromJournal`. Note this
gives the predecessor *node*, not the edge traversed — Visitor has no notion of an edge, since
`Neighbours` yields nodes, so an algorithm needing the edge must carry it in `V`.

## Traversing several components

`dfs` builds a `VisitedSet` — that is how it avoids revisiting — and then discards it, so a
caller walking each component of a graph in turn had to rebuild one. `dfsTracking` hands it
back:

```scala
var seen = VisitedSet.empty[V]
for start <- roots if !seen.isVisited(start) do
  val out = Traversal.dfsTracking(start, SimpleVisitor.withQueueJournal[V], seen)
  seen = out.visited
```

The visited set is an ordinary parameter rather than a `using` one, so that carrying it forward
reads naturally; a partial explicit `using` list cannot be completed from defaults, and would
have forced the caller to name the `Tracer` too.

`VisitedSet.from(set)` builds one from an existing `Set` in O(1), for callers who already track
visited nodes themselves.

## Java API

Visitor's Scala API is driven by `given` instances, which a Java caller cannot supply. The
`com.phasmidsoftware.visitor.java` package takes the two type classes that matter —
`Neighbours` and `Evaluable` — as ordinary arguments instead, so from Java they are just lambdas:

```java
import com.phasmidsoftware.visitor.java.*;

Map<String, List<String>> graph = Map.of(
        "A", List.of("B", "C"),
        "B", List.of("D"),
        "C", List.of("D"),
        "D", List.of());

TraversalResult<String, String> r =
        Traversals.bfs("A", v -> graph.getOrDefault(v, List.of()));

r.visited();   // [A, B, C, D]
r.cameFrom();  // {B=A, C=A, D=B}
```

| Type | Role |
|---|---|
| `Traversals` | the entry points: `bfs`, `dfs`, `traverseTree`, each with optional goal and evaluator |
| `NeighbourFunction<H, V>` | the adjacency — the Java form of `Neighbours[H, V]` |
| `Evaluator<V, R>` | what to extract from each node — the Java form of `Evaluable[V, R]`; returning `Optional.empty()` filters |
| `Order` | `PRE` or `POST`, the Java form of `DfsOrder` |
| `TraversalResult<V, R>` | `visited()`, `results()` and `cameFrom()`, all unmodifiable |

A `goal` predicate halts the traversal *after* recording the first node it accepts, so that node
is present in the result. Note that `cameFrom()` records *discovery*, not visiting: a node
discovered just before the goal fired appears there without appearing in `visited()`. For a tree,
`cameFrom()` is always empty — the parent is already evident from the tree's shape.

The weighted traversals (`bestFirst`, `bestFirstMax`, `bestFirstWeighted`) are not yet exposed to
Java; they need an `Ordering` and a `CostUpdate`, and are reached from Scala.

Javadoc for this package is published alongside the Scaladoc, at
<https://rchillyard.github.io/Visitor/latest/javadoc/>.

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
| 1.6.0   | `Zero[A]` typeclass (identity only); `Monoid[A]` alongside it, bridged by a derived given rather than by inheritance; `given Zero[A]` instances for numeric types; derived `given [A: Monoid]: Zero[A]`; `CameFromJournal[V]` with first-discovery-wins semantics; `discover(v, cameFrom)` on `Visitor` (default no-op); `JournaledVisitor` records came-from when journal present; `withQueueJournalAndCameFrom` and `withListJournalAndCameFrom` factory methods; `discover` called in `traverse` and `dfs` at discovery time |
| 1.7.0   | Built with Scala 3.9.0 (the current LTS), so needs Scala 3.9 or later; sbt 1.12.13; Scaladoc on GitHub Pages |
| 1.8.0   | **`Visitor[V, E, J]` is now generic in its journal's element type `E`**, and `visit` no longer takes a `using Evaluable` — a `JournaledVisitor` binds one when it is built. **New `SimpleVisitor`** records bare nodes into an `Appendable[V]`, needing no `Evaluable` and allocating no pair per visit, and **`Visits.bfs`/`dfs`/`tree`** return a `List[V]` directly. New `Mapped[V, R]` alias for `(V, Option[R])`. **`Appendable[X]` is now F-bounded as `Appendable[X, A <: Appendable[X, A]]`**, with `append(x: X): A`, removing four unchecked casts. **`EvaluableFrom` and `ParentedVisitor`** give an evaluator the node a node was reached from ([#20](https://github.com/rchillyard/Visitor/issues/20)); **`Traversal.dfsTracking`** returns the `VisitedSet` it built, and **`VisitedSet.from`/`empty`** construct one directly ([#22](https://github.com/rchillyard/Visitor/issues/22)) — all additive. **Java API** (`com.phasmidsoftware.visitor.java`): `Traversals.bfs`/`dfs`/`traverseTree` with `NeighbourFunction`, `Evaluator`, `Order` and `TraversalResult`; Javadoc published beside the Scaladoc. **Removed `core.Zero` and `core.Monoid`** and their `given` instances — see the migration note below. `versionScheme` is now `pvp`; MiMa checks each release; `logback-classic` moved to `Test` scope, so it is no longer forced onto consumers |
| 1.9.0   | Added **in-order traversal** for binary trees: `Traversal.inOrder`, `Traversal.inOrderTracking` and `Visits.inOrder`, together with the **`BinaryNeighbours[V]`** typeclass they require ([#48](https://github.com/rchillyard/Visitor/issues/48)). `BinaryNeighbours` extends `GraphNeighbours` with `left` and `right` and supplies `neighbours` from them, so one instance drives every traversal. In-order is a separate entry point rather than a third `DfsOrder` case because it needs to know which side each child is on, which a `Neighbours` cannot say — see the note in the Traversal section. **`Traverse` is renamed to `Visits`** — the one breaking change in this release, and the reason it is 1.9.0 rather than 1.8.1. The division between the engine and the facade is sound, but `Traversal`, `Traverse` and `Traversed` were distinguishable only by inflection, and the inflection did not map to the roles; `Visits` is named for what it returns. `Traverse` was introduced in 1.8.0, so it carried the least history of the three. **`Traversed` moves to its own file**, which is also how a long-orphaned scaladoc block on `object Traversal` came to light: it sat immediately before `Traversed`'s own comment and so had never been rendered. **`Behaviours.scala` is split into one file per typeclass family** — `Evaluable`, `Neighbours`, `VisitedSet`, `Frontier` and `CostUpdate` — which is source-compatible, nothing having left the package, but binary-breaking, because Scala 3 compiles a file's top-level definitions into a synthetic class named after the file. Everything else in this release is additive |

## Migrating to 1.8.0

1.8.0 removes `com.phasmidsoftware.visitor.core.Zero` and
`com.phasmidsoftware.visitor.core.Monoid`, together with their `given` instances for `Int`,
`Long`, `Double` and `Float` and the derived `given [A: Monoid]: Zero[A]`.

This is a binary break, which is why it is a minor release: under PVP the first two numbers form
the major version, so 1.7.x and 1.8.x are not binary compatible.

Nothing inside Visitor used either type. Neither appeared in any signature in the public API, and
the only references to them anywhere were their own declarations and their given instances —
Visitor defined two type classes, published instances of them, and never asked a caller for one.

| If you were doing this | Do this instead |
|---|---|
| `import ...visitor.core.given` to pick up a `Monoid[Double]` (or `Int`/`Long`/`Float`) | depend on `org.typelevel %% "cats-kernel"` and use `cats.kernel.Monoid`, which supplies these through the implicit scope with no import at all |
| `import ...visitor.core.{Monoid as _, *, given}` to dodge a clash with your own `Monoid` | drop the `Monoid as _` exclusion; the clash is gone |
| writing `E: {Zero, Ordering}` or `E: {Monoid, Ordering}` against Visitor's types | the same bounds against `cats.kernel.Monoid`; Visitor's traversals never required either |
| writing `Visitor[V, R, J]` in your own signatures | `Visitor[V, Mapped[V, R], J]` — the second parameter is now the journal's element type |
| calling `Traversal.bfs(...)(using nbrs, ev, vs, tracer)` with an explicit `using` list | drop `ev`; the visitor carries its own `Evaluable` now |
| `JournaledVisitor.withQueueJournal[V, R]` with no `Evaluable` in scope | bring one into scope — it is needed when the visitor is built, not when it visits |
| implementing `Appendable[X]` yourself | implement `Appendable[X, YourType]`, with `append` returning `YourType`; the same applies to `Journal`, `IterableJournal` and `AbstractMapJournal` |
| writing `J <: Appendable[X]` as a bound | `J <: Appendable[X, J]` |

They were not migrated to `cats.kernel.Monoid` inside Visitor because that would have added
`cats-kernel` at the base of the dependency stack to supply something Visitor does not use.
See [issue #21](https://github.com/rchillyard/Visitor/issues/21).
