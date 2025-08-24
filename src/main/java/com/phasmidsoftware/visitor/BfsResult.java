package com.phasmidsoftware.visitor;

import com.phasmidsoftware.visitor.javaAPI.JBfsVisitor;

import java.util.Optional;

/**
 * Represents the result of a breadth-first search (BFS) operation.
 * <p>
 * CONSIDER using Record instead.
 * <p>
 * This class encapsulates the outcome of a BFS traversal, including the updated state of
 * the visitor and an optional goal element that may have been reached during the traversal.
 *
 * @param <X> the type of elements processed during the BFS traversal
 */
public class BfsResult<X> {
    final JBfsVisitor<X> visitor;
    final Optional<X> maybeGoal;

    public BfsResult(JBfsVisitor<X> visitor, Optional<X> maybeGoal) {
        this.visitor = visitor;
        this.maybeGoal = maybeGoal;
    }
}
