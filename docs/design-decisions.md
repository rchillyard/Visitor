# Design Decisions

Decisions already taken, kept because the *reasoning* is useful to a reader of the code and
is not recoverable from a diff.

**This is not a backlog.** Work still to be done is tracked as
[GitHub issues](https://github.com/rchillyard/Visitor/issues), which is the only place it is
tracked. A markdown to-do list cannot be listed when a release window opens, and the one this
file replaces had — by the time it was retired — five of its fourteen items already done,
one tracking another project's work, and two numbered 6.

---

## `Monoid[A]` does not extend `Ordering[A]`

Considered, and rejected, when the weighted traversals were designed.

Making `Monoid[A] extends Ordering[A]` would have allowed a single context bound `E: Monoid`
for weighted traversals, rather than `E: {Monoid, Ordering}`. It was rejected because the two
abstractions are independent in both directions:

- a monoid does not imply an ordering — `String` concatenation is a perfectly good monoid with
  no natural ordering;
- an ordering does not imply a monoid — there is no way to combine two `Ordering` values.

So the two context bounds are the honest design, and conflating them would have been a
convenience bought with a false claim about the types.

Recorded in case the question resurfaces. It applies equally to `cats.kernel.Monoid`, which is
what a consumer would now use: Visitor's own `Zero` and `Monoid` were removed in 1.8.0
([#21](https://github.com/rchillyard/Visitor/issues/21)), since nothing in the engine consumed
them.

See also [issue #9](https://github.com/rchillyard/Visitor/issues/9#issue-4240254773).

---

## The traversal engine takes no `Evaluable`

Since 1.8.0, `Visitor[V, E, J]` is generic in its journal's element type, and a visitor carries
whatever it needs to turn a node into a journal entry. `Traversal`'s entry points therefore have
no `using Evaluable` clause — the engine does not know what an `Evaluable` is.

The alternative, threading the evaluator through every call, was what the pre-1.8.0 API did. It
meant the evaluator was re-resolved implicitly at every `visit`, and — more importantly — it
fixed the journal's element type at `(V, Option[R])`, which made a visitor recording bare nodes
inexpressible. `SimpleVisitor` exists because that restriction was lifted.

The cost is that a `JournaledVisitor` cannot be constructed without an `Evaluable` in scope, even
to exercise `discover` in isolation. That was judged the better trade: a dependency bound once at
construction, rather than resolved repeatedly at use.
