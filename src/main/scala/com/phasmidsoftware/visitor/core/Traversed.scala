package com.phasmidsoftware.visitor.core

/**
  * The outcome of a traversal that hands back the visited set it built, as well as the visitor.
  *
  * Returned by [[Traversal.dfsTracking]] and [[Traversal.inOrderTracking]]. Every traversal
  * necessarily builds a visited set — that is how it avoids revisiting — and the plain entry
  * points then discard it. A caller running a traversal repeatedly over the components of a
  * graph needs to know what the previous run reached, and this is how it is carried forward.
  *
  * It lives in its own file rather than beside the engine because it is a result value, not
  * part of the engine: when it sat at the head of `Traversal.scala`, its scaladoc displaced the
  * engine's, and the engine's comment was silently orphaned for several releases.
  *
  * @param visitor the visitor, carrying the journal
  * @param visited the visited set as it stood when the traversal finished
  * @tparam V the node type
  * @tparam E the journal's element type
  * @tparam J the journal type
  */
case class Traversed[V, E, J <: Appendable[E, J]](visitor: Visitor[V, E, J], visited: VisitedSet[V])
