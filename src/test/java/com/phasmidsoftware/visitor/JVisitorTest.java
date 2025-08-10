package com.phasmidsoftware.visitor;

import com.phasmidsoftware.java.JVisitor;

import static com.phasmidsoftware.visitor.JMessage.PRE;
import static org.junit.Assert.assertEquals;

public class JVisitorTest {

    @org.junit.Test
    public void testJVisitor() throws Exception {
        try (JVisitor<String> visitor = JVisitor.createPreQueue()) {
            JVisitor<String> result = visitor.visit(PRE, "Hello");
            QueueJournal<String> journal = (QueueJournal<String>) result.iterableJournal();
            System.out.println(journal);
            assertEquals("'Hello'", journal.mkString("'", ",", "'"));
        }
    }

    @org.junit.Test
    public void testClose() {
    }
}