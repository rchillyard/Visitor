# Contributing to Visitor

Thank you for your interest in contributing to Visitor! This document describes
how to get started, the conventions we follow, and what kinds of contributions
are most welcome.

---

## What is Visitor?

Visitor is a Scala 3 typeclass-driven graph traversal engine. It provides the
traversal infrastructure on which [Gryphon](https://github.com/rchillyard/Gryphon)
and other libraries build. All traversal algorithms — DFS, BFS, best-first,
weighted best-first — are driven by five orthogonal typeclasses, keeping graph
structure and traversal logic cleanly separated.

Visitor is intended to be reusable beyond graphs: the same engine can traverse
trees, ASTs, file systems, dependency graphs, or any structure for which a
`Neighbours` instance can be provided.

---

## Key Concepts

### The five typeclasses

| Typeclass | Role |
|---|---|
| `Evaluable[V, R]` | Extracts a result `R` from a visited node `V` |
| `Neighbours[H, V]` | Given a frontier element `H`, yields neighbouring `V`s |
| `VisitedSet[V]` | Tracks which vertices have been visited (immutable) |
| `Frontier[F[_]]` | The frontier data structure (stack, queue, priority queue) |
| `Visitor[V, R, J]` | Accumulates results into a journal `J` |

These typeclasses are orthogonal: swap any one independently to change traversal
behaviour without touching the others.

### The traversal entry points

All entry points live in `Traversal`:

| Method | Use |
|---|---|
| `Traversal.dfs` | Depth-first search (pre- or post-order) |
| `Traversal.bfs` | Breadth-first search |
| `Traversal.bestFirst` | Best-first search (unweighted priority) |
| `Traversal.bestFirstWeighted` | Weighted best-first (Dijkstra/Prim backbone) |

### Journal hierarchy

Results are accumulated into a `Journal[A]`:

- `ListJournal[A]` — prepends; post-order DFS results appear reversed (correct behaviour)
- `QueueJournal[A]` — appends; BFS results appear in visit order

### Priority queue hierarchy

Three honest ADTs illustrating the distinction between a data structure and an
abstract data type:

- `BinaryHeap[A]` — pure data structure; duplicates permitted
- `PrioQueue[A]` — ADT wrapping `BinaryHeap`; duplicates permitted
- `IndexedPrioQueue[A]` — ADT with `decreaseKey`; one-entry-per-key invariant

### Weight abstraction

`Monoid[A]` (mirroring Cats: `identity` + `combine`) replaces `Numeric[E]` as
the context bound for weighted traversals. Provided instances: `Int`, `Long`,
`Double`, `Float`.

### `CostUpdate[V, F[_]]`

A typeclass for post-settle priority updates, used by Dijkstra and Prim to
implement `decreaseKey`. The default no-op `given` is in scope for DFS/BFS.

---

## Getting Started

### Prerequisites

- **Java 17 or later**
- **Scala 3**
- **sbt 1.9+**
- **IntelliJ IDEA** (recommended) with the Scala plugin

### Clone and build

```bash
git clone https://github.com/rchillyard/Visitor.git
cd Visitor
sbt test
```

All tests should be green before you start making changes.

### Using a local snapshot in Gryphon

If your Visitor change needs to be validated against Gryphon:

```bash
# In the Visitor repository
sbt publishLocal

# In Gryphon's build.sbt, temporarily reference the local snapshot version
```

---

## Project Structure

```
src/
  main/scala/com/phasmidsoftware/visitor/
    core/
      Behaviours.scala   — Evaluable, Neighbours, VisitedSet, Frontier,
                           Visitor, JournaledVisitor, GraphNeighbours,
                           Monoid (given instances), CostUpdate, Tracer
      Journal.scala      — Appendable, Journal, ListJournal, QueueJournal
      PrioQueue.scala    — BinaryHeap, PrioQueue, IndexedPrioQueue
      Traversal.scala    — dfs, bfs, bestFirst, bestFirstWeighted, traverseTree
  test/scala/            — ScalaTest specs
attic/                   — Historical/non-compiling code preserved for reference
```

---

## Coding Conventions

- **Scala 3 throughout** — `given`/`using`, context bounds, type aliases, `enum`.
  Avoid Scala 2 implicits.
- **Orthogonality** — the five typeclasses must remain independent. Do not add
  cross-dependencies between `Evaluable`, `Neighbours`, `VisitedSet`, etc.
- **Lazy vs. strict bindings in iterators** — tuple pattern matching inside a
  `given` returning `Iterator` can produce lazy bindings with surprising
  evaluation order. Always use explicit strict extraction (e.g. `ev._1`,
  `ev._2`) when correctness depends on evaluation timing. This is a known
  Heisenbug source.
- **`ListJournal` prepends** — post-order DFS results appear reversed. This is
  correct behaviour, not a bug. Document it clearly.
- **`shouldBe` in tests** — use `shouldBe` rather than `should be` in ScalaTest
  specs.
- **No Cats dependency** — Visitor intentionally avoids depending on Cats.
  `Monoid` is defined locally. If Cats integration is desired, it belongs in a
  separate optional module.
- **Purely functional** — all traversal methods are pure. The `pred` and
  `bestCost` mutable maps in `WeightedTraversal` are the one principled
  exception; they are owned exclusively by `CostUpdate` and not observable
  from outside.

---

## Testing

```bash
sbt test
```

- Tests use **ScalaTest** (`AnyFlatSpec` + `Matchers`).
- New traversal entry points must be tested against at least:
    - A simple linear graph
    - A graph with multiple components (for `dfsAll`-style coverage)
    - A graph with cycles
    - The `goal` predicate (early termination)

---

## What Contributions Are Welcome

### Most welcome

- **New traversal entry points** — e.g. `dfsInOrder` for binary trees (requires
  a `BinaryNeighbours[H, V]` typeclass yielding exactly `(left, right)`).
- **Journal types** — new `Appendable` implementations (e.g. a `ParentJournal`
  recording `(child, parent)` pairs, which would allow Gryphon's Java façade to
  eliminate its Java BFS/DFS reimplementation).
- **`Monoid` instances** — additional given instances for standard types.
- **Bug fixes** — particularly anything related to lazy binding in iterators.
- **Test coverage** — additional edge cases, especially for `bestFirstWeighted`
  with non-`Double` weight types.
- **Documentation** — Scaladoc improvements, especially on the typeclass
  contracts.

### Please discuss first

- **Changes to the five core typeclasses** — any change to `Evaluable`,
  `Neighbours`, `VisitedSet`, `Frontier`, or `Visitor` affects Gryphon and any
  other downstream library. Open an issue before starting work.
- **Cats integration** — adding Cats as a dependency would affect the dependency
  footprint of every downstream user. Discuss first; a separate optional module
  is the most likely acceptable form.
- **Parallel traversal** — splitting the frontier and merging results would
  require `Monoid[J]` and `Monoid[VisitedSet[V]]`. This is tracked as a
  long-term aspiration; coordinate before starting.
- **`traverseTree` changes** — the `H` vs `V` distinction (root type vs node
  type) is intentional and subtle. Changes here need careful thought.

---

## Submitting a Pull Request

1. Fork the repository and create a feature branch from `main`.
2. Make your changes, ensuring all existing tests remain green.
3. Add tests for any new behaviour.
4. Update the README if the change affects the public API.
5. Open a pull request with a clear description of what changed and why,
   and whether any corresponding change is needed in Gryphon.

---

## Related Projects

- [Gryphon](https://github.com/rchillyard/Gryphon) — the graph algorithms
  library that depends on Visitor. Changes to Visitor's core typeclasses or
  `Traversal` entry points typically require corresponding changes in Gryphon.
- [DSAIPG](https://github.com/rchillyard/DSAIPG) — the course repository
  that both Visitor and Gryphon ultimately serve.

---

## License

Visitor is licensed under the MIT License. By contributing, you agree that your
contributions will be licensed under the same terms.