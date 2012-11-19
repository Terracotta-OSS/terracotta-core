/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictionTrigger;
import com.tc.objectserver.api.ServerMapEvictionManager;
import org.junit.Before;
import org.mockito.Mockito;

/**
 *
 * @author mscott
 */
public class CapacityEvictionTriggerTest extends AbstractEvictionTriggerTest {
    
    public CapacityEvictionTriggerTest() {
    }

    @Override
    public EvictionTrigger createTrigger() {
        return new CapacityEvictionTrigger(Mockito.mock(ServerMapEvictionManager.class), ObjectID.NULL_ID);
    }

    @Override @Before
    public void setUp() {
        Mockito.when(getEvictableMap().isEvicting()).thenReturn(Boolean.TRUE);
        Mockito.when(getEvictableMap().getSize()).thenReturn(200);
        super.setUp();
    }
    
    
}
