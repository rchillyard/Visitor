package com.phasmidsoftware.visitor;

import com.phasmidsoftware.java.JDfsVisitor;
import com.phasmidsoftware.java.JVisitor;
import com.phasmidsoftware.java.JVisitor$;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.phasmidsoftware.visitor.JMessage.PRE;
import static org.junit.Assert.assertEquals;

public class JDfsVisitorTest {

    @org.junit.Test
    public void testJDfsVisitor() {

        // Define a tree similar to a binary heap starting at slot 1
        //           10
        //        5--------13
        //      2---6---11---15
        //     1-3
        List<Integer> tree = List.of(-99, 10, 5, 13, 2, 6, 11, 15, 1, 3);

        Function<Integer, List<Integer>> g = i -> {
            int idx = tree.indexOf(i);
            if (idx < 0) return List.of(); // not found or sentinel
            int left = idx * 2;
            int right = left + 1;
            return Stream.of(left, right)
                    .filter(j -> j > 0 && j < tree.size())
                    .map(tree::get)
                    .toList();
        };


        try (JDfsVisitor<Integer> visitor = JDfsVisitor.createPreQueue(g)) {
            JDfsVisitor<Integer> result = visitor.dfs(10);
            QueueJournal<Integer> journal = (QueueJournal<Integer>) result.iterableJournal();
            Collection<Integer> actual = JVisitor$.MODULE$.toJava(journal.toList());
            assertEquals(List.of(10, 5, 2, 1, 3, 6, 13, 11, 15), List.copyOf(actual));
        }
    }

    @org.junit.Test
    public void testClose() {
        JVisitor<String> visitor = JVisitor.createPreQueue();
        JVisitor<String> result = visitor.visit(PRE, "Hello");
        result.close();
    }
}