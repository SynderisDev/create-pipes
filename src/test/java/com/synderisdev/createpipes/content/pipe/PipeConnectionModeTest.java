package com.synderisdev.createpipes.content.pipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipeConnectionModeTest {
    @Test
    void cyclesForwardInMekanismStyleOrder() {
        assertEquals(PipeConnectionMode.PULL, PipeConnectionMode.NORMAL.next());
        assertEquals(PipeConnectionMode.PUSH, PipeConnectionMode.PULL.next());
        assertEquals(PipeConnectionMode.NONE, PipeConnectionMode.PUSH.next());
        assertEquals(PipeConnectionMode.NORMAL, PipeConnectionMode.NONE.next());
    }

    @Test
    void previousStillSupportsReverseModeIterationForFutureUi() {
        assertEquals(PipeConnectionMode.NONE, PipeConnectionMode.NORMAL.previous());
        assertEquals(PipeConnectionMode.PUSH, PipeConnectionMode.NONE.previous());
    }

    @Test
    void modeRulesSeparatePullPushAndPassiveInsert() {
        assertTrue(PipeModeRules.activelyExtracts(PipeConnectionMode.PULL));
        assertFalse(PipeModeRules.activelyExtracts(PipeConnectionMode.PUSH));
        assertTrue(PipeModeRules.acceptsActiveInsertion(PipeConnectionMode.PUSH));
        assertTrue(PipeModeRules.acceptsActiveInsertion(PipeConnectionMode.NORMAL));
        assertTrue(PipeModeRules.acceptsExternalInsertion(PipeConnectionMode.NORMAL));
        assertFalse(PipeModeRules.acceptsExternalInsertion(PipeConnectionMode.PUSH));
        assertFalse(PipeModeRules.connects(PipeConnectionMode.NONE));
    }

}
