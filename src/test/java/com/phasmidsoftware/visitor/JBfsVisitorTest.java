package com.phasmidsoftware.visitor;

import com.phasmidsoftware.visitor.javaAPI.JBfsVisitor;
import com.phasmidsoftware.visitor.javaAPI.JVisitor;
import com.phasmidsoftware.visitor.javaAPI.JVisitor$;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.phasmidsoftware.visitor.JMessage.PRE;
import static org.junit.Assert.assertEquals;

public class JBfsVisitorTest {

    @org.junit.Test
    public void testJBfsVisitor() {

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

        Predicate<Integer> goal = i -> false; // There is no goal

        try (JBfsVisitor<Integer> visitor = JBfsVisitor.createPreQueue(g, goal)) {
            BfsResult<Integer> result = visitor.bfs(10);
            JBfsVisitor<Integer> visitor1 = result.visitor;
            QueueJournal<Integer> journal = (QueueJournal<Integer>) visitor1.iterableJournal();
            Collection<Integer> actual = JVisitor$.MODULE$.toJava(journal.toList());
            assertEquals(List.of(10, 5, 13, 2, 6, 11, 15, 1, 3), List.copyOf(actual));
        }
    }

    @org.junit.Test
    public void testClose() {
        JVisitor<String> visitor = JVisitor.createPreQueue();
        JVisitor<String> result = visitor.visit(PRE, "Hello");
        result.close();
    }
}