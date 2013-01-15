/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.junit.Before;
import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;

/**
 *
 * @author mscott
 */
public class PeriodicEvictionTriggerTest extends AbstractEvictionTriggerTest {
    
    public PeriodicEvictionTriggerTest() {
    }

    @Override
    public AbstractEvictionTrigger createTrigger() {
        return new PeriodicEvictionTrigger(Mockito.mock(ObjectManager.class), ObjectID.NULL_ID);
    }

    @Override @Before
    public void setUp() {
//        Mockito.when(getEvictableMap().isEvicting()).thenReturn(Boolean.TRUE);
        Mockito.when(getEvictableMap().getSize()).thenReturn(250);
        super.setUp();
    }
    
    
}
