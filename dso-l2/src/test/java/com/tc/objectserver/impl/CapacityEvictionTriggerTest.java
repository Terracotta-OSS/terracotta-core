/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author mscott
 */
public class CapacityEvictionTriggerTest extends AbstractEvictionTriggerTest {
    
    public CapacityEvictionTriggerTest() {
    }

    @Override
    public AbstractEvictionTrigger createTrigger() {
        return new CapacityEvictionTrigger(Mockito.mock(ServerMapEvictionManager.class), ObjectID.NULL_ID);
    }
    
    @Test 
    public void testCapacityEvictionChaining() throws Exception {
        //  ten million elements in map
        Mockito.when(getEvictableMap().getSize()).thenReturn(10000000);
        //  set max to 250k
        Mockito.when(getEvictableMap().getMaxTotalCount()).thenReturn(250000);
        checkCycle(250000);
        Mockito.verify(this.getClientSet()).addReferenceSetChangeListener(Mockito.<ClientObjectReferenceSetChangedListener>any());
    }

    @Override @Before
    public void setUp() {
        getEvictableMap().startEviction();
        Mockito.when(getEvictableMap().isEvicting()).thenReturn(Boolean.TRUE);
        Mockito.when(getEvictableMap().getSize()).thenReturn(250);
        super.setUp();
    }
    
    
}
