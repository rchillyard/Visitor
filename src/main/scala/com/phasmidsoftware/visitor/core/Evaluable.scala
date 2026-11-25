package com.phasmidsoftware.visitor.core

/**
  * Typeclass: given a node of type V, extract an optional result of type R.
  * This is the "what do we want to know about a node" concern.
  *
  * @tparam V the node type
  * @tparam R the result type
  */
trait Evaluable[V, R]:
  def evaluate(v: V): Option[R]

/**
  * Typeclass: like [[Evaluable]], but also given the node this one was first reached from.
  *
  * Some algorithms decide on the strength of *how* they arrived, not just where they are —
  * cycle detection in an undirected graph is "a back edge to anything other than my parent",
  * and edge classification (tree / back / forward / cross) is the same family. `Evaluable` sees
  * the node alone and cannot express those.
  *
  * `from` is `None` for the start node, which was reached from nowhere, and for any node whose
  * predecessor the visitor did not record. It is the *first* node this one was discovered from:
  * a node reached again by another route keeps its original predecessor, matching
  * [[CameFromJournal]].
  *
  * Use it with [[ParentedVisitor]], which records predecessors at discovery and supplies them
  * here at visit time. No change to [[Traversal]] is needed: the engine already calls
  * `discover(v, parent)` before `visit(v)` for every non-start node.
  *
  * See [[https://github.com/rchillyard/Visitor/issues/20 issue #20]]. Note that this gives the
  * predecessor *node*, not the edge traversed — Visitor has no notion of an edge, since
  * [[Neighbours]] yields nodes. An algorithm needing the edge must carry it in `V`.
  *
  * @tparam V the node type
  * @tparam R the result type
  */
trait EvaluableFrom[V, R]:
  def evaluate(from: Option[V], v: V): Option[R]

/**
  * Companion utilities for creating [[EvaluableFrom]] instances.
  *
  * Provides adapters for cases where evaluation either does not depend on the
  * predecessor node or needs direct access to the predecessor recorded by a
  * traversal visitor.
  */
object EvaluableFrom:
  /**
    * Adapts an ordinary [[Evaluable]], ignoring the predecessor.
    *
    * @param ev the evaluable to adapt
    * @tparam V the node type
    * @tparam R the result type
    */
  def ignoringParent[V, R](ev: Evaluable[V, R]): EvaluableFrom[V, R] =
    (_: Option[V], v: V) => ev.evaluate(v)

  /** Yields each node paired with the node it was reached from. */
  def parentOf[V]: EvaluableFrom[V, Option[V]] =
    (from: Option[V], _: V) => Some(from)
