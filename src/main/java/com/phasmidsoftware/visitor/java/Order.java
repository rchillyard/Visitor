package com.phasmidsoftware.visitor.java;

/**
 * Whether a depth-first traversal records a node before or after its descendants.
 *
 * <p>The Java-facing form of Visitor's {@code DfsOrder}.
 */
public enum Order {

    /** Record a node before expanding its children. */
    PRE,

    /** Record a node after its children have been expanded and recorded. */
    POST
}
